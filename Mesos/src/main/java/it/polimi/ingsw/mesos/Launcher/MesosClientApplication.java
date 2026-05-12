package it.polimi.ingsw.mesos.Launcher;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.gui.MesosAssetResolver;
import it.polimi.ingsw.mesos.gui.MesosGameBoardView;
import it.polimi.ingsw.mesos.gui.MesosGuiService;
import it.polimi.ingsw.mesos.gui.MesosUserMessageMapper;
import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.LobbyGameSummary;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class MesosClientApplication extends Application {
    private final MesosGuiService guiService = new MesosGuiService();
    private final MesosAssetResolver assetResolver = new MesosAssetResolver();

    private Stage stage;
    private BorderPane appShell;
    private StackPane rootStack;
    private VBox busyOverlay;
    private Label statusBarLabel;

    private ConnectionType connectedType;
    private String connectedHost;
    private MesosGameBoardView gameBoardView;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.guiService.setUpdateHandler(this::handleRemoteUpdate);

        statusBarLabel = new Label("Ready.");
        statusBarLabel.getStyleClass().add("status-bar");

        appShell = new BorderPane();
        appShell.getStyleClass().add("app-shell");
        appShell.setPadding(new Insets(16));
        appShell.setMinSize(0, 0);
        appShell.setBottom(buildStatusBar());

        busyOverlay = buildBusyOverlay();
        rootStack = new StackPane(appShell, busyOverlay);
        rootStack.setMinSize(0, 0);
        busyOverlay.setVisible(false);
        busyOverlay.setManaged(false);

        Scene scene = new Scene(rootStack, 1480, 900);
        applyStylesheet(scene);

        stage.setTitle("MESOS");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(620);
        stage.setMinHeight(520);

        showConnectionScreen();
        stage.show();
    }

    @Override
    public void stop() {
        guiService.close();
    }

    private Node buildStatusBar() {
        HBox bar = new HBox(statusBarLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.getStyleClass().add("status-bar-shell");
        return bar;
    }

    private VBox buildBusyOverlay() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(84, 84);

        Label busyLabel = new Label("Working...");
        busyLabel.getStyleClass().add("view-title");

        VBox overlay = new VBox(16, indicator, busyLabel);
        overlay.getStyleClass().add("busy-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setFillWidth(false);
        return overlay;
    }

    private void applyStylesheet(Scene scene) {
        var stylesheet = MesosClientApplication.class.getResource("/it/polimi/ingsw/mesos/gui/mesos-gui.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private void showConnectionScreen() {
        updateStatus("Choose a connection type and the server host.");
        appShell.setCenter(buildConnectionView());
    }

    private Node buildConnectionView() {
        ComboBox<ConnectionType> connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getItems().setAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.SOCKET);
        connectionTypeBox.setMaxWidth(Double.MAX_VALUE);

        TextField hostField = new TextField(NetworkDefaults.DEFAULT_RMI_HOST);
        hostField.setPromptText("Server host");

        Button connectButton = new Button("Connect");
        connectButton.getStyleClass().add("primary-button");
        connectButton.setMaxWidth(Double.MAX_VALUE);
        connectButton.setOnAction(event -> handleConnect(connectionTypeBox.getValue(), hostField.getText()));

        VBox formCard = new VBox(
                14,
                sectionHeader("Connection", "Select the transport and the machine hosting the server."),
                buildField("Connection", connectionTypeBox, 220),
                buildField("Host", hostField, 220),
                connectButton
        );
        formCard.getStyleClass().addAll("panel-shell", "form-card");
        formCard.setPrefWidth(240);
        formCard.setMaxWidth(240);
        formCard.setTranslateY(54);

        VBox shell = new VBox(14, buildConnectionLogo(), formCard);
        shell.setAlignment(Pos.TOP_CENTER);
        shell.getStyleClass().addAll("screen-shell", "screen-shell-compact");
        shell.setMaxWidth(500);
        shell.setPadding(new Insets(6, 14, 12, 14));

        StackPane container = new StackPane(shell);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(6, 0, 6, 0));
        container.setTranslateY(96);
        return container;
    }

    private Node buildConnectionLogo() {
        var logoUrl = MesosClientApplication.class.getResource("/cards_png/Scritta_Mesos.png");
        if (logoUrl == null) {
            Label placeholder = new Label("MESOS");
            placeholder.getStyleClass().add("view-title");
            return placeholder;
        }

        ImageView imageView = new ImageView(new Image(logoUrl.toExternalForm(), true));
        imageView.setViewport(new Rectangle2D(1300, 1152, 2220, 656));
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(
                Bindings.max(420, Bindings.min(700, rootStack.widthProperty().subtract(220)))
        );
        imageView.setSmooth(true);

        StackPane wrapper = new StackPane(imageView);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(720);
        wrapper.setPadding(new Insets(0, 0, 0, 0));
        wrapper.setTranslateY(12); // nudge logo downward slightly more
        return wrapper;
    }

    private Node buildHeroCard() {
        StackPane visual = new StackPane();
        visual.getStyleClass().add("hero-visual");
        visual.setMinHeight(300);
        visual.setPrefHeight(360);
        visual.setMaxWidth(Double.MAX_VALUE);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(visual.widthProperty());
        clip.heightProperty().bind(visual.heightProperty());
        visual.setClip(clip);

        Optional<Image> heroImage = assetResolver.resolveHeroImage();
        if (heroImage.isPresent()) {
            ImageView imageView = new ImageView(heroImage.get());
            imageView.setPreserveRatio(true);
            imageView.fitWidthProperty().bind(visual.widthProperty().subtract(28));
            imageView.fitHeightProperty().bind(visual.heightProperty().subtract(28));
            imageView.setSmooth(true);
            visual.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("MESOS");
            placeholder.getStyleClass().add("view-title");
            visual.getChildren().add(placeholder);
        }

        Region tint = new Region();
        tint.getStyleClass().add("hero-tint");
        tint.prefWidthProperty().bind(visual.widthProperty());
        tint.prefHeightProperty().bind(visual.heightProperty());
        visual.getChildren().add(tint);

        VBox card = new VBox(
                16,
                visual,
            sectionHeader("Full game flow", "Connect, browse open rooms, create or join a match, wait for the start, then play through live shared state.")
        );
        card.getStyleClass().addAll("panel-shell", "hero-info-card");
        card.setPrefWidth(680);
        card.setMinWidth(340);
        card.setMaxWidth(680);
        return card;
    }

    private void handleConnect(ConnectionType connectionType, String host) {
        if (connectionType == null) {
            showAlert("Missing connection type", "Choose either SOCKET or RMI before connecting.");
            return;
        }
        if (host == null || host.isBlank()) {
            showAlert("Missing host", "Enter the server host or IP address.");
            return;
        }

        withBusy(
                "Connecting to " + host.trim() + "...",
                guiService.connect(connectionType, host.trim()),
                ignored -> {
                    connectedType = connectionType;
                    connectedHost = host.trim();
                    updateStatus("Connected via " + connectionType.name() + " to " + connectedHost + ".");
                    loadLobby("Lobby refreshed.");
                },
                "Connection failed"
        );
    }

    private void loadLobby(String successMessage) {
        withBusy(
                "Loading lobby...",
                guiService.fetchLobby(),
                result -> {
                    if (!result.success() || result.lobbySnapshot() == null) {
                        String message = MesosUserMessageMapper.describeResult(result, successMessage);
                        updateStatus(message);
                        showAlert("Lobby unavailable", message);
                        return;
                    }

                    updateStatus(MesosUserMessageMapper.describeResult(result, successMessage));
                    showLobbyScreen(result.lobbySnapshot());
                },
                "Lobby request failed"
        );
    }

    private void showLobbyScreen(LobbySnapshot lobbySnapshot) {
        appShell.setCenter(buildLobbyView(lobbySnapshot));
    }

    private Node buildLobbyView(LobbySnapshot lobbySnapshot) {
        TableView<LobbyGameSummary> gamesTable = new TableView<>();
        gamesTable.getStyleClass().add("game-table");
        gamesTable.setPlaceholder(new Label("No open games are currently available."));
        gamesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        gamesTable.setItems(FXCollections.observableArrayList(lobbySnapshot.openGames()));
        gamesTable.setMinHeight(250);
        gamesTable.setPrefHeight(340);
        VBox.setVgrow(gamesTable, Priority.ALWAYS);

        TableColumn<LobbyGameSummary, String> nameColumn = new TableColumn<>("Game");
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().gameName()));

        TableColumn<LobbyGameSummary, String> playersColumn = new TableColumn<>("Players");
        playersColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().joinedPlayers() + "/" + cell.getValue().playerCount()
        ));

        TableColumn<LobbyGameSummary, String> seatsColumn = new TableColumn<>("Open Seats");
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                Integer.toString(cell.getValue().availableSeats())
        ));

        gamesTable.getColumns().setAll(nameColumn, playersColumn, seatsColumn);

        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Your nickname");

        TextField gameNameField = new TextField();
        gameNameField.setPromptText("Game name");

        Spinner<Integer> playerCountSpinner = new Spinner<>();
        playerCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                Globals.MIN_NUM_PLAYERS,
                Globals.MAX_NUM_PLAYERS,
                Globals.MIN_NUM_PLAYERS
        ));
        playerCountSpinner.setEditable(false);

        gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                gameNameField.setText(newValue.gameName());
            }
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> loadLobby("Lobby refreshed."));

        Button disconnectButton = new Button("Disconnect");
        disconnectButton.getStyleClass().add("secondary-button");
        disconnectButton.setOnAction(event -> disconnectToConnectionScreen());

        Button joinButton = new Button("Join Game");
        joinButton.getStyleClass().add("primary-button");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        joinButton.setOnAction(event -> {
            String gameName = resolveSelectedGameName(gameNameField, gamesTable);
            String nickname = nicknameField.getText();
            if (!validateLobbyActionFields(gameName, nickname)) {
                return;
            }
            joinGame(gameName.trim(), nickname.trim());
        });

        Button createButton = new Button("Create and Join");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> {
            String gameName = gameNameField.getText();
            String nickname = nicknameField.getText();
            if (!validateLobbyActionFields(gameName, nickname)) {
                return;
            }
            createAndJoinGame(gameName.trim(), playerCountSpinner.getValue(), nickname.trim());
        });

        VBox tableCard = new VBox(
                14,
                sectionHeader("Open Games", "Select a game from the list or type its name manually."),
                gamesTable,
                buildMetaWrap(
                        metaBadge("Connected via " + connectedType),
                        metaBadge("Host " + connectedHost)
                ),
                buildActionRow(refreshButton, disconnectButton)
        );
        tableCard.getStyleClass().addAll("panel-shell", "table-card");
        tableCard.setMinWidth(520);
        tableCard.setPrefWidth(720);
        tableCard.setMaxWidth(Double.MAX_VALUE);

        VBox fieldStack = new VBox(14);
        fieldStack.getStyleClass().add("field-stack");
        fieldStack.getChildren().addAll(
                buildField("Nickname", nicknameField, 250),
                buildField("Game Name", gameNameField, 260),
                buildField("Player Count", playerCountSpinner, 180)
        );

        VBox sidebar = new VBox(
                18,
                (VBox) buildFormActionCard(fieldStack, createButton, joinButton)
        );
        sidebar.getStyleClass().add("screen-sidebar");
        sidebar.setMinWidth(320);
        sidebar.setPrefWidth(360);

        FlowPane content = new FlowPane(24, 24);
        content.getStyleClass().add("screen-grid");
        content.setAlignment(Pos.TOP_CENTER);
        content.prefWrapLengthProperty().bind(rootStack.widthProperty().subtract(150));
        content.getChildren().addAll(tableCard, sidebar);

        VBox shell = new VBox(
                20,
                titleBlock("Lobby", "Browse open games or create a new one."),
                content
        );
        shell.getStyleClass().add("screen-shell");
        shell.setMaxWidth(1240);
        shell.setPadding(new Insets(34, 14, 20, 14));

        return centeredContent(shell);
    }

    private Node buildFormActionCard(Node fields, Button createButton, Button joinButton) {
        VBox actions = new VBox(
                16,
                sectionHeader("Join or Create", "Use the same form for both actions. The server remains authoritative for all rules."),
                fields,
                buildButtonStack(createButton, joinButton)
        );
        actions.getStyleClass().addAll("panel-shell", "form-card");
        actions.setMinWidth(300);
        return actions;
    }

    private boolean validateLobbyActionFields(String gameName, String nickname) {
        if (gameName == null || gameName.isBlank()) {
            showAlert("Missing game name", "Enter a game name or select one from the table.");
            return false;
        }
        if (nickname == null || nickname.isBlank()) {
            showAlert("Missing nickname", "Enter the nickname you want to use.");
            return false;
        }
        return true;
    }

    private String resolveSelectedGameName(TextField gameNameField, TableView<LobbyGameSummary> gamesTable) {
        if (gameNameField.getText() != null && !gameNameField.getText().isBlank()) {
            return gameNameField.getText();
        }
        LobbyGameSummary selected = gamesTable.getSelectionModel().getSelectedItem();
        return selected == null ? "" : selected.gameName();
    }

    private void createAndJoinGame(String gameName, int playerCount, String nickname) {
        withBusy(
                "Creating game...",
                guiService.createAndJoinGame(gameName, playerCount, nickname),
                result -> handleJoinFlowResult(result, "Game created."),
                "Game creation failed"
        );
    }

    private void joinGame(String gameName, String nickname) {
        withBusy(
                "Joining game...",
                guiService.joinGame(gameName, nickname),
                result -> handleJoinFlowResult(result, "Joined game."),
                "Could not join the selected game"
        );
    }

    private void handleJoinFlowResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        updateStatus(message);

        if (result.snapshot() != null && guiService.hasJoinedGame()) {
            renderSnapshot(result.snapshot(), message);
            if (!result.success()) {
                showAlert("Live updates unavailable", message);
            }
            return;
        }

        if (!result.success()) {
            showAlert("Operation failed", message);
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

    private void showWaitingRoom(GameSnapshot snapshot, String message) {
        appShell.setCenter(buildWaitingRoomView(snapshot, message));
    }

    private Node buildWaitingRoomView(GameSnapshot snapshot, String message) {
        VBox playersList = new VBox(12);
        playersList.getStyleClass().add("players-list");
        for (PlayerSummary player : snapshot.players()) {
            playersList.getChildren().add(waitingPlayerRow(player));
        }
        if (snapshot.players().isEmpty()) {
            playersList.getChildren().add(new Label("No players have joined yet."));
        }

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> refreshCurrentSnapshot());

        Button disconnectButton = new Button("Disconnect");
        disconnectButton.getStyleClass().add("secondary-button");
        disconnectButton.setOnAction(event -> disconnectToConnectionScreen());

        VBox summaryCard = new VBox(
                14,
                sectionHeader("Match Summary", "The game will start automatically when the lobby becomes full."),
                buildMetaWrap(
                        metaBadge("Game " + snapshot.gameName()),
                        metaBadge("Players " + snapshot.joinedPlayers() + "/" + snapshot.playerCount()),
                        metaBadge("You " + guiService.currentNickname())
                ),
                helperText(message),
                buildActionRow(refreshButton, disconnectButton)
        );
        summaryCard.getStyleClass().addAll("panel-shell", "table-card");
        summaryCard.setPrefWidth(360);
        summaryCard.setMaxWidth(Double.MAX_VALUE);

        VBox playersCard = new VBox(
                14,
                sectionHeader("Joined Players", "Live updates will move you to the board as soon as the match starts."),
                playersList
        );
        playersCard.getStyleClass().addAll("panel-shell", "table-card");
        playersCard.setMinWidth(420);
        playersCard.setPrefWidth(560);
        playersCard.setMaxWidth(Double.MAX_VALUE);

        FlowPane content = new FlowPane(24, 24, summaryCard, playersCard);
        content.getStyleClass().add("screen-grid");
        content.setAlignment(Pos.TOP_CENTER);
        content.prefWrapLengthProperty().bind(rootStack.widthProperty().subtract(150));

        VBox shell = new VBox(
                20,
                titleBlock("Waiting Room", "Stay connected while the lobby fills. The board opens automatically when the match starts."),
                content
        );
        shell.getStyleClass().add("screen-shell");
        shell.setMaxWidth(1100);
        shell.setPadding(new Insets(34, 14, 28, 14));
        return centeredContent(shell);
    }

    private Node waitingPlayerRow(PlayerSummary player) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("player-card", "waiting-player-row");

        row.getChildren().add(buildColorChip(player.color()));

        Label name = new Label(player.nickname());
        name.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(player.nickname().equals(guiService.currentNickname()) ? "You" : "Joined");
        status.getStyleClass().add("meta-badge");
        row.getChildren().addAll(name, spacer, status);
        return row;
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
        appShell.setCenter(gameBoardView);
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
        updateStatus(message);

        // For mutating actions the subscribed server update is the authoritative board refresh,
        // just like the CLI flow. Re-render the response snapshot only on failure so the user
        // still sees the current server state if the action is rejected.
        if (!result.success() && result.snapshot() != null) {
            renderSnapshot(result.snapshot(), message);
        }
    }

    private void handleSnapshotResult(GameOperationResult result, String successMessage) {
        String message = MesosUserMessageMapper.describeResult(result, successMessage);
        updateStatus(message);
        if (result.snapshot() != null) {
            renderSnapshot(result.snapshot(), message);
        }
    }

    private void handleRemoteUpdate(GameViewUpdate update) {
        Platform.runLater(() -> {
            String message = MesosUserMessageMapper.describeUpdate(update);
            updateStatus(message);
            renderSnapshot(update.snapshot(), message);
        });
    }

    private void disconnectToConnectionScreen() {
        withBusy(
                "Disconnecting...",
                guiService.disconnect(),
                ignored -> {
                    connectedType = null;
                    connectedHost = null;
                    gameBoardView = null;
                    showConnectionScreen();
                    updateStatus("Disconnected.");
                },
                "Disconnect failed"
        );
    }

    private void updateStatus(String message) {
        statusBarLabel.setText(message);
    }

    private <T> void withBusy(
            String busyMessage,
            CompletableFuture<T> future,
            Consumer<T> onSuccess,
            String errorTitle
    ) {
        setBusy(true, busyMessage);
        future.whenComplete((value, throwable) -> Platform.runLater(() -> {
            setBusy(false, null);
            if (throwable != null) {
                String message = MesosUserMessageMapper.describeException(throwable);
                updateStatus(message);
                showAlert(errorTitle, message);
                return;
            }
            onSuccess.accept(value);
        }));
    }

    private void setBusy(boolean busy, String text) {
        busyOverlay.setVisible(busy);
        busyOverlay.setManaged(busy);
        if (busy && text != null) {
            ((Label) busyOverlay.getChildren().get(1)).setText(text);
        }
    }

    private Node buildField(String label, Node input, double preferredWidth) {
        Label title = new Label(label);
        title.getStyleClass().add("subsection-title");

        if (input instanceof Region region) {
            region.setPrefWidth(preferredWidth);
            region.setMaxWidth(Double.MAX_VALUE);
        }

        VBox box = new VBox(8, title, input);
        box.getStyleClass().add("field-box");
        box.setPrefWidth(preferredWidth);
        return box;
    }

    private VBox titleBlock(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("view-title");
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("muted-label");
        subtitleLabel.setWrapText(true);

        return new VBox(6, titleLabel, subtitleLabel);
    }

    private VBox sectionHeader(String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        Label details = new Label(text);
        details.getStyleClass().add("details-label");
        details.setWrapText(true);

        return new VBox(6, titleLabel, details);
    }

    private Label helperText(String text) {
        Label details = new Label(text);
        details.getStyleClass().add("details-label");
        details.setWrapText(true);
        return details;
    }

    private FlowPane buildActionRow(Button... buttons) {
        FlowPane actions = new FlowPane(12, 12);
        actions.getStyleClass().add("actions-row");
        for (Button button : buttons) {
            button.setPrefWidth(170);
            actions.getChildren().add(button);
        }
        return actions;
    }

    private VBox buildButtonStack(Button... buttons) {
        VBox actions = new VBox(10);
        actions.getStyleClass().add("actions-stack");
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
            actions.getChildren().add(button);
        }
        return actions;
    }

    private FlowPane buildMetaWrap(Node... chips) {
        FlowPane wrap = new FlowPane(10, 10);
        wrap.getStyleClass().add("meta-wrap");
        wrap.getChildren().addAll(chips);
        return wrap;
    }

    private Label metaBadge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("meta-badge");
        return badge;
    }

    private Node centeredScroll(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private Node centeredContent(Node content) {
        StackPane container = new StackPane(content);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(8, 0, 8, 0));
        return container;
    }

    private Node centeredFixedContent(Node content) {
        StackPane container = new StackPane(content);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(6, 0, 6, 0));
        return container;
    }

    private Node buildColorChip(it.polimi.ingsw.mesos.Model.Enums.Color color) {
        StackPane chip = new StackPane();
        chip.setMinSize(16, 16);
        chip.setPrefSize(16, 16);
        chip.setMaxSize(16, 16);

        Color fill = switch (color == null ? it.polimi.ingsw.mesos.Model.Enums.Color.WHITE : color) {
            case BLUE -> Color.web("#2f74ff");
            case YELLOW -> Color.web("#f1c40f");
            case ORANGE -> Color.web("#f39c12");
            case WHITE -> Color.web("#f6f7fb");
            case BLACK -> Color.web("#8e44ad");
        };

        chip.setStyle("-fx-background-radius: 999px; -fx-background-color: " + toRgb(fill)
                + "; -fx-border-radius: 999px; -fx-border-color: rgba(58,34,16,0.20); -fx-border-width: 1;");
        return chip;
    }

    private String toRgb(Color color) {
        return "rgba("
                + (int) Math.round(color.getRed() * 255)
                + ","
                + (int) Math.round(color.getGreen() * 255)
                + ","
                + (int) Math.round(color.getBlue() * 255)
                + ","
                + color.getOpacity()
                + ")";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("MESOS");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }
}
