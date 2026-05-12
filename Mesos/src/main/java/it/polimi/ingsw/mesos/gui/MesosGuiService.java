package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameClientFactory;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;

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
    private volatile Consumer<GameViewUpdate> updateHandler;
    private volatile String currentGameName;
    private volatile String currentNickname;

    public MesosGuiService() {
        this.stateLock = new Object();
        this.ioExecutor = Executors.newFixedThreadPool(4, new GuiThreadFactory());
        this.updateHandler = update -> { };
    }

    public void setUpdateHandler(Consumer<GameViewUpdate> updateHandler) {
        this.updateHandler = updateHandler == null ? update -> { } : updateHandler;
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

    public CompletableFuture<GameOperationResult> createAndJoinGame(String gameName, int playerCount, String nickname) {
        return runAsync(() -> {
            GameClient activeClient = requireClient();
            GameOperationResult createResult = activeClient.createGame(new CreateGameRequest(gameName, playerCount));
            if (!createResult.success()) {
                return createResult;
            }

            return joinAndSubscribeInternal(activeClient, gameName, nickname);
        });
    }

    public CompletableFuture<GameOperationResult> joinGame(String gameName, String nickname) {
        return runAsync(() -> joinAndSubscribeInternal(requireClient(), gameName, nickname));
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

        currentGameName = gameName;
        currentNickname = nickname;

        GameOperationResult subscribeResult = activeClient.subscribe(
                new SubscribeGameRequest(gameName, nickname),
                this::forwardUpdate
        );
        if (subscribeResult.success()) {
            GameSnapshot snapshot = snapshotOrFallback(subscribeResult, joinResult);
            return GameOperationResult.success("Joined game", snapshot);
        }

        GameSnapshot snapshot = snapshotOrFallback(subscribeResult, joinResult);
        return GameOperationResult.failure(
                subscribeResult.message(),
                subscribeResult.errorCode(),
                snapshot,
                subscribeResult.lobbySnapshot()
        );
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

    private void forwardUpdate(GameViewUpdate update) {
        if (update == null) {
            return;
        }

        Consumer<GameViewUpdate> handler = updateHandler;
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
