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

public final class MesosGuiService implements AutoCloseable {
    private final Object stateLock;
    private final ExecutorService ioExecutor;

    private volatile GameClient client;
    private volatile Consumer<GameServerMessage> updateHandler;
    private volatile String currentGameName;
    private volatile String currentNickname;

    public MesosGuiService() {
        this.stateLock = new Object();
        this.ioExecutor = Executors.newFixedThreadPool(4, new GuiThreadFactory());
    }

    public void setUpdateHandler(Consumer<GameServerMessage> updateHandler) {
        this.updateHandler = updateHandler == null ? _ -> { } : updateHandler;
    }

    public String currentGameName() {
        return currentGameName;
    }

    public String currentNickname() {
        return currentNickname;
    }

    public boolean hasJoinedGame() {
        return currentGameName != null && currentNickname != null;
    }

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

    public CompletableFuture<GameOperationResult> fetchLobby() {
        return runAsync(() -> requireClient().getLobby());
    }

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

    public CompletableFuture<GameOperationResult> rejoinGame(String nickname) {
        return runAsync(() -> rejoinAndSubscribeInternal(requireClient(), nickname));
    }

    public void clearCurrentNickname() {
        synchronized (stateLock) {
            currentGameName = null;
            currentNickname = null;
        }
    }

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

    public CompletableFuture<GameOperationResult> createGame(String gameName, int playerCount) {
        return runAsync(() -> requireClient().createGame(new CreateGameRequest(gameName, playerCount)));
    }

    public CompletableFuture<GameOperationResult> joinGame(String gameName) {
        return runAsync(() -> joinAndSubscribeInternal(requireClient(), gameName, requireNickname()));
    }

    public CompletableFuture<GameOperationResult> fetchGameSnapshot(String gameName) {
        return runAsync(() -> requireClient().getSnapshot(new SnapshotRequest(gameName)));
    }

    public CompletableFuture<GameOperationResult> refreshSnapshot() {
        return runAsync(() -> requireJoinedClient()
                .getSnapshot(new SnapshotRequest(currentGameName)));
    }

    public CompletableFuture<GameOperationResult> chooseOffer(int offerIndex) {
        return runAsync(() -> requireJoinedClient()
                .chooseOffer(new ChooseOfferRequest(currentGameName, currentNickname, offerIndex)));
    }

    public CompletableFuture<GameOperationResult> chooseCard(int cardIndex, boolean topRow) {
        return runAsync(() -> requireJoinedClient()
                .chooseCard(new ChooseCardRequest(currentGameName, currentNickname, cardIndex, topRow)));
    }

    public CompletableFuture<GameOperationResult> pass() {
        return runAsync(() -> requireJoinedClient()
                .pass(new PassRequest(currentGameName, currentNickname)));
    }

    public CompletableFuture<GameOperationResult> requestRankings() {
        return runAsync(() -> requireClient()
                .getRankings(new RankingsRequest(currentGameName)));
    }

    public CompletableFuture<GameOperationResult> reserveColorForGame(String gameName, Color color) {
        return runAsync(() -> requireClient()
                .chooseColor(new ChooseColorRequest(gameName, requireNickname(), color)));
    }

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

    private GameOperationResult joinAndSubscribeInternal(GameClient activeClient, String gameName, String nickname)
            throws IOException {
        GameOperationResult joinResult = activeClient.joinGame(new JoinGameRequest(gameName, nickname));
        if (!joinResult.success()) {
            return joinResult;
        }

        return subscribeToJoinedGame(activeClient, gameName, nickname, joinResult);
    }

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

    private String requireNickname() {
        String nickname = currentNickname;
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalStateException("No nickname has been selected");
        }
        return nickname;
    }

    private GameSnapshot snapshotOrFallback(GameOperationResult preferred, GameOperationResult fallback) {
        if (preferred != null && preferred.snapshot() != null) {
            return preferred.snapshot();
        }
        if (fallback != null) {
            return fallback.snapshot();
        }
        return null;
    }

    private void forwardUpdate(GameServerMessage update) {
        if (update == null) {
            return;
        }

        Consumer<GameServerMessage> handler = updateHandler;
        if (handler != null) {
            handler.accept(update);
        }
    }

    private GameClient requireClient() {
        GameClient activeClient = client;
        if (activeClient == null) {
            throw new IllegalStateException("Client is not connected");
        }
        return activeClient;
    }

    private GameClient requireJoinedClient() {
        if (!hasJoinedGame()) {
            throw new IllegalStateException("No active game session");
        }
        return requireClient();
    }

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

    private <T> CompletableFuture<T> runAsync(IoSupplier<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return action.get();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, ioExecutor);
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws Exception;
    }

    private static final class GuiThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "mesos-gui-io-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
