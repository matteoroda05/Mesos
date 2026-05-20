package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class MesosGuiFlowController {
    private final Host host;
    private final MesosGuiService guiService;
    private final MesosAssetResolver assetResolver;
    private final MesosGuiScreenFactory screenFactory;

    private ConnectionType connectedType;
    private String connectedHost;
    private MesosGameBoardView gameBoardView;
    private MesosGuiScreenFactory.NicknameScreen nicknameScreen;
    private final Map<String, Color> preJoinColorByGame;

    public MesosGuiFlowController(
            Host host,
            MesosGuiService guiService,
            MesosAssetResolver assetResolver,
            MesosGuiScreenFactory screenFactory
    ) {
        this.host = Objects.requireNonNull(host, "host cannot be null");
        this.guiService = Objects.requireNonNull(guiService, "guiService cannot be null");
        this.assetResolver = Objects.requireNonNull(assetResolver, "assetResolver cannot be null");
        this.screenFactory = Objects.requireNonNull(screenFactory, "screenFactory cannot be null");
        this.preJoinColorByGame = new LinkedHashMap<>();
    }

    public void start() {
        showConnectionScreen();
    }

    public void handleRemoteUpdate(GameViewUpdate update) {
        Platform.runLater(() -> {
            String message = MesosUserMessageMapper.describeUpdate(update);
            host.updateStatus(message);
            renderSnapshot(update == null ? null : update.snapshot(), message);
        });
    }

    private void showConnectionScreen() {
        nicknameScreen = null;
        host.updateStatus("Choose a connection type and the server host.");
        host.setContent(screenFactory.buildConnectionScreen(this::handleConnect));
    }

    private void showNicknameScreen(String suggestedNickname) {
        host.updateStatus("Choose your nickname.");
        nicknameScreen = screenFactory.buildNicknameScreen(
                connectedType,
                connectedHost,
                suggestedNickname,
                this::submitNickname,
                this::disconnectToConnectionScreen
        );
        host.setContent(nicknameScreen.root());
    }

    private void showLobbyScreen(LobbySnapshot lobbySnapshot) {
        nicknameScreen = null;
        host.setContent(screenFactory.buildLobbyScreen(
                lobbySnapshot,
                guiService.currentNickname(),
                connectedType,
                connectedHost,
                () -> loadLobby("Lobby refreshed."),
                this::disconnectToConnectionScreen,
                this::joinGame,
                this::createAndJoinGame
        ));
    }

    private void showWaitingRoom(GameSnapshot snapshot, String message) {
        host.setContent(screenFactory.buildWaitingRoomScreen(
                snapshot,
                guiService.currentNickname(),
                message,
                this::refreshCurrentSnapshot,
                this::disconnectToConnectionScreen
        ));
    }

    private void showGameScreen(GameSnapshot snapshot, String message) {
        if (gameBoardView == null) {
            gameBoardView = new MesosGameBoardView(
                    guiService.currentNickname(),
                    assetResolver,
                    this::refreshCurrentSnapshot,
                    this::disconnectToConnectionScreen,
                    this::chooseOffer,
                    this::chooseCard
            );
        }
        gameBoardView.updateSnapshot(snapshot, message);
        host.setContent(gameBoardView);
    }

    private void handleConnect(ConnectionType connectionType, String hostValue) {
        if (connectionType == null) {
            host.showAlert("Missing connection type", "Choose either SOCKET or RMI before connecting.");
            return;
        }

        String hostName = hostValue == null ? "" : hostValue.trim();
        if (hostName.isBlank()) {
            host.showAlert("Missing host", "Enter the server host or IP address.");
            return;
        }

        withBusy(
                "Connecting to " + hostName + "...",
                guiService.connect(connectionType, hostName),
                ignored -> {
                    connectedType = connectionType;
                    connectedHost = hostName;
                    host.updateStatus("Connected via " + connectionType.name() + " to " + connectedHost + ".");
                    showNicknameScreen("");
                },
                "Connection failed"
        );
    }

    private void submitNickname(String rawNickname) {
        String nickname = rawNickname == null ? "" : rawNickname.trim();
        if (nickname.isBlank()) {
            host.showAlert("Missing nickname", "Enter the nickname you want to use.");
            return;
        }

        withBusy(
                "Checking nickname...",
                guiService.chooseNickname(nickname),
                result -> handleNicknameResult(nickname, result),
                "Nickname request failed"
        );
    }

    private void handleNicknameResult(String nickname, GameOperationResult result) {
        String message = MesosUserMessageMapper.describeResult(result, "Nickname accepted.");
        host.updateStatus(message);

        if (result == null) {
            return;
        }

        if (!result.success()) {
            if (result.errorCode() == OperationErrorCode.PLAYER_NAME_TAKEN && handleTakenNickname(nickname)) {
                return;
            }

            host.showAlert("Nickname rejected", message);
            return;
        }

        if (!result.rejoinAvailable()) {
            loadLobby("Lobby refreshed.");
            return;
        }

        boolean rejoin = host.showConfirmation(
                "Rejoin available",
                "A disconnected session exists for this nickname. Do you want to rejoin it?"
        );
        if (!rejoin) {
            guiService.clearCurrentNickname();
            clearNicknameField();
            host.updateStatus("Choose another nickname.");
            return;
        }

        rejoinGame(nickname);
    }

    private boolean handleTakenNickname(String nickname) {
        boolean useSuffix = host.showConfirmation(
                "Nickname already in use",
                "This nickname is already in use. Do you want to keep it with an automatic suffix?"
        );
        if (!useSuffix) {
            guiService.clearCurrentNickname();
            selectNicknameField();
            host.updateStatus("Choose another nickname.");
            return true;
        }

        String suffixedNickname = appendNumericSuffix(nickname, 1);
        TextField field = nicknameField();
        if (field != null) {
            field.setText(suffixedNickname);
        }
        submitNickname(suffixedNickname);
        return true;
    }

    private void rejoinGame(String nickname) {
        withBusy(
                "Rejoining previous session...",
                guiService.rejoinGame(nickname),
                this::handleRejoinResult,
                "Rejoin failed"
        );
    }

    private void handleRejoinResult(GameOperationResult result) {
        String successMessage = result != null && result.lobbySnapshot() != null && result.snapshot() == null
                ? "Lobby refreshed."
                : "Rejoined game.";
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);

        if (result == null) {
            return;
        }

        if (result.snapshot() != null && guiService.hasJoinedGame()) {
            renderSnapshot(result.snapshot(), message);
            if (!result.success()) {
                host.showAlert("Live updates unavailable", message);
            }
            return;
        }

        if (result.success() && result.lobbySnapshot() != null) {
            showLobbyScreen(result.lobbySnapshot());
            return;
        }

        if (result.success()) {
            loadLobby("Lobby refreshed.");
            return;
        }

        guiService.clearCurrentNickname();
        clearNicknameField();
        host.showAlert("Rejoin failed", message);
    }

    private void loadLobby(String successMessage) {
        withBusy(
                "Loading lobby...",
                guiService.fetchLobby(),
                result -> handleLobbyResult(result, successMessage),
                "Lobby request failed"
        );
    }

    private void handleLobbyResult(GameOperationResult result, String successMessage) {
        if (result == null) {
            host.updateStatus("Lobby unavailable.");
            host.showAlert("Lobby unavailable", "The server did not return lobby data.");
            return;
        }

        if (!result.success() || result.lobbySnapshot() == null) {
            String message = MesosUserMessageMapper.describeResult(result, successMessage);
            host.updateStatus(message);
            host.showAlert("Lobby unavailable", message);
            return;
        }

        host.updateStatus(MesosUserMessageMapper.describeResult(result, successMessage));
        showLobbyScreen(result.lobbySnapshot());
    }

    private void createAndJoinGame(String gameName, Integer playerCount) {
        if (!validateLobbyActionFields(gameName)) {
            return;
        }
        String normalizedGameName = gameName.trim();
        Color selectedColor = host.showTotemSelection(
                "Choose your totem",
                normalizedGameName,
                Set.of(),
                preJoinColorByGame.get(normalizedGameName)
        );
        if (selectedColor == null) {
            host.updateStatus("Game creation cancelled.");
            return;
        }

        withBusy(
                "Creating game...",
                guiService.createGame(normalizedGameName, playerCount),
                result -> handleCreateGameResult(normalizedGameName, selectedColor, result),
                "Game creation failed"
        );
    }

    private void joinGame(String gameName) {
        if (!validateLobbyActionFields(gameName)) {
            return;
        }
        String normalizedGameName = gameName.trim();
        withBusy(
                "Loading match details...",
                guiService.fetchGameSnapshot(normalizedGameName),
                result -> handlePreJoinSnapshot(normalizedGameName, result),
                "Could not open the totem picker"
        );
    }

    private boolean validateLobbyActionFields(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            host.showAlert("Missing game name", "Enter a game name or select one from the table.");
            return false;
        }
        if (guiService.currentNickname() == null || guiService.currentNickname().isBlank()) {
            host.showAlert("Missing nickname", "Choose a nickname before entering the lobby.");
            return false;
        }
        return true;
    }

    private void handleJoinFlowResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);

        if (result == null) {
            return;
        }

        if (result.snapshot() != null && guiService.hasJoinedGame()) {
            preJoinColorByGame.remove(result.snapshot().gameName());
            renderSnapshot(result.snapshot(), message);
            if (!result.success()) {
                host.showAlert("Live updates unavailable", message);
            }
            return;
        }

        if (!result.success()) {
            host.showAlert("Operation failed", message);
        } else {
            loadLobby(successMessage);
        }
    }

    private void renderSnapshot(GameSnapshot snapshot, String message) {
        if (snapshot == null) {
            return;
        }

        if (!snapshot.started()) {
            showWaitingRoom(snapshot, message);
            return;
        }

        showGameScreen(snapshot, message);
    }

    private void chooseOffer(int offerIndex) {
        withBusy(
                "Sending offer selection...",
                guiService.chooseOffer(offerIndex),
                result -> handleMutatingGameActionResult(result, "Offer tile selected."),
                "Offer selection failed"
        );
    }

    private void chooseCard(int cardIndex, boolean topRow) {
        withBusy(
                "Sending card selection...",
                guiService.chooseCard(cardIndex, topRow),
                result -> handleMutatingGameActionResult(result, "Card selected."),
                "Card selection failed"
        );
    }

    private void refreshCurrentSnapshot() {
        withBusy(
                "Refreshing board...",
                guiService.refreshSnapshot(),
                result -> handleSnapshotResult(result, "Board refreshed."),
                "Snapshot refresh failed"
        );
    }

    private void handleMutatingGameActionResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);
        // Render the direct operation result immediately for responsiveness.
        // Later subscribed updates remain the authoritative refresh path.
        renderResultSnapshot(result, message);
    }

    private void handleSnapshotResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);
        renderResultSnapshot(result, message);
    }

    private void renderResultSnapshot(GameOperationResult result, String message) {
        if (result != null && result.snapshot() != null) {
            renderSnapshot(result.snapshot(), message);
        }
    }

    private void disconnectToConnectionScreen() {
        withBusy(
                "Disconnecting...",
                guiService.disconnect(),
                ignored -> {
                    connectedType = null;
                    connectedHost = null;
                    gameBoardView = null;
                    nicknameScreen = null;
                    preJoinColorByGame.clear();
                    showConnectionScreen();
                    host.updateStatus("Disconnected.");
                },
                "Disconnect failed"
        );
    }

    private <T> void withBusy(
            String busyMessage,
            CompletableFuture<T> future,
            Consumer<T> onSuccess,
            String errorTitle
    ) {
        host.setBusy(true, busyMessage);
        future.whenComplete((value, throwable) -> Platform.runLater(() -> {
            host.setBusy(false, null);
            if (throwable != null) {
                String message = MesosUserMessageMapper.describeException(throwable);
                host.updateStatus(message);
                host.showAlert(errorTitle, message);
                return;
            }
            onSuccess.accept(value);
        }));
    }

    private void clearNicknameField() {
        TextField field = nicknameField();
        if (field == null) {
            return;
        }
        field.clear();
        field.requestFocus();
    }

    private void selectNicknameField() {
        TextField field = nicknameField();
        if (field == null) {
            return;
        }
        field.requestFocus();
        field.selectAll();
    }

    private TextField nicknameField() {
        return nicknameScreen == null ? null : nicknameScreen.nicknameField();
    }

    private String appendNumericSuffix(String baseName, int suffix) {
        return baseName + "-" + suffix;
    }

    private void handleCreateGameResult(String gameName, Color selectedColor, GameOperationResult result) {
        String message = MesosUserMessageMapper.describeResult(result, "Game created.");
        host.updateStatus(message);
        if (result == null) {
            return;
        }
        if (!result.success()) {
            host.showAlert("Game creation failed", message);
            return;
        }
        reserveColorThenJoin(gameName, selectedColor, true);
    }

    private void handlePreJoinSnapshot(String gameName, GameOperationResult result) {
        String message = MesosUserMessageMapper.describeResult(result, "Board refreshed.");
        host.updateStatus(message);
        if (result == null || !result.success() || result.snapshot() == null) {
            host.showAlert("Could not open the totem picker", message);
            return;
        }

        Color selectedColor = host.showTotemSelection(
                "Choose your totem",
                gameName,
                unavailableColors(result.snapshot()),
                preJoinColorByGame.get(gameName)
        );
        if (selectedColor == null) {
            host.updateStatus("Join cancelled.");
            return;
        }
        reserveColorThenJoin(gameName, selectedColor, true);
    }

    private void reserveColorThenJoin(String gameName, Color selectedColor, boolean allowRetry) {
        withBusy(
                "Reserving totem...",
                guiService.reserveColorForGame(gameName, selectedColor),
                result -> handleReserveColorResult(gameName, selectedColor, result, allowRetry),
                "Color selection failed"
        );
    }

    private void handleReserveColorResult(
            String gameName,
            Color selectedColor,
            GameOperationResult result,
            boolean allowRetry
    ) {
        String message = MesosUserMessageMapper.describeResult(result, "Color selected.");
        host.updateStatus(message);
        if (result == null) {
            return;
        }
        if (!result.success()) {
            if (allowRetry && result.errorCode() == OperationErrorCode.COLOR_ALREADY_TAKEN && result.snapshot() != null) {
                Color retryColor = host.showTotemSelection(
                        "Choose your totem",
                        gameName,
                        unavailableColors(result.snapshot()),
                        preJoinColorByGame.get(gameName)
                );
                if (retryColor != null) {
                    reserveColorThenJoin(gameName, retryColor, true);
                } else {
                    host.updateStatus("Join cancelled.");
                }
                return;
            }
            host.showAlert("Color selection failed", message);
            return;
        }

        preJoinColorByGame.put(gameName, selectedColor);
        withBusy(
                "Joining game...",
                guiService.joinGame(gameName),
                joinResult -> handleJoinFlowResult(joinResult, "Joined game."),
                "Could not join the selected game"
        );
    }

    private Set<Color> unavailableColors(GameSnapshot snapshot) {
        Set<Color> unavailableColors = new LinkedHashSet<>();
        if (snapshot == null) {
            return unavailableColors;
        }

        if (!snapshot.reservedColors().isEmpty()) {
            unavailableColors.addAll(snapshot.reservedColors());
            return unavailableColors;
        }

        snapshot.players().stream()
                .map(player -> player.color())
                .filter(Objects::nonNull)
                .forEach(unavailableColors::add);
        return unavailableColors;
    }

    public interface Host {
        void setContent(Node node);

        void updateStatus(String message);

        void setBusy(boolean busy, String message);

        void showAlert(String title, String message);

        boolean showConfirmation(String title, String message);

        Color showTotemSelection(String title, String gameName, Set<Color> takenColors, Color selectedColor);
    }
}
