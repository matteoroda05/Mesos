package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.LobbyGameSummary;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Builds the JavaFX screens used for connection, lobby, waiting-room, and
 * nickname flows with responsive layouts.
 */
public final class MesosGuiScreenFactory {
    private final ReadOnlyDoubleProperty availableWidth;

    /**
     * Creates a screen factory whose responsive layouts use the available width.
     *
     * @param availableWidth property that reports the current content width
     * @throws NullPointerException if the property is null
     */
    public MesosGuiScreenFactory(ReadOnlyDoubleProperty availableWidth) {
        this.availableWidth = Objects.requireNonNull(availableWidth, "availableWidth cannot be null");
    }

    /**
     * Builds the initial transport and host selection screen.
     *
     * @param onConnect callback receiving the selected transport and host
     * @return configured connection screen
     */
    public Node buildConnectionScreen(BiConsumer<ConnectionType, String> onConnect) {
        ComboBox<ConnectionType> connectionTypeBox = new ComboBox<>();
        connectionTypeBox.getItems().setAll(ConnectionType.values());
        connectionTypeBox.setValue(ConnectionType.SOCKET);
        connectionTypeBox.setMaxWidth(Double.MAX_VALUE);

        TextField hostField = new TextField(NetworkDefaults.DEFAULT_RMI_HOST);
        hostField.setPromptText("Server host");
        hostField.setOnAction(event -> onConnect.accept(connectionTypeBox.getValue(), hostField.getText()));

        Button connectButton = new Button("Connect");
        connectButton.getStyleClass().add("primary-button");
        connectButton.setMaxWidth(Double.MAX_VALUE);
        connectButton.setOnAction(event -> onConnect.accept(connectionTypeBox.getValue(), hostField.getText()));

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
        container.getStyleClass().addAll("connection-screen-root", "connection-screen-content");
        container.setPadding(new Insets(6, 0, 6, 0));
        container.setTranslateY(96);

        Platform.runLater(hostField::requestFocus);
        return container;
    }

    /**
     * Builds the nickname-registration screen after a transport connection succeeds.
     *
     * @param connectedType active transport type
     * @param connectedHost connected server host
     * @param suggestedNickname value prefilled in the nickname field, possibly null
     * @param onSubmitNickname callback receiving the entered nickname
     * @param onDisconnect callback to disconnect from the server
     * @return screen root together with its nickname text field
     */
    public NicknameScreen buildNicknameScreen(
            ConnectionType connectedType,
            String connectedHost,
            String suggestedNickname,
            Consumer<String> onSubmitNickname,
            Runnable onDisconnect
    ) {
        TextField nicknameField = new TextField(suggestedNickname == null ? "" : suggestedNickname.trim());
        nicknameField.setPromptText("Nickname");
        nicknameField.setOnAction(event -> onSubmitNickname.accept(nicknameField.getText()));

        Button confirmButton = new Button("Confirm");
        confirmButton.getStyleClass().add("primary-button");
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(event -> onSubmitNickname.accept(nicknameField.getText()));

        Button disconnectButton = new Button("Disconnect");
        disconnectButton.getStyleClass().add("secondary-button");
        disconnectButton.setMaxWidth(Double.MAX_VALUE);
        disconnectButton.setOnAction(event -> onDisconnect.run());

        VBox card = new VBox(
                18,
                sectionHeader("Nickname", "Choose the nickname to register before entering the lobby."),
                buildField("Nickname", nicknameField, 240),
                buildMetaWrap(
                        metaBadge("Connected via " + safeText(connectedType)),
                        metaBadge("Host " + safeText(connectedHost))
                ),
                buildButtonStack(confirmButton, disconnectButton)
        );
        card.getStyleClass().addAll("panel-shell", "form-card");
        card.setPrefWidth(320);
        card.setMaxWidth(320);

        VBox shell = new VBox(
                20,
                titleBlock("Nickname", "Use the same post-connect nickname step that already exists in the CLI flow."),
                card
        );
        shell.getStyleClass().add("screen-shell");
        shell.setAlignment(Pos.TOP_CENTER);
        shell.setMaxWidth(520);
        shell.setPadding(new Insets(34, 14, 20, 14));

        Platform.runLater(nicknameField::requestFocus);
        return new NicknameScreen(centeredFixedContent(shell), nicknameField);
    }

    /**
     * Builds the lobby screen for browsing, joining, and creating games.
     *
     * @param lobbySnapshot games available to display
     * @param currentNickname selected nickname
     * @param connectedType active transport type
     * @param connectedHost connected server host
     * @param onRefresh callback to refresh the lobby
     * @param onDisconnect callback to disconnect from the server
     * @param onJoin callback receiving the chosen game name
     * @param onCreate callback receiving a new game name and player count
     * @return configured lobby screen
     */
    public Node buildLobbyScreen(
            LobbySnapshot lobbySnapshot,
            String currentNickname,
            ConnectionType connectedType,
            String connectedHost,
            Runnable onRefresh,
            Runnable onDisconnect,
            Consumer<String> onJoin,
            BiConsumer<String, Integer> onCreate
    ) {
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
        refreshButton.setOnAction(event -> onRefresh.run());

        Button disconnectButton = new Button("Disconnect");
        disconnectButton.getStyleClass().add("secondary-button");
        disconnectButton.setOnAction(event -> onDisconnect.run());

        Button joinButton = new Button("Join Game");
        joinButton.getStyleClass().add("primary-button");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        joinButton.setOnAction(event -> onJoin.accept(resolveSelectedGameName(gameNameField, gamesTable)));

        Button createButton = new Button("Create and Join");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> onCreate.accept(gameNameField.getText(), playerCountSpinner.getValue()));

        VBox tableCard = new VBox(
                14,
                sectionHeader("Open Games", "Select a game from the list or type its name manually."),
                gamesTable,
                buildMetaWrap(
                        metaBadge("Nickname " + safeText(currentNickname)),
                        metaBadge("Connected via " + safeText(connectedType)),
                        metaBadge("Host " + safeText(connectedHost))
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
                buildField("Game Name", gameNameField, 260),
                buildField("Player Count", playerCountSpinner, 180)
        );

        VBox sidebar = new VBox(18, buildFormActionCard(fieldStack, createButton, joinButton));
        sidebar.getStyleClass().add("screen-sidebar");
        sidebar.setMinWidth(320);
        sidebar.setPrefWidth(360);

        FlowPane content = new FlowPane(24, 24);
        content.getStyleClass().add("screen-grid");
        content.setAlignment(Pos.TOP_CENTER);
        content.prefWrapLengthProperty().bind(availableWidth.subtract(150));
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

    /**
     * Builds the pre-game waiting room with joined players and refresh controls.
     *
     * @param snapshot current pre-game snapshot
     * @param currentNickname local player's nickname
     * @param message current status message
     * @param onRefresh callback to refresh the snapshot
     * @param onDisconnect callback to disconnect from the server
     * @return configured waiting-room screen
     */
    public Node buildWaitingRoomScreen(
            GameSnapshot snapshot,
            String currentNickname,
            String message,
            Runnable onRefresh,
            Runnable onDisconnect
    ) {
        VBox playersList = new VBox(12);
        playersList.getStyleClass().add("players-list");
        for (PlayerSummary player : snapshot.players()) {
            playersList.getChildren().add(buildWaitingPlayerRow(player, currentNickname));
        }
        if (snapshot.players().isEmpty()) {
            playersList.getChildren().add(new Label("No players have joined yet."));
        }

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> onRefresh.run());

        Button disconnectButton = new Button("Disconnect");
        disconnectButton.getStyleClass().add("secondary-button");
        disconnectButton.setOnAction(event -> onDisconnect.run());

        VBox summaryCard = new VBox(
                14,
                sectionHeader("Match Summary", "The game will start automatically when the lobby becomes full."),
                buildMetaWrap(
                        metaBadge("Game " + safeText(snapshot.gameName())),
                        metaBadge("Players " + snapshot.joinedPlayers() + "/" + snapshot.playerCount()),
                        metaBadge("You " + safeText(currentNickname))
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
        content.prefWrapLengthProperty().bind(availableWidth.subtract(150));

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

    /**
     * Groups the nickname screen root with its input field so callers can
     * display and focus the same control.
     */
    public record NicknameScreen(Node root, TextField nicknameField) {
    }

    /**
     * Loads and crops the Mesos logo, using a text fallback when unavailable.
     *
     * @return responsive logo node
     */
    private Node buildConnectionLogo() {
        var logoUrl = MesosGuiScreenFactory.class.getResource("/cards_png/Scritta_Mesos.png");
        if (logoUrl == null) {
            Label placeholder = new Label("MESOS");
            placeholder.getStyleClass().add("view-title");
            return placeholder;
        }

        ImageView imageView = new ImageView(new Image(logoUrl.toExternalForm(), true));
        imageView.setViewport(new Rectangle2D(1300, 1152, 2220, 656));
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(
                Bindings.max(Bindings.min(availableWidth.subtract(260), 560.0), 320.0)
        );
        imageView.setSmooth(true);

        StackPane wrapper = new StackPane(imageView);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(580);
        wrapper.setTranslateY(12);
        return wrapper;
    }

    /**
     * Builds the lobby sidebar card containing fields and create/join buttons.
     *
     * @param fields form fields shown above the actions
     * @param createButton button that creates a new game
     * @param joinButton button that joins a selected game
     * @return configured action card
     */
    private VBox buildFormActionCard(Node fields, Button createButton, Button joinButton) {
        VBox actions = new VBox(
                16,
                sectionHeader("Join or Create", "The nickname selected after connection is reused automatically for both actions."),
                fields,
                buildButtonStack(createButton, joinButton)
        );
        actions.getStyleClass().addAll("panel-shell", "form-card");
        actions.setMinWidth(300);
        return actions;
    }

    /**
     * Resolves a game name from typed text or the currently selected table row.
     *
     * @param gameNameField field that may contain a manually typed name
     * @param gamesTable table containing selectable games
     * @return typed or selected name, or an empty string when neither exists
     */
    private String resolveSelectedGameName(TextField gameNameField, TableView<LobbyGameSummary> gamesTable) {
        if (gameNameField.getText() != null && !gameNameField.getText().isBlank()) {
            return gameNameField.getText();
        }
        LobbyGameSummary selected = gamesTable.getSelectionModel().getSelectedItem();
        return selected == null ? "" : selected.gameName();
    }

    /**
     * Builds one joined-player row and marks the local player as {@code You}.
     *
     * @param player player shown in the waiting room
     * @param currentNickname local player's nickname, possibly null
     * @return configured player row
     */
    private Node buildWaitingPlayerRow(PlayerSummary player, String currentNickname) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("player-card", "waiting-player-row");

        row.getChildren().add(buildColorChip(player.color()));

        Label name = new Label(player.nickname());
        name.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean localPlayer = currentNickname != null && currentNickname.equals(player.nickname());
        Label status = new Label(localPlayer ? "You" : "Joined");
        status.getStyleClass().add("meta-badge");
        row.getChildren().addAll(name, spacer, status);
        return row;
    }

    /**
     * Wraps an input control with a section label and preferred width.
     *
     * @param label field label
     * @param input input control to configure
     * @param preferredWidth preferred width for the field
     * @return configured field container
     */
    private Node buildField(String label, Region input, double preferredWidth) {
        Label title = new Label(label);
        title.getStyleClass().add("subsection-title");

        input.setPrefWidth(preferredWidth);
        input.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(8, title, input);
        box.getStyleClass().add("field-box");
        box.setPrefWidth(preferredWidth);
        return box;
    }

    /**
     * Creates the screen title and subtitle block.
     *
     * @param title main heading
     * @param subtitle supporting text
     * @return configured title block
     */
    private VBox titleBlock(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("view-title");
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("muted-label");
        subtitleLabel.setWrapText(true);

        return new VBox(6, titleLabel, subtitleLabel);
    }

    /**
     * Creates a section heading with supporting descriptive text.
     *
     * @param title section heading
     * @param text section description
     * @return configured section header
     */
    private VBox sectionHeader(String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        Label details = new Label(text);
        details.getStyleClass().add("details-label");
        details.setWrapText(true);

        return new VBox(6, titleLabel, details);
    }

    /**
     * Creates a wrapping details label, treating null as empty text.
     *
     * @param text helper text, possibly null
     * @return configured details label
     */
    private Label helperText(String text) {
        Label details = new Label(text == null ? "" : text);
        details.getStyleClass().add("details-label");
        details.setWrapText(true);
        return details;
    }

    /**
     * Places action buttons in a wrapping row with uniform preferred widths.
     *
     * @param buttons buttons to place in the row
     * @return configured action row
     */
    private FlowPane buildActionRow(Button... buttons) {
        FlowPane actions = new FlowPane(12, 12);
        actions.getStyleClass().add("actions-row");
        for (Button button : buttons) {
            button.setPrefWidth(170);
            actions.getChildren().add(button);
        }
        return actions;
    }

    /**
     * Stacks action buttons vertically and expands them to the available width.
     *
     * @param buttons buttons to stack
     * @return configured action stack
     */
    private VBox buildButtonStack(Button... buttons) {
        VBox actions = new VBox(10);
        actions.getStyleClass().add("actions-stack");
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
            actions.getChildren().add(button);
        }
        return actions;
    }

    /**
     * Places metadata chips in a wrapping row.
     *
     * @param chips metadata nodes to place
     * @return configured metadata wrapper
     */
    private FlowPane buildMetaWrap(Node... chips) {
        FlowPane wrap = new FlowPane(10, 10);
        wrap.getStyleClass().add("meta-wrap");
        wrap.getChildren().addAll(chips);
        return wrap;
    }

    /**
     * Creates a compact metadata badge label.
     *
     * @param text badge text
     * @return styled badge label
     */
    private Label metaBadge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("meta-badge");
        return badge;
    }

    /**
     * Centers content horizontally at the top of a padded responsive container.
     *
     * @param content node to position
     * @return configured container
     */
    private Node centeredContent(Node content) {
        StackPane container = new StackPane(content);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(8, 0, 8, 0));
        return container;
    }

    /**
     * Centers fixed-width content horizontally at the top of a compact container.
     *
     * @param content node to position
     * @return configured container
     */
    private Node centeredFixedContent(Node content) {
        StackPane container = new StackPane(content);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(6, 0, 6, 0));
        return container;
    }

    /**
     * Creates a circular CSS color chip for a player color.
     *
     * @param color game color, with null treated as white
     * @return configured color chip
     */
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

    /**
     * Converts a JavaFX color to an RGBA CSS value.
     *
     * @param color color to convert
     * @return CSS {@code rgba(...)} value
     */
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

    /**
     * Converts a nullable value to text suitable for a metadata badge.
     *
     * @param value value to render, possibly null
     * @return value text, or {@code -} when null
     */
    private String safeText(Object value) {
        return value == null ? "-" : value.toString();
    }
}
