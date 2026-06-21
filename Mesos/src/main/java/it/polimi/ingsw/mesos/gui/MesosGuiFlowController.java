package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.dto.event.EventLogEntry;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.dto.game.GameSnapshot;
import it.polimi.ingsw.mesos.dto.game.PlayerSummary;
import it.polimi.ingsw.mesos.dto.lobby.LobbySnapshot;
import it.polimi.ingsw.mesos.network.config.ConnectionType;
import it.polimi.ingsw.mesos.network.protocol.OperationErrorCode;
import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessage;
import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessageVisitor;
import it.polimi.ingsw.mesos.network.protocol.messages.EventNotification;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.protocol.messages.GameViewUpdate;
import it.polimi.ingsw.mesos.network.protocol.messages.ResultType;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Coordinates GUI screen transitions, asynchronous game operations, and
 * server-pushed updates for the JavaFX client.
 */
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
    private final List<EventLogEntry> eventLog = new ArrayList<>();

    /**
     * Creates the coordinator that connects GUI screens to asynchronous game operations.
     *
     * @param host application shell that displays content and dialogs
     * @param guiService service that performs network operations
     * @param assetResolver resolver passed to the game board
     * @param screenFactory factory for connection, nickname, lobby, and waiting screens
     * @throws NullPointerException if any dependency is null
     */
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

    /**
     * Shows the initial connection screen.
     */
    public void start() {
        showConnectionScreen();
    }

    /**
     * Schedules a server-pushed message for dispatch on the JavaFX application thread.
     *
     * @param update server message to dispatch
     */
    public void handleRemoteUpdate(GameServerMessage update) {
        Platform.runLater(() -> update.accept(new GameServerMessageVisitor() {
            /**
             * Adds an event notification to history and shows it as a popup.
             *
             * @param notification event notification received from the server
             */
            public void visitEventNotification(EventNotification notification) {
                eventLog.add(notification.entry());
                if (gameBoardView != null) {
                    gameBoardView.setEventLog(eventLog);
                }
                host.showEventPopup(notification.entry());
            }

            /**
             * Updates status text and re-renders the board from a pushed snapshot.
             *
             * @param view update containing the latest snapshot
             */
            public void visitGameViewUpdate(GameViewUpdate view) {
                String message = MesosUserMessageMapper.describeUpdate(view);
                host.updateStatus(message);
                renderSnapshot(view.snapshot(), message);
            }
        }));
    }

    /**
     * Resets nickname-screen state and displays the transport connection screen.
     */
    private void showConnectionScreen() {
        nicknameScreen = null;
        host.updateStatus("Choose a connection type and the server host.");
        host.setContent(screenFactory.buildConnectionScreen(this::handleConnect));
    }

    /**
     * Displays the nickname-registration screen for the current connection.
     *
     * @param suggestedNickname text to prefill in the nickname field
     */
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

    /**
     * Displays the lobby using current connection and identity information.
     *
     * @param lobbySnapshot lobby data to render
     */
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

    /**
     * Displays the waiting room for a game that has not started yet.
     *
     * @param snapshot pre-game state to render
     * @param message status message to display
     */
    private void showWaitingRoom(GameSnapshot snapshot, String message) {
        host.setContent(screenFactory.buildWaitingRoomScreen(
                snapshot,
                guiService.currentNickname(),
                message,
                this::refreshCurrentSnapshot,
                this::disconnectToConnectionScreen
        ));
    }

    /**
     * Displays or refreshes the game board and requests global rankings after a finish.
     *
     * @param snapshot game state to render
     * @param message current status message
     */
    private void showGameScreen(GameSnapshot snapshot, String message) {
        if (gameBoardView == null) {
            gameBoardView = new MesosGameBoardView(
                    guiService.currentNickname(),
                    assetResolver,
                    this::refreshCurrentSnapshot,
                    this::disconnectToConnectionScreen,
                    this::chooseOffer,
                    this::chooseCard,
                    this::choosePass,
                    () -> withBusy(
                            "Returning to lobby...",
                            guiService.leaveGame().thenCompose(ignored -> guiService.fetchLobby()),
                            result -> handleLobbyResult(result, "Returned to lobby."),
                            "Could not return to lobby"
                    )
            );
        }
        gameBoardView.updateSnapshot(snapshot);
        if (snapshot.finished()) {
            withBusy("Requesting global leaderboard, ",
                    guiService.requestRankings(),
                    this::handleGloablLeaderboard,
                    "Cannot receive gloabl leaderboard");
        }
        gameBoardView.setEventLog(eventLog);
        host.setContent(gameBoardView);
    }

    /**
     * Validates connection input and starts an asynchronous transport connection.
     *
     * @param connectionType selected transport type, possibly null
     * @param hostValue host text entered by the user, possibly null
     */
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

    /**
     * Validates and submits the nickname entered on the nickname screen.
     *
     * @param rawNickname raw user input, possibly null
     */
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

    /**
     * Interprets a nickname response, continuing to lobby or reconnect handling.
     *
     * @param nickname nickname that was submitted
     * @param result server response, possibly null
     */
    private void handleNicknameResult(String nickname, GameOperationResult result) {
        String message = MesosUserMessageMapper.describeResult(result, "Nickname accepted.");
        host.updateStatus(message);

        if (result == null) {
            return;
        }

        if (!result.success()) {
            if (GameOperationResult.errorCodeOf(result) == OperationErrorCode.PLAYER_NAME_TAKEN) {
                handleTakenNickname();
                return;
            }

            host.showAlert("Nickname rejected", message);
            return;
        }

        if (!((GameOperationResult.Nickname) result).rejoinAvailable()) {
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

    /**
     * Clears and selects the nickname field after a duplicate-name rejection.
     */
    private void handleTakenNickname() {
        guiService.clearCurrentNickname();
        selectNicknameField();
        host.updateStatus("Choose another nickname.");
        host.showAlert(
                "Nickname already in use",
                "This nickname is already in use. Choose another nickname."
        );
    }

    /**
     * Starts an asynchronous attempt to rejoin a disconnected session.
     *
     * @param nickname nickname associated with the prior session
     */
    private void rejoinGame(String nickname) {
        withBusy(
                "Rejoining previous session...",
                guiService.rejoinGame(nickname),
                this::handleRejoinResult,
                "Rejoin failed"
        );
    }

    /**
     * Routes a rejoin response to a game, lobby, or nickname retry path.
     *
     * @param result rejoin response, possibly null
     */
    private void handleRejoinResult(GameOperationResult result) {
        String successMessage = GameOperationResult.lobbyOf(result) != null
                && GameOperationResult.snapshotOf(result) == null
                ? "Lobby refreshed."
                : "Rejoined game.";
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);

        if (result == null) {
            return;
        }

        var snapshot = GameOperationResult.snapshotOf(result);
        var lobby = GameOperationResult.lobbyOf(result);

        if (snapshot != null && guiService.hasJoinedGame()) {
            renderSnapshot(snapshot, message);
            if (!result.success()) {
                host.showAlert("Live updates unavailable", message);
            }
            return;
        }

        if (result.success() && lobby != null) {
            showLobbyScreen(lobby);
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

    /**
     * Loads the lobby asynchronously and uses the supplied message on success.
     *
     * @param successMessage status text for a successful refresh
     */
    private void loadLobby(String successMessage) {
        withBusy(
                "Loading lobby...",
                guiService.fetchLobby(),
                result -> handleLobbyResult(result, successMessage),
                "Lobby request failed"
        );
    }

    /**
     * Displays a lobby response or reports unavailable lobby data.
     *
     * @param result lobby response, possibly null
     * @param successMessage status text for a successful response
     */
    private void handleLobbyResult(GameOperationResult result, String successMessage) {
        if (result == null) {
            host.updateStatus("Lobby unavailable.");
            host.showAlert("Lobby unavailable", "The server did not return lobby data.");
            return;
        }

        if (!result.success() || GameOperationResult.lobbyOf(result) == null) {
            String message = MesosUserMessageMapper.describeResult(result, successMessage);
            host.updateStatus(message);
            host.showAlert("Lobby unavailable", message);
            return;
        }

        host.updateStatus(MesosUserMessageMapper.describeResult(result, successMessage));
        showLobbyScreen(GameOperationResult.lobbyOf(result));
    }

    /**
     * Prompts for a totem and creates a new game before joining it.
     *
     * @param gameName requested game name
     * @param playerCount requested player count
     */
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

    /**
     * Loads a game's pre-join snapshot before prompting for an available totem.
     *
     * @param gameName game to join
     */
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

    /**
     * Checks that a game name and selected nickname exist before a lobby action.
     *
     * @param gameName game name to validate, possibly null
     * @return true when both required inputs are available
     */
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

    /**
     * Routes a join response to the game board, a lobby refresh, or an error alert.
     *
     * @param result join response, possibly null
     * @param successMessage status text for a successful join
     */
    private void handleJoinFlowResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);

        if (result == null) {
            return;
        }

        var snapshot = GameOperationResult.snapshotOf(result);
        if (snapshot != null && guiService.hasJoinedGame()) {
            preJoinColorByGame.remove(snapshot.gameName());
            renderSnapshot(snapshot, message);
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

    /**
     * Routes a snapshot to the waiting room or game board based on its start state.
     *
     * @param snapshot state to render, possibly null
     * @param message status message to display
     */
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

    /**
     * Submits the local player's chosen offer tile.
     *
     * @param offerIndex selected offer-tile index
     */
    private void chooseOffer(int offerIndex) {
        withBusy(
                "Sending offer selection...",
                guiService.chooseOffer(offerIndex),
                result -> handleMutatingGameActionResult(result, "Offer tile selected."),
                "Offer selection failed"
        );
    }

    /**
     * Submits the local player's chosen market card.
     *
     * @param cardIndex selected card index
     * @param topRow whether the card is in the top market row
     */
    private void chooseCard(int cardIndex, boolean topRow) {
        withBusy(
                "Sending card selection...",
                guiService.chooseCard(cardIndex, topRow),
                result -> handleMutatingGameActionResult(result, "Card selected."),
                "Card selection failed"
        );
    }

    /**
     * Requests that the local player skip an optional extra pick.
     */
    private void choosePass() {
        withBusy(
                "Skipping optional extra pick...",
                guiService.pass(),
                result -> handleMutatingGameActionResult(result, "Optional extra pick skipped."),
                "Pass request failed"
        );
    }

    /**
     * Requests and renders a fresh snapshot for the current game.
     */
    private void refreshCurrentSnapshot() {
        withBusy(
                "Refreshing board...",
                guiService.refreshSnapshot(),
                result -> handleSnapshotResult(result, "Board refreshed."),
                "Snapshot refresh failed"
        );
    }

    /**
     * Updates status and renders the direct response to a mutating game action.
     *
     * @param result action response, possibly null
     * @param successMessage status text for success
     */
    private void handleMutatingGameActionResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);
        // Render the direct operation result immediately for responsiveness.
        // Later subscribed updates remain the authoritative refresh path.
        renderResultSnapshot(result, message);
    }

    /**
     * Updates status and renders a snapshot response when it contains a snapshot.
     *
     * @param result snapshot response, possibly null
     * @param successMessage status text for success
     */
    private void handleSnapshotResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        host.updateStatus(message);
        renderResultSnapshot(result, message);
    }

    /**
     * Renders the snapshot embedded in a server result when present.
     *
     * @param result response that may contain a snapshot
     * @param message status message to accompany the snapshot
     */
    private void renderResultSnapshot(GameOperationResult result, String message) {
        var snapshot = GameOperationResult.snapshotOf(result);
        if (snapshot != null) {
            renderSnapshot(snapshot, message);
        }
    }

    /**
     * Supplies returned all-time rankings to the finished-game board.
     *
     * @param result ranking response
     */
    private void handleGloablLeaderboard(GameOperationResult result) {
        var rankings = result.type() == ResultType.RANKINGS
                ? ((GameOperationResult.Rankings) result).rankings()
                : null;
        gameBoardView.updateGlobalLeaderboard(rankings);
    }

    /**
     * Disconnects asynchronously, clears session state, and returns to the connection screen.
     */
    private void disconnectToConnectionScreen() {
        withBusy(
                "Disconnecting...",
                guiService.disconnect(),
                ignored -> {
                    clearSessionState();
                    showConnectionScreen();
                    host.updateStatus("Disconnected.");
                },
                "Disconnect failed"
        );
    }

    /**
     * Clears connection-specific screens, selected colors, and event history.
     */
    private void clearSessionState() {
        connectedType = null;
        connectedHost = null;
        gameBoardView = null;
        nicknameScreen = null;
        preJoinColorByGame.clear();
        eventLog.clear();
    }

    /**
     * Recovers from a lost transport by resetting the UI to the connection screen.
     *
     * @param status failure status to display after the reset
     */
    private void returnToConnectionScreen(String status) {
        // The transport is already gone, so release the dead client best-effort
        // (disconnect() closes quietly) and reset the UI to the connection screen,
        // letting the user reconnect instead of being stranded on a dead board.
        guiService.disconnect();
        clearSessionState();
        showConnectionScreen();
        host.updateStatus(status);
    }

    /**
     * Runs a future while showing busy state and routes completion to the JavaFX thread.
     * Connection failures reset the UI; other failures show an alert.
     *
     * @param busyMessage busy-overlay text
     * @param future asynchronous operation to observe
     * @param onSuccess handler for a successful result
     * @param errorTitle title for a failure alert
     * @param <T> result type of the operation
     */
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
                if (connectedType != null && MesosUserMessageMapper.isConnectionLost(throwable)) {
                    returnToConnectionScreen(message);
                } else {
                    host.updateStatus(message);
                }
                host.showAlert(errorTitle, message);
                return;
            }
            onSuccess.accept(value);
        }));
    }

    /**
     * Clears and focuses the active nickname text field when it is available.
     */
    private void clearNicknameField() {
        TextField field = nicknameField();
        if (field == null) {
            return;
        }
        field.clear();
        field.requestFocus();
    }

    /**
     * Focuses and selects all text in the active nickname field when available.
     */
    private void selectNicknameField() {
        TextField field = nicknameField();
        if (field == null) {
            return;
        }
        field.requestFocus();
        field.selectAll();
    }

    /**
     * Returns the nickname field of the currently displayed nickname screen.
     *
     * @return nickname field, or null when that screen is not active
     */
    private TextField nicknameField() {
        return nicknameScreen == null ? null : nicknameScreen.nicknameField();
    }

    /**
     * Interprets game creation and continues with color reservation after success.
     *
     * @param gameName created game name
     * @param selectedColor color selected by the creator
     * @param result creation response, possibly null
     */
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

    /**
     * Opens the totem selector using the pre-join snapshot's unavailable colors.
     *
     * @param gameName game about to be joined
     * @param result snapshot response, possibly null
     */
    private void handlePreJoinSnapshot(String gameName, GameOperationResult result) {
        String message = MesosUserMessageMapper.describeResult(result, "Board refreshed.");
        host.updateStatus(message);
        var snapshot = GameOperationResult.snapshotOf(result);
        if (result == null || !result.success() || snapshot == null) {
            host.showAlert("Could not open the totem picker", message);
            return;
        }

        Color selectedColor = host.showTotemSelection(
                "Choose your totem",
                gameName,
                unavailableColors(snapshot),
                preJoinColorByGame.get(gameName)
        );
        if (selectedColor == null) {
            host.updateStatus("Join cancelled.");
            return;
        }
        reserveColorThenJoin(gameName, selectedColor, true);
    }

    /**
     * Reserves a selected color before sending the actual join request.
     *
     * @param gameName game to join
     * @param selectedColor color to reserve
     * @param allowRetry whether a taken-color response may reopen the selector
     */
    private void reserveColorThenJoin(String gameName, Color selectedColor, boolean allowRetry) {
        withBusy(
                "Reserving totem...",
                guiService.reserveColorForGame(gameName, selectedColor),
                result -> handleReserveColorResult(gameName, selectedColor, result, allowRetry),
                "Color selection failed"
        );
    }

    /**
     * Handles color reservation, retrying the selector for a concurrently taken color.
     *
     * @param gameName game to join
     * @param selectedColor reserved color
     * @param result reservation response, possibly null
     * @param allowRetry whether a taken color can be retried
     */
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
            var snapshot = GameOperationResult.snapshotOf(result);
            if (allowRetry && GameOperationResult.errorCodeOf(result) == OperationErrorCode.COLOR_ALREADY_TAKEN
                    && snapshot != null) {
                Color retryColor = host.showTotemSelection(
                        "Choose your totem",
                        gameName,
                        unavailableColors(snapshot),
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

    /**
     * Collects reserved colors, falling back to colors already assigned to players.
     *
     * @param snapshot game snapshot, possibly null
     * @return insertion-ordered set of unavailable colors
     */
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
                .map(PlayerSummary::color)
                .filter(Objects::nonNull)
                .forEach(unavailableColors::add);
        return unavailableColors;
    }

    /**
     * Defines the application-shell operations the GUI flow controller uses
     * to display content, status, dialogs, and event popups.
     */
    public interface Host {
        /**
         * Replaces the application's primary content node.
         *
         * @param node content to display
         */
        void setContent(Node node);

        /**
         * Updates the application's visible status message.
         *
         * @param message status text to display
         */
        void updateStatus(String message);

        /**
         * Shows or hides the application's busy indicator.
         *
         * @param busy whether work is in progress
         * @param message busy message, possibly null when hiding
         */
        void setBusy(boolean busy, String message);

        /**
         * Shows an informational alert to the user.
         *
         * @param title alert heading
         * @param message alert body
         */
        void showAlert(String title, String message);

        /**
         * Shows a transient popup for a resolved game event.
         *
         * @param entry event to display
         */
        void showEventPopup(EventLogEntry entry);

        /**
         * Shows a confirmation dialog and returns the user's decision.
         *
         * @param title dialog heading
         * @param message dialog body
         * @return true when the user confirms
         */
        boolean showConfirmation(String title, String message);

        /**
         * Prompts the user to choose an available totem color.
         *
         * @param title dialog heading
         * @param gameName game for which a color is chosen
         * @param takenColors colors that cannot be selected
         * @param selectedColor color to preselect, possibly null
         * @return selected color, or null when the dialog is cancelled
         */
        Color showTotemSelection(String title, String gameName, Set<Color> takenColors, Color selectedColor);
    }
}
