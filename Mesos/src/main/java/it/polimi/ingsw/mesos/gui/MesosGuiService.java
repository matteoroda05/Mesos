package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameClientFactory;
import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Model.Enums.Color;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Owns the GUI's network client and executes its blocking game operations
 * asynchronously while retaining connection identity and subscription state.
 */
public final class MesosGuiService implements AutoCloseable {
    private final Object stateLock;
    private final ExecutorService ioExecutor;

    private volatile GameClient client;
    private volatile Consumer<GameServerMessage> updateHandler;
    private volatile String currentGameName;
    private volatile String currentNickname;

    /**
     * Creates a GUI service with a dedicated daemon pool for blocking network calls.
     */
    public MesosGuiService() {
        this.stateLock = new Object();
        this.ioExecutor = Executors.newFixedThreadPool(4, new GuiThreadFactory());
    }

    /**
     * Sets the recipient of server-pushed updates.
     * A null handler is replaced with a no-op consumer.
     *
     * @param updateHandler consumer that receives non-null server messages
     */
    public void setUpdateHandler(Consumer<GameServerMessage> updateHandler) {
        this.updateHandler = updateHandler == null ? _ -> { } : updateHandler;
    }

    /**
     * Returns the game currently joined by this service.
     *
     * @return joined game name, or null when no game is joined
     */
    public String currentGameName() {
        return currentGameName;
    }

    /**
     * Returns the nickname selected for this connection.
     *
     * @return selected nickname, or null when none is set
     */
    public String currentNickname() {
        return currentNickname;
    }

    /**
     * Checks whether both a game and nickname are available for game operations.
     *
     * @return true when this service has an active joined-game identity
     */
    public boolean hasJoinedGame() {
        return currentGameName != null && currentNickname != null;
    }

    /**
     * Connects a new transport client and clears any previous game session.
     *
     * @param connectionType transport type to create
     * @param host server host to contact
     * @return future completed after the client is connected
     * @throws NullPointerException if the connection type is null
     * @throws IllegalArgumentException if the host is null or blank
     */
    public CompletableFuture<Void> connect(ConnectionType connectionType, String host) {
        Objects.requireNonNull(connectionType, "connectionType cannot be null");
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }

        return runAsync(() -> {
            synchronized (stateLock) {
                closeClientQuietly();
                client = GameClientFactory.connect(connectionType, host);
                currentGameName = null;
                currentNickname = null;
            }
            return null;
        });
    }

    /**
     * Retrieves the lobby asynchronously through the current client.
     *
     * @return future containing the lobby operation result
     */
    public CompletableFuture<GameOperationResult> fetchLobby() {
        return runAsync(() -> requireClient().getLobby());
    }

    /**
     * Submits a nickname and retains it when the server accepts a fresh session.
     *
     * @param nickname nickname to register
     * @return future containing the server result
     */
    public CompletableFuture<GameOperationResult> chooseNickname(String nickname) {
        return runAsync(() -> {
            GameOperationResult nicknameResult = requireClient().nickname(new NicknameRequest(nickname));
            if (nicknameResult.success() && !nicknameResult.rejoinAvailable()) {
                currentGameName = null;
                currentNickname = nickname;
            }
            return nicknameResult;
        });
    }

    /**
     * Attempts to rejoin the disconnected game associated with a nickname.
     *
     * @param nickname nickname to reconnect
     * @return future containing the rejoin and subscription result
     */
    public CompletableFuture<GameOperationResult> rejoinGame(String nickname) {
        return runAsync(() -> rejoinAndSubscribeInternal(requireClient(), nickname));
    }

    /**
     * Clears the local game and nickname state without closing the transport.
     */
    public void clearCurrentNickname() {
        synchronized (stateLock) {
            currentGameName = null;
            currentNickname = null;
        }
    }

    /**
     * Unsubscribes from the joined game when possible and clears its local name.
     *
     * @return future completed after the leave attempt
     */
    public CompletableFuture<Void> leaveGame() {
        return runAsync(() -> {
            GameClient activeClient = requireClient();
            String gameName = currentGameName;
            String nickname = currentNickname;
            if (gameName != null && nickname != null) {
                try {
                    activeClient.unsubscribe(new UnsubscribeGameRequest(gameName, nickname));
                } catch (IOException ignored) {
                }
                currentGameName = null;
            }
            return null;
        });
    }

    /**
     * Creates a game asynchronously through the current client.
     *
     * @param gameName requested game name
     * @param playerCount requested number of players
     * @return future containing the creation result
     */
    public CompletableFuture<GameOperationResult> createGame(String gameName, int playerCount) {
        return runAsync(() -> requireClient().createGame(new CreateGameRequest(gameName, playerCount)));
    }

    /**
     * Joins a game with the selected nickname and subscribes to its updates.
     *
     * @param gameName game to join
     * @return future containing the join and subscription result
     */
    public CompletableFuture<GameOperationResult> joinGame(String gameName) {
        return runAsync(() -> joinAndSubscribeInternal(requireClient(), gameName, requireNickname()));
    }

    /**
     * Retrieves a snapshot for the specified game asynchronously.
     *
     * @param gameName game to inspect
     * @return future containing the snapshot result
     */
    public CompletableFuture<GameOperationResult> fetchGameSnapshot(String gameName) {
        return runAsync(() -> requireClient().getSnapshot(new SnapshotRequest(gameName)));
    }

    /**
     * Retrieves a fresh snapshot for the currently joined game.
     *
     * @return future containing the snapshot result
     */
    public CompletableFuture<GameOperationResult> refreshSnapshot() {
        return runAsync(() -> requireJoinedClient()
                .getSnapshot(new SnapshotRequest(currentGameName)));
    }

    /**
     * Submits an offer-tile choice for the current player.
     *
     * @param offerIndex selected offer-tile index
     * @return future containing the choice result
     */
    public CompletableFuture<GameOperationResult> chooseOffer(int offerIndex) {
        return runAsync(() -> requireJoinedClient()
                .chooseOffer(new ChooseOfferRequest(currentGameName, currentNickname, offerIndex)));
    }

    /**
     * Submits a market-card choice for the current player.
     *
     * @param cardIndex selected card index
     * @param topRow whether the selected card is in the top row
     * @return future containing the choice result
     */
    public CompletableFuture<GameOperationResult> chooseCard(int cardIndex, boolean topRow) {
        return runAsync(() -> requireJoinedClient()
                .chooseCard(new ChooseCardRequest(currentGameName, currentNickname, cardIndex, topRow)));
    }

    /**
     * Requests that the current player skip an optional action.
     *
     * @return future containing the pass result
     */
    public CompletableFuture<GameOperationResult> pass() {
        return runAsync(() -> requireJoinedClient()
                .pass(new PassRequest(currentGameName, currentNickname)));
    }

    /**
     * Retrieves the ranking for the current game.
     *
     * @return future containing the ranking result
     */
    public CompletableFuture<GameOperationResult> requestRankings() {
        return runAsync(() -> requireClient()
                .getRankings(new RankingsRequest(currentGameName)));
    }

    /**
     * Reserves a totem color for the selected nickname before joining a game.
     *
     * @param gameName game in which to reserve the color
     * @param color requested totem color
     * @return future containing the reservation result
     */
    public CompletableFuture<GameOperationResult> reserveColorForGame(String gameName, Color color) {
        return runAsync(() -> requireClient()
                .chooseColor(new ChooseColorRequest(gameName, requireNickname(), color)));
    }

    /**
     * Closes the client asynchronously and clears all connection state.
     *
     * @return future completed after cleanup
     */
    public CompletableFuture<Void> disconnect() {
        return runAsync(() -> {
            synchronized (stateLock) {
                closeClientQuietly();
                client = null;
                currentGameName = null;
                currentNickname = null;
            }
            return null;
        });
    }

    /**
     * Closes the client synchronously and stops pending GUI I/O work.
     */
    @Override
    public void close() {
        synchronized (stateLock) {
            closeClientQuietly();
            client = null;
            currentGameName = null;
            currentNickname = null;
        }
        ioExecutor.shutdownNow();
    }

    /**
     * Joins a named game and subscribes after a successful join.
     *
     * @param activeClient connected client to use
     * @param gameName game to join
     * @param nickname joining player nickname
     * @return join failure or the ensuing subscription result
     * @throws IOException if either client call fails
     */
    private GameOperationResult joinAndSubscribeInternal(GameClient activeClient, String gameName, String nickname)
            throws IOException {
        GameOperationResult joinResult = activeClient.joinGame(new JoinGameRequest(gameName, nickname));
        if (!joinResult.success()) {
            return joinResult;
        }

        return subscribeToJoinedGame(activeClient, gameName, nickname, joinResult);
    }

    /**
     * Attempts a reconnect join and subscribes when the server returns a game snapshot.
     *
     * @param activeClient connected client to use
     * @param nickname reconnecting player nickname
     * @return rejoin failure or subscription result
     * @throws IOException if a client call fails
     */
    private GameOperationResult rejoinAndSubscribeInternal(GameClient activeClient, String nickname)
            throws IOException {
        GameOperationResult rejoinResult = activeClient.joinGame(new JoinGameRequest(null, nickname, true));
        if (!rejoinResult.success()) {
            return rejoinResult;
        }

        if (rejoinResult.snapshot() != null) {
            return subscribeToJoinedGame(activeClient, rejoinResult.snapshot().gameName(), nickname, rejoinResult);
        }

        currentGameName = null;
        currentNickname = nickname;
        return rejoinResult;
    }

    /**
     * Stores joined-game state and registers the service update-forwarding callback.
     *
     * @param activeClient connected client to use
     * @param gameName joined game name
     * @param nickname joined player nickname
     * @param joinResult successful join result used as a snapshot fallback
     * @return normalized result containing the best available snapshot
     * @throws IOException if subscription cannot be sent
     */
    private GameOperationResult subscribeToJoinedGame(
            GameClient activeClient,
            String gameName,
            String nickname,
            GameOperationResult joinResult
    ) throws IOException {
        currentGameName = gameName;
        currentNickname = nickname;

        GameOperationResult subscribeResult = activeClient.subscribe(
                new SubscribeGameRequest(gameName, nickname),
                this::forwardUpdate
        );
        if (subscribeResult.success()) {
            GameSnapshot snapshot = snapshotOrFallback(subscribeResult, joinResult);
            return GameOperationResult.success(joinResult.message(), snapshot, joinResult.lobbySnapshot());
        }

        GameSnapshot snapshot = snapshotOrFallback(subscribeResult, joinResult);
        return GameOperationResult.failure(
                subscribeResult.message(),
                subscribeResult.errorCode(),
                snapshot,
                subscribeResult.lobbySnapshot()
        );
    }

    /**
     * Returns the selected nickname or rejects operations without one.
     *
     * @return non-blank selected nickname
     * @throws IllegalStateException if no nickname has been selected
     */
    private String requireNickname() {
        String nickname = currentNickname;
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalStateException("No nickname has been selected");
        }
        return nickname;
    }

    /**
     * Selects a snapshot from the preferred result, then from a fallback result.
     *
     * @param preferred result whose snapshot has priority, possibly null
     * @param fallback result used when preferred has no snapshot, possibly null
     * @return selected snapshot, or null when neither result contains one
     */
    private GameSnapshot snapshotOrFallback(GameOperationResult preferred, GameOperationResult fallback) {
        if (preferred != null && preferred.snapshot() != null) {
            return preferred.snapshot();
        }
        if (fallback != null) {
            return fallback.snapshot();
        }
        return null;
    }

    /**
     * Forwards a non-null server update to the currently registered GUI handler.
     *
     * @param update server message to forward, possibly null
     */
    private void forwardUpdate(GameServerMessage update) {
        if (update == null) {
            return;
        }

        Consumer<GameServerMessage> handler = updateHandler;
        if (handler != null) {
            handler.accept(update);
        }
    }

    /**
     * Returns the connected client required for a network operation.
     *
     * @return active client
     * @throws IllegalStateException if the service is disconnected
     */
    private GameClient requireClient() {
        GameClient activeClient = client;
        if (activeClient == null) {
            throw new IllegalStateException("Client is not connected");
        }
        return activeClient;
    }

    /**
     * Returns the client required for an operation on the joined game.
     *
     * @return active client with joined-game state
     * @throws IllegalStateException if no game session is active
     */
    private GameClient requireJoinedClient() {
        if (!hasJoinedGame()) {
            throw new IllegalStateException("No active game session");
        }
        return requireClient();
    }

    /**
     * Attempts to unsubscribe and close the current client while suppressing I/O failures.
     */
    private void closeClientQuietly() {
        GameClient activeClient = client;
        if (activeClient == null) {
            return;
        }

        try {
            if (currentGameName != null && currentNickname != null) {
                activeClient.unsubscribe(new UnsubscribeGameRequest(currentGameName, currentNickname));
            }
        } catch (IOException ignored) {
        }

        try {
            activeClient.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Executes a potentially blocking operation on the GUI I/O executor.
     * Checked failures are wrapped in {@link CompletionException}.
     *
     * @param action operation to run
     * @param <T> operation result type
     * @return future representing the asynchronous operation
     */
    private <T> CompletableFuture<T> runAsync(IoSupplier<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return action.get();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, ioExecutor);
    }

    /**
     * Supplies a value from a GUI I/O task that may throw a checked exception.
     *
     * @param <T> supplied value type
     */
    @FunctionalInterface
    private interface IoSupplier<T> {
        /**
         * Performs an operation that may fail with a checked exception.
         *
         * @return operation result
         * @throws Exception if the operation cannot complete
         */
        T get() throws Exception;
    }

    /**
     * Creates named daemon threads for the GUI service's blocking I/O executor.
     */
    private static final class GuiThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger(1);

        /**
         * Creates a numbered daemon thread for GUI network I/O.
         *
         * @param runnable work executed by the thread
         * @return configured daemon thread
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "mesos-gui-io-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
