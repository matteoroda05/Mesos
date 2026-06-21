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

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.GameUpdateSink;
import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;

/**
 * Hosts the game service over object streams, accepting socket clients and
 * routing their requests while maintaining update subscriptions.
 */
public class SocketGameServer implements AutoCloseable {
    private final MesosServerService serverService;
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final AtomicBoolean running;
    private final Set<Socket> clientSockets;

    /**
     * Opens a socket server and starts the background accept loop.
     *
     * @param serverService service used to handle incoming requests
     * @param port port to bind, or 0 for an ephemeral port
     * @throws IllegalArgumentException if the service is null or the port is
     *                                  outside 0 to 65535
     * @throws IOException if the server socket cannot be opened
     */
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

    /**
     * Returns the actual bound server port.
     *
     * @return local server socket port
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Accepts client sockets while the server is running and dispatches each
     * client to the executor.
     * Socket exceptions after shutdown are treated as normal termination.
     *
     * @throws IllegalStateException if accepting a connection fails while the
     *                               server is still running
     */
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

    /**
     * Handles one socket client until the socket closes or the server stops.
     * Unsupported payload types are returned as failure results, and active
     * subscriptions are cleaned up when the client disconnects.
     *
     * @param clientSocket accepted client socket
     * @throws IllegalStateException if request handling fails while the server
     *                               is still running
     */
    private void handleClient(Socket clientSocket) {
        ClientConnection connection = null;
        try (Socket socket = clientSocket;
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            output.flush();
            connection = new ClientConnection(output);

            while (running.get() && !socket.isClosed()) {
                GameRequest payload;
                GameOperationResult response;
                try {
                    payload = (GameRequest) input.readObject();
                    response = payload.requestToService(serverService, connection);
                } catch (EOFException exception) {
                    return;
                } catch (ClassCastException exception) {
                    response = GameOperationResult.failure("Unsupported request payload", null);
                }

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

    /**
     * Stops accepting clients, closes connected sockets, closes the server
     * socket, and interrupts executor tasks.
     *
     * @throws IOException if closing a socket fails
     */
    @Override
    public void close() throws IOException {
        running.set(false);
        for (Socket socket : clientSockets) {
            socket.close();
        }
        serverSocket.close();
        executorService.shutdownNow();
    }

    /**
     * Represents one connected socket client and manages its active game-update
     * subscription.
     */
    public final class ClientConnection implements GameUpdateSink {
        private final ObjectOutputStream output;
        private SubscribeGameRequest activeSubscription;

        /**
         * Creates a per-client connection wrapper around the output stream.
         *
         * @param output object output stream used to send responses and updates
         */
        private ClientConnection(ObjectOutputStream output) {
            this.output = output;
        }

        /**
         * Sends an asynchronous game update to this client.
         *
         * @param update update payload
         * @throws IOException if writing to the socket fails
         */
        @Override
        public void accept(GameServerMessage update) throws IOException {
            sendMessage(update);
        }

        /**
         * Sends one serializable server message over the object stream.
         *
         * @param message response or update to send
         * @throws IOException if serialization or socket writing fails
         */
        private synchronized void sendMessage(Serializable message) throws IOException {
            output.writeObject(message);
            output.flush();
            output.reset();
        }

        /**
         * Records this client's active subscription.
         * If the client was subscribed to a different game or nickname, the old
         * subscription is removed before replacing it.
         *
         * @param request subscription request
         */
        public void subscribe(SubscribeGameRequest request) {
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

        /**
         * Clears the active subscription when the request matches it.
         * Non-matching requests leave the current subscription unchanged.
         *
         * @param request unsubscription request
         */
        public void unsubscribe(UnsubscribeGameRequest request) {
            if (activeSubscription != null
                    && activeSubscription.gameName().equals(request.gameName())
                    && activeSubscription.nickname().equals(request.nickname())) {
                activeSubscription = null;
            }
        }

        /**
         * Removes any active subscription during client disconnect cleanup.
         * Calling cleanup with no active subscription is a no-op.
         */
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
