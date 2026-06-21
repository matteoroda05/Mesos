package it.polimi.ingsw.mesos.network.client.socket;

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

import it.polimi.ingsw.mesos.network.protocol.requests.*;
import it.polimi.ingsw.mesos.network.client.GameClient;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessage;
import it.polimi.ingsw.mesos.network.protocol.messages.ResultType;
import it.polimi.ingsw.mesos.network.client.GameUpdateListener;
import it.polimi.ingsw.mesos.network.config.NetworkDefaults;

/**
 * Implements the game-client contract over a socket connection, coordinating
 * request responses, update delivery, and periodic heartbeats.
 */
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

    /**
     * Opens a socket client connection and starts the background reader thread.
     *
     * @param host server host
     * @param port server port
     * @throws IllegalArgumentException if host is null/blank or port is outside
     *                                  1 to 65535
     * @throws IOException if the socket or object streams cannot be opened
     */
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

    /**
     * Sends a nickname request and starts lobby heartbeat when the nickname is
     * accepted without an immediate rejoin path.
     *
     * @param request nickname request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult nickname(NicknameRequest request) throws IOException {
        GameOperationResult result = exchange(request);
        if (result.type() == ResultType.NICKNAME
                && !((GameOperationResult.Nickname) result).rejoinAvailable()) {
            heartbeatNickname = request.nickname();
            heartbeatGameName = null;
            sendHeartbeat = true;
            startHeartbeat();
        }
        return result;
    }

    /**
     * Requests the current lobby snapshot.
     *
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult getLobby() throws IOException {
        return exchange(new LobbyRequest());
    }

    /**
     * Sends a create-game request.
     *
     * @param request creation request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult createGame(CreateGameRequest request) throws IOException {
        return exchange(request);
    }

    /**
     * Sends a join request and starts game heartbeat when successful.
     *
     * @param request join request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult joinGame(JoinGameRequest request) throws IOException {
        GameOperationResult result = exchange(request);
        if (result.success()) {
            heartbeatNickname = request.nickname();
            var snapshot = GameOperationResult.snapshotOf(result);
            heartbeatGameName = snapshot == null ? null : snapshot.gameName();
            sendHeartbeat = true;
            startHeartbeat();
        }
        return result;
    }

    /**
     * Requests a game snapshot.
     *
     * @param request snapshot request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult getSnapshot(SnapshotRequest request) throws IOException {
        return exchange(request);
    }

    /**
     * Sends an offer-choice request.
     *
     * @param request offer-choice request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult chooseOffer(ChooseOfferRequest request) throws IOException {
        return exchange(request);
    }

    /**
     * Sends a card-choice request.
     *
     * @param request card-choice request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult chooseCard(ChooseCardRequest request) throws IOException {
        return exchange(request);
    }

    /**
     * Sends a color-choice request.
     *
     * @param request color-choice request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult chooseColor(ChooseColorRequest request) throws IOException {
        return exchange(request);
    }

    /**
     * Sends a pass request.
     *
     * @param request pass request
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult pass(PassRequest request) throws IOException {
        return exchange(request);
    }

    /**
     * Requests the global game rankings.
     *
     * @return operation result from the server
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult getRankings(RankingsRequest request) throws IOException {
        return exchange(request);
    }

    /**
     * Subscribes to game updates by installing an update listener in the socket
     * client context. Any existing subscription is removed before the new one is
     * sent.
     *
     * @param request subscription request
     * @param listener listener invoked by the reader thread for update messages
     * @return operation result from the server
     * @throws IllegalArgumentException if the listener is null
     * @throws IOException if request exchange fails
     */
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

    /**
     * Unsubscribes from the active update stream.
     * When no subscription is active, clears the listener context and returns a
     * successful no-op result.
     *
     * @param request unsubscription request
     * @return operation result from the server, or a successful no-op result
     * @throws IOException if request exchange fails
     */
    @Override
    public GameOperationResult unsubscribe(UnsubscribeGameRequest request) throws IOException {
        if (activeSubscription == null) {
            context = new SocketClientContext(responses, null);
            return GameOperationResult.success("No active subscription", null);
        }

        GameOperationResult result = exchange(request);
        if (result.success()) {
            activeSubscription = null;
            heartbeatGameName = null;
            context = new SocketClientContext(responses, null);
        }
        return result;
    }

    /**
     * Sends a request and waits for the next operation response from the reader
     * thread.
     *
     * @param request request to send
     * @return operation result received from the server
     * @throws IOException if the connection closes, the wait is interrupted, or
     *                     request serialization fails
     */
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

    /**
     * Closes the socket client, best-effort unsubscribing first, stopping
     * heartbeat, closing the socket, and interrupting the reader thread.
     *
     * @throws IOException if closing the underlying socket fails
     */
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

    /**
     * Starts periodic heartbeat after cancelling any previous heartbeat task.
     */
    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                this::sendHeartbeat,
                NetworkDefaults.HEARTBEAT_INTERVAL_MILLIS,
                NetworkDefaults.HEARTBEAT_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cancels the active heartbeat task, if one exists.
     */
    private void stopHeartbeat() {
        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(true);
            heartbeatTask = null;
        }
    }

    /**
     * Sends one heartbeat when the client is running, heartbeat is enabled, and
     * a nickname is known. I/O failures are ignored so the scheduled task can
     * keep running.
     */
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


    /**
     * Reads server messages, dispatching updates through the current context
     * and queuing operation results for waiting request exchanges.
     * Socket closure is treated as normal shutdown; other failures enqueue a
     * failure result while the client is still marked running.
     */
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
                context.responses().offer(GameOperationResult.failure("Socket connection closed: " + exception.getMessage(), null));
            }
        } finally {
            running = false;
        }
    }

    /**
     * Mutable reader-thread context containing the response queue and current
     * update listener.
     *
     * @param responses queue used to return operation results to request calls
     * @param updateListener listener for asynchronous updates, or null when not
     *                       subscribed
     */
    /**
     * Holds the response queue and optional update listener used by the socket
     * reader thread.
     */
    public record SocketClientContext (BlockingQueue<GameOperationResult> responses, GameUpdateListener updateListener) { }
}
