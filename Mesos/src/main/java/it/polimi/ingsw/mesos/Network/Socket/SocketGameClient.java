package it.polimi.ingsw.mesos.Network.Socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.GameUpdateListener;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;

public class SocketGameClient implements GameClient {
    private final Socket socket;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final BlockingQueue<GameOperationResult> responses;
    private final Thread readerThread;
    private final ScheduledExecutorService heartbeatExecutor;

    private volatile SocketClientContext context;
    private volatile boolean running;
    private volatile SubscribeGameRequest activeSubscription;
    private volatile boolean sendHeartbeat;
    private volatile String heartbeatNickname;
    private volatile String heartbeatGameName;
    private volatile ScheduledFuture<?> heartbeatTask;

    public SocketGameClient(String host, int port) throws IOException {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        this.socket = new Socket(host, port);
        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.output.flush();
        this.input = new ObjectInputStream(socket.getInputStream());
        this.responses = new LinkedBlockingQueue<>();
        this.context = new SocketClientContext(this.responses, null);
        this.running = true;
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "mesos-socket-client-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.readerThread = new Thread(this::readLoop, "mesos-socket-client-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    @Override
    public GameOperationResult nickname(NicknameRequest request) throws IOException {
        GameOperationResult result = exchange(request);
        if (result.success() && !result.rejoinAvailable()) {
            heartbeatNickname = request.nickname();
            heartbeatGameName = null;
            sendHeartbeat = true;
            startHeartbeat();
        }
        return result;
    }

    @Override
    public GameOperationResult getLobby() throws IOException {
        return exchange(new LobbyRequest());
    }

    @Override
    public GameOperationResult createGame(CreateGameRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult joinGame(JoinGameRequest request) throws IOException {
        GameOperationResult result = exchange(request);
        if (result.success()) {
            heartbeatNickname = request.nickname();
            heartbeatGameName = result.snapshot() == null ? null : result.snapshot().gameName();
            sendHeartbeat = true;
            startHeartbeat();
        }
        return result;
    }

    @Override
    public GameOperationResult getSnapshot(SnapshotRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult chooseOffer(ChooseOfferRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult chooseCard(ChooseCardRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateListener listener) throws IOException {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (activeSubscription != null) {
            unsubscribe(new UnsubscribeGameRequest(activeSubscription.gameName(), activeSubscription.nickname()));
        }

        context = new SocketClientContext(responses, listener);
        GameOperationResult result = exchange(request);
        if (result.success()) {
            activeSubscription = request;
            return result;
        }
        context = new SocketClientContext(responses, null);
        return result;
    }

    @Override
    public GameOperationResult unsubscribe(UnsubscribeGameRequest request) throws IOException {
        if (activeSubscription == null) {
            context = new SocketClientContext(responses, null);
            return GameOperationResult.success("Nessuna sottoscrizione attiva", null);
        }

        GameOperationResult result = exchange(request);
        if (result.success()) {
            activeSubscription = null;
            context = new SocketClientContext(responses, null);
        }
        return result;
    }

    private synchronized GameOperationResult exchange(GameRequest request) throws IOException {
        try {
            output.writeObject(request);
            output.flush();
            output.reset();

            while (running) {
                GameOperationResult response = responses.take();
                if (response != null) {
                    return response;
                }
            }
            throw new IOException("Client connection closed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for server response", exception);
        }
    }

    @Override
    public void close() throws IOException {
        if (activeSubscription != null) {
            try {
                unsubscribe(new UnsubscribeGameRequest(activeSubscription.gameName(), activeSubscription.nickname()));
            } catch (IOException ignored) {
            }
        }
        sendHeartbeat = false;
        heartbeatNickname = null;
        heartbeatGameName = null;
        stopHeartbeat();
        heartbeatExecutor.shutdownNow();
        running = false;
        socket.close();
        readerThread.interrupt();
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                this::sendHeartbeat,
                NetworkDefaults.HEARTBEAT_INTERVAL_MILLIS,
                NetworkDefaults.HEARTBEAT_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopHeartbeat() {
        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(true);
            heartbeatTask = null;
        }
    }

    private void sendHeartbeat() {
        String nickname = heartbeatNickname;
        String gameName = heartbeatGameName;
        if (!running || !sendHeartbeat || nickname == null) {
            return;
        }

        try {
            if (gameName == null) {
                exchange(new HeartbeatRequest(nickname));
            } else {
                exchange(new HeartbeatRequest(gameName, nickname, true));
            }
        } catch (IOException ignored) {
        }
    }


    private void readLoop() {
        try {
            while (running && !socket.isClosed()) {
                Object response = input.readObject();
                try {
                    GameServerMessage message = (GameServerMessage) response;
                    message.messageExecute(context);
                } catch (ClassCastException exception) {
                    throw new IOException("Unexpected response type: " + response);
                }
            }
        } catch (SocketException ignored) {
        } catch (ClassNotFoundException | IOException exception) {
            if (running) {
                context.responses().offer(GameOperationResult.failure("Connessione socket chiusa: " + exception.getMessage(), null));
            }
        } finally {
            running = false;
        }
    }

    public record SocketClientContext (BlockingQueue<GameOperationResult> responses, GameUpdateListener updateListener) { }
}
