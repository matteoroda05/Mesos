package it.polimi.ingsw.mesos.Network.Socket;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import it.polimi.ingsw.mesos.Network.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.GameUpdateSink;
import it.polimi.ingsw.mesos.Network.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.UnsubscribeGameRequest;

public class SocketGameServer implements AutoCloseable {
    private final MesosServerService serverService;
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final AtomicBoolean running;
    private final Set<Socket> clientSockets;

    public SocketGameServer(MesosServerService serverService, int port) throws IOException {
        if (serverService == null) {
            throw new IllegalArgumentException("serverService cannot be null");
        }
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }

        this.serverService = serverService;
        this.serverSocket = new ServerSocket(port);
        this.executorService = Executors.newCachedThreadPool();
        this.running = new AtomicBoolean(true);
        this.clientSockets = ConcurrentHashMap.newKeySet();

        executorService.execute(this::acceptLoop);
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                executorService.execute(() -> handleClient(clientSocket));
            } catch (SocketException exception) {
                if (running.get()) {
                    throw new IllegalStateException("Socket server stopped unexpectedly", exception);
                }
                return;
            } catch (IOException exception) {
                if (running.get()) {
                    throw new IllegalStateException("Failed to accept a client connection", exception);
                }
                return;
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        ClientConnection connection = null;
        try (Socket socket = clientSocket;
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            output.flush();
            connection = new ClientConnection(output);

            while (running.get() && !socket.isClosed()) {
                Object payload;
                try {
                    payload = input.readObject();
                } catch (EOFException exception) {
                    return;
                }
                GameOperationResult response = dispatch(payload, connection);
                connection.sendMessage(response);
            }
        } catch (SocketException ignored) {
        } catch (ClassNotFoundException | IOException exception) {
            if (running.get()) {
                throw new IllegalStateException("Failed to handle a socket client", exception);
            }
        } finally {
            if (connection != null) {
                connection.cleanup();
            }
            clientSockets.remove(clientSocket);
        }
    }

    private GameOperationResult dispatch(Object payload, ClientConnection connection) {
        if (!(payload instanceof GameRequest request)) {
            return GameOperationResult.failure("Unsupported request payload", null);
        }

        return switch (request) {
            case CreateGameRequest createGameRequest -> serverService.createGame(createGameRequest);
            case JoinGameRequest joinGameRequest -> serverService.joinGame(joinGameRequest);
            case SnapshotRequest snapshotRequest -> serverService.getSnapshot(snapshotRequest);
            case ChooseOfferRequest chooseOfferRequest -> serverService.chooseOffer(chooseOfferRequest);
            case ChooseCardRequest chooseCardRequest -> serverService.chooseCard(chooseCardRequest);
            case SubscribeGameRequest subscribeGameRequest -> {
                GameOperationResult result = serverService.subscribe(subscribeGameRequest, connection);
                if (result.success()) {
                    connection.subscribe(subscribeGameRequest);
                }
                yield result;
            }
            case UnsubscribeGameRequest unsubscribeGameRequest -> {
                GameOperationResult result = serverService.unsubscribe(unsubscribeGameRequest, connection);
                connection.unsubscribe(unsubscribeGameRequest);
                yield result;
            }
        };
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        for (Socket socket : clientSockets) {
            socket.close();
        }
        serverSocket.close();
        executorService.shutdownNow();
    }

    private final class ClientConnection implements GameUpdateSink {
        private final ObjectOutputStream output;
        private SubscribeGameRequest activeSubscription;

        private ClientConnection(ObjectOutputStream output) {
            this.output = output;
        }

        @Override
        public void accept(GameViewUpdate update) throws IOException {
            sendMessage(update);
        }

        private synchronized void sendMessage(Serializable message) throws IOException {
            output.writeObject(message);
            output.flush();
            output.reset();
        }

        private void subscribe(SubscribeGameRequest request) {
            if (activeSubscription != null
                    && (!activeSubscription.gameName().equals(request.gameName())
                    || !activeSubscription.nickname().equals(request.nickname()))) {
                serverService.unsubscribe(
                        new UnsubscribeGameRequest(activeSubscription.gameName(), activeSubscription.nickname()),
                        this
                );
            }
            activeSubscription = request;
        }

        private void unsubscribe(UnsubscribeGameRequest request) {
            if (activeSubscription != null
                    && activeSubscription.gameName().equals(request.gameName())
                    && activeSubscription.nickname().equals(request.nickname())) {
                activeSubscription = null;
            }
        }

        private void cleanup() {
            if (activeSubscription == null) {
                return;
            }
            serverService.unsubscribe(
                    new UnsubscribeGameRequest(activeSubscription.gameName(), activeSubscription.nickname()),
                    this
            );
            activeSubscription = null;
        }
    }
}
