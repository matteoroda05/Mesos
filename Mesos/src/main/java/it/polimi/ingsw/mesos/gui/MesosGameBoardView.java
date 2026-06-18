package it.polimi.ingsw.mesos.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.EventLogEntry;
import it.polimi.ingsw.mesos.EventMessageFormatter;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.CardSummary;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.OfferTileSummary;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import it.polimi.ingsw.mesos.Network.TurnOrderSpaceSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

public final class MesosGameBoardView extends BorderPane {
    private static final double DESIGN_WIDTH = 920;
    private static final double DESIGN_HEIGHT = 370;
    private static final double TABLETOP_VERTICAL_OFFSET_RATIO = 0.07;
    private static final double LEFT_HUD_WIDTH = 340;
    private static final double MARKET_CARD_WIDTH = 85;
    private static final double MARKET_CARD_HEIGHT = 130;
    private static final double MARKET_CARD_SLOT_HEIGHT = MARKET_CARD_HEIGHT + 8;
    private static final double MARKET_CARD_ROW_GAP = 12;
    private static final double TOP_MARKET_ROW_MAX_WIDTH = DESIGN_WIDTH - 48;
    private static final double BOTTOM_MARKET_ROW_MAX_WIDTH = 410;
    private static final double OFFER_TILE_WIDTH = 85;
    private static final double OFFER_TILE_HEIGHT = 130;
    private static final double OWNED_TRIBE_CARD_WIDTH = 54;
    private static final double OWNED_TRIBE_CARD_HEIGHT = 82;
    private static final double OWNED_CARD_DECK_SLOT_WIDTH = OWNED_TRIBE_CARD_WIDTH + 16;
    private static final double OWNED_CARD_DECK_SLOT_HEIGHT = OWNED_TRIBE_CARD_HEIGHT + 18;
    private static final double OWNED_CARD_DECK_STACK_X = 4.0;
    private static final double OWNED_CARD_DECK_STACK_Y = 2.0;
    private static final double OWNED_CARD_PREVIEW_MAX_SCALE = 2.25;
    private static final double OWNED_CARD_PREVIEW_MAX_WIDTH = 840;
    private static final double OWNED_CARD_PREVIEW_MAX_HEIGHT = 320;
    private static final double OWNED_CARD_PREVIEW_GAP = 10;
    private static final double OWNED_CARD_PREVIEW_ROW_GAP = 12;
    private static final int OWNED_CARD_PREVIEW_COLUMNS = 5;
    private static final double PLAYER_TRIBE_CARD_WIDTH = 54;
    private static final double PLAYER_TRIBE_CARD_HEIGHT = 82;
    private static final double INFO_CARD_WIDTH = 220;
    private static final double INFO_CARD_HEIGHT = 338;
    private static final int OWNED_CARD_DECK_MAX_VISIBLE_CARDS = 3;
    private static final List<OwnedCardDeckDefinition> OWNED_CARD_DECK_DEFINITIONS = List.of(
            new OwnedCardDeckDefinition("hunter", "Hunters", false),
            new OwnedCardDeckDefinition("builder", "Builders", false),
            new OwnedCardDeckDefinition("gatherer", "Gatherers", false),
            new OwnedCardDeckDefinition("artist", "Artists", false),
            new OwnedCardDeckDefinition("inventor", "Inventors", false),
            new OwnedCardDeckDefinition("shaman", "Shamans", false),
            new OwnedCardDeckDefinition("buildings", "Buildings", true)
    );

    private record OwnedCardDeckDefinition(String key, String label, boolean buildingDeck) {
    }

    private final String localNickname;
    private final MesosAssetResolver assetResolver;
    private final Runnable onRefresh;
    private final Runnable onDisconnect;
    private final Runnable onPass;
    private final Runnable onBackToLobby;
    private final IntConsumer onChooseOffer;
    private final BiConsumer<Integer, Boolean> onChooseCard;

    private final Label titleLabel;
    private final Label subtitleLabel;
    private final Label turnLabel;
    private final Label actionHintLabel;
    private final FlowPane actionRow;
    private final Button passButton;
    private final HBox topRowPane;
    private final HBox bottomRowPane;
    private final HBox offerTilesPane;
    private final VBox rankingBox;
    private final VBox ownedCardsDebugBox;
    private final HBox ownedTribeDecksPane;
    private final StackPane ownedDeckPreviewOverlay;
    private final HBox playerButtonsPane;
    private final StackPane playerTribeOverlay;
    private final VBox eventLogPane;
    private final StackPane eventLogOverlay;
    private final Button infoButton;
    private final StackPane infoPopupOverlay;
    private final HBox topRightBar;
    private final StackPane bottomHudOverlay;
    private final StackPane finalLeaderboardOverlay;
    private final VBox finalLeaderboardBox;
    private final VBox minimalTurnBarPane;
    private final StackPane infoPanelColorIndicator;
    private final StackPane viewport;
    private final VBox tabletopCanvas;
    private final Scale boardScale;

    private GameSnapshot snapshot;
    private List<EventLogEntry> eventLog = List.of();
    private boolean actionPending;

    public MesosGameBoardView(
            String localNickname,
            MesosAssetResolver assetResolver,
            Runnable onRefresh,
            Runnable onDisconnect,
            IntConsumer onChooseOffer,
            BiConsumer<Integer, Boolean> onChooseCard,
            Runnable onPass,
            Runnable onBackToLobby
    ) {
        this.localNickname = localNickname;
        this.assetResolver = assetResolver;
        this.onRefresh = onRefresh;
        this.onDisconnect = onDisconnect;
        this.onChooseOffer = onChooseOffer;
        this.onChooseCard = onChooseCard;
        this.onPass = onPass;
        this.onBackToLobby = onBackToLobby;

        this.titleLabel = new Label("MESOS");
        this.titleLabel.getStyleClass().add("view-title");

        this.subtitleLabel = new Label("Waiting for data...");
        this.subtitleLabel.getStyleClass().add("muted-label");

        this.turnLabel = new Label("Board updates will appear here.");
        this.turnLabel.getStyleClass().add("details-label");
        this.turnLabel.setWrapText(true);

        this.actionHintLabel = new Label("The central board will unlock actions when it is your turn.");
        this.actionHintLabel.getStyleClass().add("action-copy");
        this.actionHintLabel.setWrapText(true);

        this.passButton = buildInfoActionButton("Pass", this.onPass);
        this.passButton.setOnAction(event -> {
            if (lockActionUntilNextSnapshot()) {
                this.onPass.run();
            }
        });
        this.passButton.setVisible(false);
        this.passButton.setManaged(false);
        this.actionRow = new FlowPane(
                8,
                8,
                buildInfoActionButton("Refresh", onRefresh),
                passButton,
                buildInfoActionButton("Disconnect", onDisconnect)
        );
        this.actionRow.getStyleClass().add("board-action-row");
        this.actionRow.setAlignment(Pos.CENTER_LEFT);

        this.topRowPane = new HBox(MARKET_CARD_ROW_GAP);
        this.bottomRowPane = new HBox(MARKET_CARD_ROW_GAP);
        this.topRowPane.setAlignment(Pos.CENTER);
        this.bottomRowPane.setAlignment(Pos.CENTER);
        this.topRowPane.getStyleClass().add("table-row-cards");
        this.bottomRowPane.getStyleClass().add("table-row-cards");

        this.offerTilesPane = new HBox(0);
        this.offerTilesPane.setAlignment(Pos.CENTER_LEFT);

        this.rankingBox = new VBox(8);
        this.rankingBox.getStyleClass().add("ranking-box");
        this.rankingBox.setVisible(false);
        this.rankingBox.setManaged(false);

        this.ownedCardsDebugBox = new VBox(6);
        this.ownedCardsDebugBox.getStyleClass().add("owned-cards-debug-box");
        this.ownedCardsDebugBox.setVisible(false);
        this.ownedCardsDebugBox.setManaged(false);

        this.ownedTribeDecksPane = new HBox(0);
        this.ownedTribeDecksPane.getStyleClass().add("owned-tribe-decks-strip");
        this.ownedTribeDecksPane.setAlignment(Pos.BOTTOM_RIGHT);
        this.ownedTribeDecksPane.setVisible(false);
        this.ownedTribeDecksPane.setManaged(false);

        this.ownedDeckPreviewOverlay = new StackPane();
        this.ownedDeckPreviewOverlay.getStyleClass().add("owned-card-preview-overlay");
        this.ownedDeckPreviewOverlay.setAlignment(Pos.CENTER);
        this.ownedDeckPreviewOverlay.setVisible(false);
        this.ownedDeckPreviewOverlay.setMouseTransparent(true);
        this.ownedDeckPreviewOverlay.setPickOnBounds(false);
        this.ownedDeckPreviewOverlay.setMinSize(0, 0);
        this.ownedDeckPreviewOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        this.playerButtonsPane = new HBox(6);
        this.playerButtonsPane.getStyleClass().add("player-buttons-pane");
        this.playerButtonsPane.setAlignment(Pos.TOP_LEFT);
        this.playerButtonsPane.setVisible(false);
        this.playerButtonsPane.setManaged(false);
        this.playerButtonsPane.setMaxWidth(Region.USE_PREF_SIZE);

        this.playerTribeOverlay = new StackPane();
        this.playerTribeOverlay.getStyleClass().add("player-tribe-overlay");
        this.playerTribeOverlay.setAlignment(Pos.CENTER);
        this.playerTribeOverlay.setVisible(false);
        this.playerTribeOverlay.setPickOnBounds(false);
        this.playerTribeOverlay.setMinSize(0, 0);
        this.playerTribeOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        this.eventLogPane = new VBox(6);
        this.eventLogPane.getStyleClass().add("event-log-pane");
        this.eventLogPane.setAlignment(Pos.TOP_RIGHT);
        this.eventLogPane.setMaxWidth(Region.USE_PREF_SIZE);

        this.eventLogOverlay = new StackPane();
        this.eventLogOverlay.getStyleClass().add("event-log-overlay");
        this.eventLogOverlay.setAlignment(Pos.CENTER);
        this.eventLogOverlay.setVisible(false);
        this.eventLogOverlay.setPickOnBounds(false);
        this.eventLogOverlay.setMinSize(0, 0);
        this.eventLogOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // built identically to the Event Log button so the two match exactly
        // (same font, padding, and size); "info-popup-button" is only a marker class
        this.infoButton = new Button("Instruction Cards");
        this.infoButton.getStyleClass().addAll("event-log-button", "info-popup-button");
        this.infoButton.setMaxWidth(Region.USE_PREF_SIZE);
        this.infoButton.setOnAction(event -> showInfoPopup());

        this.infoPopupOverlay = new StackPane();
        this.infoPopupOverlay.getStyleClass().add("info-popup-overlay");
        this.infoPopupOverlay.setAlignment(Pos.CENTER);
        this.infoPopupOverlay.setVisible(false);
        this.infoPopupOverlay.setMinSize(0, 0);
        this.infoPopupOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.infoPopupOverlay.setOnMouseClicked(event -> hideInfoPopup());

        this.topRightBar = new HBox(6, eventLogPane, infoButton);
        this.topRightBar.getStyleClass().add("top-right-bar");
        this.topRightBar.setAlignment(Pos.CENTER_RIGHT);
        this.topRightBar.setFillHeight(false);
        this.topRightBar.setMaxWidth(Region.USE_PREF_SIZE);
        this.topRightBar.setMaxHeight(Region.USE_PREF_SIZE);
        this.topRightBar.setPickOnBounds(false);

        this.infoPanelColorIndicator = buildColorIndicator(null, "info-panel-color-indicator");
        this.infoPanelColorIndicator.setVisible(false);
        this.infoPanelColorIndicator.setManaged(false);
        this.minimalTurnBarPane = buildMinimalTurnOrderBar();
        this.bottomHudOverlay = buildBottomHudOverlay();
        this.finalLeaderboardBox = new VBox(10);
        this.finalLeaderboardBox.getStyleClass().add("final-leaderboard-box");
        this.finalLeaderboardBox.setAlignment(Pos.CENTER);
        this.finalLeaderboardBox.setMaxHeight(Region.USE_PREF_SIZE);
        this.finalLeaderboardOverlay = buildFinalLeaderboardOverlay();
        this.tabletopCanvas = buildTabletopCanvas();
        this.boardScale = new Scale(1, 1, 0, 0);
        this.viewport = buildViewport();

        rebuildEventLogPane();
        buildLayout();
    }

    public void updateSnapshot(GameSnapshot snapshot, String updateMessage) {
        hideOwnedDeckPreview();
        hidePlayerTribePopup();
        hideInfoPopup();
        actionPending = false;
        this.snapshot = snapshot;
        if (snapshot == null) {
            setFinalMode(false);
            rebuildPlayerButtonsPane();
            return;
        }

        if (isFinalSnapshot()) {
            rebuildFinalLeaderboard();
            setFinalMode(true);
            return;
        }

        setFinalMode(false);

        titleLabel.setText("Game Status:");
        subtitleLabel.setText("Round " + snapshot.currentRound() + "/10");
        turnLabel.setText("Era: " + formatEra(snapshot.currentEra()));
        actionHintLabel.setText("Player Turn: " + (snapshot.currentPlayerNickname() == null
                ? "-"
                : snapshot.currentPlayerNickname()));
        boolean showPass = canUsePass();
        passButton.setVisible(showPass);
        passButton.setManaged(showPass);

        rebuildMarketRows();
        rebuildOfferTiles();
        rebuildRanking();
        rebuildMinimalTurnBar();
        refreshInfoPanelColorIndicator();
        rebuildOwnedCardsDebugBox();
        rebuildOwnedTribeDecksPane();
        rebuildPlayerButtonsPane();
    }

    private boolean isFinalSnapshot() {
        return snapshot != null
                && (snapshot.finished()
                || snapshot.status() == GameStatus.END_GAME
                || snapshot.status() == GameStatus.END_SCREEN);
    }

    private void setFinalMode(boolean finalMode) {
        tabletopCanvas.setVisible(!finalMode);
        topRightBar.setVisible(!finalMode);
        topRightBar.setManaged(!finalMode);
        bottomHudOverlay.setVisible(!finalMode);
        bottomHudOverlay.setManaged(!finalMode);
        playerButtonsPane.setVisible(!finalMode && snapshot != null && !snapshot.players().isEmpty());
        playerButtonsPane.setManaged(!finalMode && snapshot != null && !snapshot.players().isEmpty());
        finalLeaderboardOverlay.setVisible(finalMode);
        finalLeaderboardOverlay.setManaged(finalMode);
        relocateEventLogPane(finalMode);
    }

    /**
     * Keeps the event-log button reachable on the end screen by hosting it in
     * the final leaderboard overlay (top-left, clear of the "Back to Lobby"
     * button) while in final mode, and back in the in-game top-right bar
     * otherwise.
     *
     * @param finalMode whether the final leaderboard is being shown
     */
    private void relocateEventLogPane(boolean finalMode) {
        if (finalMode) {
            topRightBar.getChildren().remove(eventLogPane);
            if (!finalLeaderboardOverlay.getChildren().contains(eventLogPane)) {
                finalLeaderboardOverlay.getChildren().add(eventLogPane);
                StackPane.setAlignment(eventLogPane, Pos.TOP_LEFT);
                StackPane.setMargin(eventLogPane, new Insets(12, 0, 0, 12));
            }
        } else {
            finalLeaderboardOverlay.getChildren().remove(eventLogPane);
            if (!topRightBar.getChildren().contains(eventLogPane)) {
                topRightBar.getChildren().add(0, eventLogPane);
            }
        }
    }

    private void rebuildMinimalTurnBar() {
        VBox refreshedBar = buildMinimalTurnOrderBar();
        minimalTurnBarPane.getChildren().setAll(refreshedBar.getChildren());
    }

    private void refreshInfoPanelColorIndicator() {
        Optional<it.polimi.ingsw.mesos.Model.Enums.Color> indicatorColor = resolveInfoPanelColor();
        boolean hasColor = indicatorColor.isPresent();
        infoPanelColorIndicator.setVisible(hasColor);
        infoPanelColorIndicator.setManaged(hasColor);
        indicatorColor.ifPresent(color -> updateColorIndicator(infoPanelColorIndicator, color));
    }

    private Optional<it.polimi.ingsw.mesos.Model.Enums.Color> resolveInfoPanelColor() {
        if (snapshot == null) {
            return Optional.empty();
        }
        Optional<it.polimi.ingsw.mesos.Model.Enums.Color> localPlayerColor = snapshot.players().stream()
                .filter(player -> player.nickname().equals(localNickname))
                .map(PlayerSummary::color)
                .filter(color -> color != null)
                .findFirst();
        return localPlayerColor.or(() -> Optional.ofNullable(snapshot.currentPlayerColor()));
    }

    private void buildLayout() {
        getStyleClass().add("board-root");
        // keep horizontal padding at zero so HUD overlays can reach the screen edges
        setPadding(new Insets(0, 0, 20, 0));
        setMinSize(0, 0);
        setCenter(viewport);
        // Keep the tabletop slightly lower while the background stays fixed.
        viewport.heightProperty().addListener((observable, oldValue, newValue) -> {
            tabletopCanvas.setTranslateY(newValue.doubleValue() * TABLETOP_VERTICAL_OFFSET_RATIO);
        });
    }

    private StackPane buildViewport() {
        Group wrapper = new Group(tabletopCanvas);
        tabletopCanvas.getTransforms().setAll(boardScale);

        // Load background image
        Optional<Image> backgroundImage = assetResolver.resolveBackgroundImage("sfondo1.png");
        StackPane pane = new StackPane();
        
        if (backgroundImage.isPresent()) {
            ImageView bgView = new ImageView(backgroundImage.get());
            bgView.setPreserveRatio(false);
            bgView.fitWidthProperty().bind(pane.widthProperty());
            bgView.fitHeightProperty().bind(pane.heightProperty());
            pane.getChildren().add(bgView);
        }
        
        pane.getChildren().add(wrapper);
        StackPane.setAlignment(wrapper, Pos.TOP_CENTER);
        StackPane.setAlignment(playerButtonsPane, Pos.TOP_LEFT);
        StackPane.setMargin(playerButtonsPane, new Insets(12, 0, 0, 12));
        pane.getChildren().add(playerButtonsPane);
        StackPane.setAlignment(topRightBar, Pos.TOP_RIGHT);
        StackPane.setMargin(topRightBar, new Insets(12, 12, 0, 0));
        pane.getChildren().add(topRightBar);
        StackPane.setAlignment(bottomHudOverlay, Pos.BOTTOM_CENTER);
        pane.getChildren().add(bottomHudOverlay);
        StackPane.setAlignment(finalLeaderboardOverlay, Pos.CENTER);
        pane.getChildren().add(finalLeaderboardOverlay);
        StackPane.setAlignment(ownedDeckPreviewOverlay, Pos.CENTER);
        pane.getChildren().add(ownedDeckPreviewOverlay);
        StackPane.setAlignment(playerTribeOverlay, Pos.CENTER);
        pane.getChildren().add(playerTribeOverlay);
        StackPane.setAlignment(eventLogOverlay, Pos.CENTER);
        pane.getChildren().add(eventLogOverlay);
        StackPane.setAlignment(infoPopupOverlay, Pos.CENTER);
        pane.getChildren().add(infoPopupOverlay);
        pane.getStyleClass().add("board-viewport");
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(0));
        pane.setMinSize(0, 0);
        pane.setPrefSize(0, 0);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(pane.widthProperty());
        clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);

        pane.widthProperty().addListener((observable, oldValue, newValue) -> updateBoardScale(pane));
        pane.heightProperty().addListener((observable, oldValue, newValue) -> updateBoardScale(pane));
        return pane;
    }

    private void updateBoardScale(Region viewport) {
        double availableWidth = Math.max(0,
                viewport.getWidth() - viewport.getPadding().getLeft() - viewport.getPadding().getRight());
        double availableHeight = Math.max(0,
                viewport.getHeight() - viewport.getPadding().getTop() - viewport.getPadding().getBottom());
        double scale = 1.0;
        if (availableWidth > 0 && availableHeight > 0) {
            scale = Math.min(availableWidth / DESIGN_WIDTH, availableHeight / DESIGN_HEIGHT);
        }

        boardScale.setX(scale);
        boardScale.setY(scale);
    }

    private VBox buildTabletopCanvas() {
        VBox canvas = new VBox(buildTabletopShell());
        canvas.getStyleClass().add("game-canvas");
        canvas.setAlignment(Pos.TOP_CENTER);
        canvas.setPrefWidth(DESIGN_WIDTH);
        canvas.setMinWidth(DESIGN_WIDTH);
        canvas.setMaxWidth(DESIGN_WIDTH);
        return canvas;
    }

    private Node buildTabletopShell() {
        VBox tableContent = new VBox(
                6,
                buildTopTableRow(),
                buildCenterBand(),
                buildBottomTableRow()
        );
        tableContent.setAlignment(Pos.CENTER);
        tableContent.setPrefWidth(DESIGN_WIDTH);
        tableContent.setMinWidth(DESIGN_WIDTH);

        VBox shell = new VBox(tableContent);
        shell.getStyleClass().add("tabletop-shell");
        shell.setAlignment(Pos.TOP_CENTER);
        shell.setPrefWidth(DESIGN_WIDTH);
        shell.setMinWidth(DESIGN_WIDTH);
        return shell;
    }

    private StackPane buildBottomHudOverlay() {
        StackPane bottomHudOverlay = new StackPane();
        bottomHudOverlay.getStyleClass().add("bottom-hud-overlay");
        bottomHudOverlay.setPadding(new Insets(0, 12, 12, 0));
        bottomHudOverlay.setMaxWidth(Double.MAX_VALUE);
        bottomHudOverlay.setMaxHeight(Region.USE_PREF_SIZE);
        bottomHudOverlay.setPickOnBounds(false);

        VBox statusPanel = buildCompactInfoPanel();
        statusPanel.setMaxHeight(Region.USE_PREF_SIZE);
        statusPanel.setMaxWidth(Double.MAX_VALUE);
        ownedCardsDebugBox.setMaxHeight(Region.USE_PREF_SIZE);
        ownedCardsDebugBox.setMaxWidth(Double.MAX_VALUE);

        VBox leftHudColumn = new VBox(8, statusPanel, ownedCardsDebugBox);
        leftHudColumn.getStyleClass().add("left-hud-column");
        leftHudColumn.setAlignment(Pos.BOTTOM_LEFT);
        leftHudColumn.setPrefWidth(LEFT_HUD_WIDTH);
        leftHudColumn.setMaxWidth(LEFT_HUD_WIDTH);
        leftHudColumn.setMaxHeight(Region.USE_PREF_SIZE);
        bottomHudOverlay.getChildren().add(leftHudColumn);
        StackPane.setAlignment(leftHudColumn, Pos.BOTTOM_LEFT);

        ownedTribeDecksPane.setMaxHeight(Region.USE_PREF_SIZE);
        ownedTribeDecksPane.setMaxWidth(Region.USE_PREF_SIZE);
        bottomHudOverlay.getChildren().add(ownedTribeDecksPane);
        StackPane.setAlignment(ownedTribeDecksPane, Pos.BOTTOM_RIGHT);
        return bottomHudOverlay;
    }

    private Node buildTopTableRow() {
        HBox row = new HBox(6, topRowPane);
        row.getStyleClass().addAll("table-row", "table-row-top");
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(24, 0, 0, 0));
        return row;
    }

    private Node buildBottomTableRow() {
        HBox row = new HBox(6, bottomRowPane);
        row.getStyleClass().addAll("table-row", "table-row-bottom");
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Node buildCenterBand() {
        VBox offerStage = new VBox(10, offerTilesPane, rankingBox);
        offerStage.getStyleClass().add("offer-stage-free");
        offerStage.setAlignment(Pos.CENTER_LEFT);

        HBox centerBand = new HBox(12, minimalTurnBarPane, offerStage);
        centerBand.getStyleClass().add("center-stage");
        centerBand.setAlignment(Pos.TOP_CENTER);
        return centerBand;
    }

    private VBox buildCompactInfoPanel() {
        HBox titleRow = new HBox(6, infoPanelColorIndicator, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleSection = new VBox(2, titleRow, subtitleLabel);
        titleSection.setMaxWidth(650);

        VBox statusLines = new VBox(3, turnLabel, actionHintLabel);
        statusLines.setMaxWidth(380);

        VBox infoPanel = new VBox(10, titleSection, statusLines, actionRow);
        infoPanel.getStyleClass().add("info-panel");
        infoPanel.setAlignment(Pos.TOP_LEFT);
        infoPanel.setPrefWidth(390);
        infoPanel.setMaxWidth(430);
        return infoPanel;
    }

    private Button buildInfoActionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("secondary-button", "board-action-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private Label badgeLabel(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("meta-badge");
        return badge;
    }

    private void rebuildMarketRows() {
        renderSnapshotRow(topRowPane, snapshot.topRow(), true);
        renderSnapshotRow(bottomRowPane, snapshot.bottomRow(), false);
    }

    private void renderSnapshotRow(
            HBox targetRow,
            List<CardSummary> cards,
            boolean topRow
    ) {
        targetRow.getChildren().clear();
        updateMarketRowScale(targetRow, cards.size(), topRow);

        for (CardSummary cardSummary : cards) {
            targetRow.getChildren().add(buildCardNode(
                    cardSummary,
                    false,
                    canPickCard(cardSummary, topRow),
                    topRow,
                    MARKET_CARD_WIDTH,
                    MARKET_CARD_HEIGHT
            ));
        }

        if (targetRow.getChildren().isEmpty()) {
            targetRow.getChildren().add(buildEmptyState("No cards available."));
        }
    }

    private void updateMarketRowScale(HBox targetRow, int cardCount, boolean topRow) {
        if (cardCount <= 0) {
            targetRow.setScaleX(1.0);
            targetRow.setScaleY(1.0);
            targetRow.setPrefWidth(Region.USE_COMPUTED_SIZE);
            return;
        }

        double rowWidth = (MARKET_CARD_WIDTH * cardCount)
                + (MARKET_CARD_ROW_GAP * Math.max(0, cardCount - 1));
        double maxWidth = topRow ? TOP_MARKET_ROW_MAX_WIDTH : BOTTOM_MARKET_ROW_MAX_WIDTH;
        double rowScale = Math.min(1.0, maxWidth / rowWidth);
        targetRow.setScaleX(rowScale);
        targetRow.setScaleY(rowScale);
        targetRow.setPrefWidth(rowWidth);
    }

    private void rebuildOfferTiles() {
        offerTilesPane.getChildren().clear();
        if (snapshot.offerTiles().isEmpty()) {
            offerTilesPane.getChildren().add(buildEmptyState("No offer tiles."));
            return;
        }

        for (OfferTileSummary tile : snapshot.offerTiles()) {
            offerTilesPane.getChildren().add(buildOfferTile(tile));
        }
    }

    private Node buildOfferTile(OfferTileSummary tile) {
        boolean clickable = snapshot.status() == GameStatus.ROUND_OFFER
                && isLocalTurn()
                && tile.available();

        StackPane tileNode = buildSurfaceArt(
                assetResolver.resolveOfferTile(tile.id()),
                OFFER_TILE_WIDTH,
                OFFER_TILE_HEIGHT,
                "Offer " + tile.id(),
                ""
        );
        tileNode.getStyleClass().add("offer-tile-art");
        tileNode.getStyleClass().add(clickable ? "interactive-card" : "static-card");

        if (tile.occupantColor() != null) {
            Node totem = buildTotem(tile.occupantColor(), 30);
            StackPane.setAlignment(totem, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(totem, new Insets(6));
            tileNode.getChildren().add(totem);
        }

        if (clickable) {
            tileNode.setCursor(Cursor.HAND);
            tileNode.setOnMouseClicked(event -> {
                if (lockActionUntilNextSnapshot()) {
                    onChooseOffer.accept(tile.index());
                }
            });
        }

        Tooltip.install(tileNode, new Tooltip("Offer " + tile.id()
                + "\nTop picks: " + tile.topPicks()
                + "\nBottom picks: " + tile.bottomPicks()
                + "\nFood gain: " + tile.foodGain()
                + "\n" + (tile.available() ? "Available" : "Chosen by " + safeText(tile.occupantNickname()))));
        return tileNode;
    }

    private VBox buildMinimalTurnOrderBar() {
        VBox bar = new VBox(4);
        bar.getStyleClass().addAll("minimal-turn-bar", "turn-order-mini-box");
        bar.setAlignment(Pos.TOP_LEFT);
        bar.setMinWidth(MARKET_CARD_WIDTH);
        bar.setPrefWidth(MARKET_CARD_WIDTH);
        bar.setMaxWidth(MARKET_CARD_WIDTH);
        bar.setMinHeight(MARKET_CARD_HEIGHT);
        bar.setPrefHeight(MARKET_CARD_HEIGHT);
        bar.setMaxHeight(MARKET_CARD_HEIGHT);
        List<TurnOrderSpaceSummary> spaces = currentTurnOrderSpaces();
        if (spaces.isEmpty()) {
            Label placeholder = new Label("Waiting...");
            placeholder.getStyleClass().add("turn-order-tile-waiting");
            bar.getChildren().add(placeholder);
        } else {
            for (TurnOrderSpaceSummary space : spaces) {
                bar.getChildren().add(buildMinimalTurnOrderEntry(space));
            }
        }
        return bar;
    }

    private List<TurnOrderSpaceSummary> currentTurnOrderSpaces() {
        if (snapshot == null) {
            return List.of();
        }
        if (!snapshot.turnOrder().isEmpty()) {
            return snapshot.turnOrder();
        }
        if (snapshot.players().isEmpty()) {
            return List.of();
        }

        List<TurnOrderSpaceSummary> fallback = new ArrayList<>();
        for (int index = 0; index < snapshot.players().size(); index++) {
            PlayerSummary player = snapshot.players().get(index);
            fallback.add(new TurnOrderSpaceSummary(
                    index,
                    0,
                    player.color(),
                    player.nickname()
            ));
        }
        return fallback;
    }

    private HBox buildMinimalTurnOrderEntry(TurnOrderSpaceSummary space) {
        boolean active = snapshot != null
                && space.occupantNickname() != null
                && space.occupantNickname().equals(snapshot.currentPlayerNickname());

        HBox entry = new HBox(4);
        entry.getStyleClass().add("turn-order-tile-entry");
        if (active) {
            entry.getStyleClass().add("turn-order-tile-entry-active");
        }
        if (space.occupantNickname() == null) {
            entry.getStyleClass().add("turn-order-tile-entry-empty");
        }
        entry.setAlignment(Pos.CENTER_LEFT);
        entry.setMinWidth(73);
        entry.setPrefWidth(73);
        entry.setMaxWidth(73);

        String nickname = space.occupantNickname() == null ? "Empty" : space.occupantNickname();
        Label nameLabel = new Label(nickname);
        nameLabel.getStyleClass().add("turn-order-tile-name");
        if (space.occupantNickname() == null) {
            nameLabel.getStyleClass().add("turn-order-tile-name-empty");
        }
        nameLabel.setMaxWidth(73);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        if (space.occupantNickname() != null) {
            entry.getChildren().add(buildColorIndicator(space.occupantColor(), "turn-order-color-indicator"));
        }
        entry.getChildren().add(nameLabel);
        return entry;
    }

    private void rebuildRanking() {
        rankingBox.getChildren().clear();
        boolean showRanking = snapshot.finished()
                || snapshot.status() == GameStatus.END_GAME
                || snapshot.status() == GameStatus.END_SCREEN;
        rankingBox.setVisible(showRanking);
        rankingBox.setManaged(showRanking);
        if (!showRanking) {
            return;
        }

        Label title = new Label("Final ranking");
        title.getStyleClass().add("subsection-title");
        rankingBox.getChildren().add(title);

        List<PlayerSummary> ranking = new ArrayList<>(snapshot.players());
        String winner = snapshot.winnerNickname();
        ranking.sort(Comparator.<PlayerSummary, Boolean>comparing(
                p -> p.nickname().equals(winner), Comparator.reverseOrder())
                .thenComparing(Comparator.comparingInt(PlayerSummary::prestigePoints).reversed())
                .thenComparing(PlayerSummary::nickname));

        int rank = 1;
        for (PlayerSummary player : ranking) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("ranking-row");
            row.getChildren().addAll(
                    badgeLabel("#" + rank),
                    buildTotem(player.color(), 22),
                    new Label(player.nickname()),
                    spacer(),
                    badgeLabel(player.prestigePoints() + " pp")
            );
            rankingBox.getChildren().add(row);
            rank++;
        }
    }

    private StackPane buildFinalLeaderboardOverlay() {
        Button lobbyButton = new Button("Back to Lobby");
        lobbyButton.getStyleClass().addAll("secondary-button", "final-leaderboard-lobby-button");
        lobbyButton.setOnAction(event -> onBackToLobby.run());
        lobbyButton.setMaxWidth(Region.USE_PREF_SIZE);

        StackPane overlay = new StackPane(finalLeaderboardBox, lobbyButton);
        overlay.getStyleClass().add("final-leaderboard-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setMinSize(0, 0);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(finalLeaderboardBox, Pos.CENTER);
        StackPane.setAlignment(lobbyButton, Pos.TOP_RIGHT);
        StackPane.setMargin(lobbyButton, new Insets(12, 12, 0, 0));
        return overlay;
    }

    private void rebuildFinalLeaderboard() {
        finalLeaderboardBox.getChildren().clear();

        Label title = new Label("Final Leaderboard");
        title.getStyleClass().add("final-leaderboard-title");
        finalLeaderboardBox.getChildren().add(title);

        List<PlayerSummary> ranking = new ArrayList<>(snapshot.players());
        String winner = snapshot.winnerNickname();
        ranking.sort(Comparator.<PlayerSummary, Boolean>comparing(
                p -> p.nickname().equals(winner), Comparator.reverseOrder())
                .thenComparing(Comparator.comparingInt(PlayerSummary::prestigePoints).reversed())
                .thenComparing(PlayerSummary::nickname));

        int rank = 1;
        for (PlayerSummary player : ranking) {
            HBox row = new HBox(10);
            row.getStyleClass().add("final-leaderboard-row");
            row.setAlignment(Pos.CENTER_LEFT);
            Label playerName = new Label(player.nickname());
            playerName.getStyleClass().add("final-leaderboard-player-name");
            row.getChildren().addAll(
                    badgeLabel("#" + rank),
                    buildColorIndicator(player.color(), "final-leaderboard-color-indicator"),
                    playerName,
                    spacer(),
                    badgeLabel("Food " + player.food()),
                    badgeLabel(player.prestigePoints() + " PP")
            );
            finalLeaderboardBox.getChildren().add(row);
            rank++;
        }
    }

    private void rebuildOwnedCardsDebugBox() {
        ownedCardsDebugBox.getChildren().clear();
        if (snapshot == null) {
            ownedCardsDebugBox.setVisible(false);
            ownedCardsDebugBox.setManaged(false);
            return;
        }

        Optional<PlayerSummary> localPlayer = snapshot.players().stream()
                .filter(player -> player.nickname().equals(localNickname))
                .findFirst();
        if (localPlayer.isEmpty()) {
            ownedCardsDebugBox.setVisible(false);
            ownedCardsDebugBox.setManaged(false);
            return;
        }

        PlayerSummary player = localPlayer.get();
        Label title = new Label("Tribe stats:");
        title.getStyleClass().add("subsection-title");
        ownedCardsDebugBox.getChildren().add(title);

        VBox leftCol = new VBox(6,
                ownedCardsStatRow("Food " + player.food()),
                ownedCardsStatRow("Prestige " + player.prestigePoints()),
                ownedCardsStatRow("Characters " + player.characterCount()),
                ownedCardsStatRow("Buildings " + player.buildingCount())
        );

        VBox rightCol = new VBox(6,
                ownedCardsStatRow("Shamanic stars " + player.shamanStars()),
                ownedCardsStatRow("Gatherers discount " + player.gatherersDiscount()),
                ownedCardsStatRow("Builders discount " + player.buildersDiscount()),
                ownedCardsStatRow("Builders prestige " + player.buildersPrestige()),
                ownedCardsStatRow("Buildings prestige " + player.buildingsPrestige()),
                ownedCardsStatRow("Unique inventors " + player.inventorTypeCount())
        );

        HBox columns = new HBox(16, leftCol, rightCol);
        ownedCardsDebugBox.getChildren().add(columns);

        ownedCardsDebugBox.setVisible(true);
        ownedCardsDebugBox.setManaged(true);
    }

    private Label ownedCardsStatRow(String text) {
        Label row = new Label(text);
        row.getStyleClass().add("owned-cards-debug-row");
        return row;
    }

    private void rebuildPlayerButtonsPane() {
        playerButtonsPane.getChildren().clear();
        if (snapshot == null || snapshot.players().isEmpty()) {
            playerButtonsPane.setVisible(false);
            playerButtonsPane.setManaged(false);
            return;
        }

        for (PlayerSummary player : snapshot.players()) {
            playerButtonsPane.getChildren().add(buildPlayerSummaryButton(player));
        }
        playerButtonsPane.setVisible(true);
        playerButtonsPane.setManaged(true);
    }

    private Button buildPlayerSummaryButton(PlayerSummary player) {
        Button button = new Button(player.nickname());
        button.getStyleClass().add("player-summary-button");
        boolean localPlayer = player.nickname().equals(localNickname);
        if (localPlayer) {
            button.getStyleClass().add("player-summary-button-local");
        }
        if (snapshot != null && player.nickname().equals(snapshot.currentPlayerNickname())) {
            button.getStyleClass().add("player-summary-button-active");
        }
        button.setGraphic(buildColorIndicator(player.color(), "player-summary-color-indicator"));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setMaxWidth(Region.USE_PREF_SIZE);
        if (localPlayer) {
            button.setDisable(true);
        } else {
            button.setOnAction(event -> showPlayerTribePopup(player));
        }
        return button;
    }

    private void showPlayerTribePopup(PlayerSummary player) {
        hideOwnedDeckPreview();
        hideInfoPopup();
        playerTribeOverlay.getChildren().setAll(buildPlayerTribePopup(player));
        playerTribeOverlay.setVisible(true);
    }

    private void hidePlayerTribePopup() {
        playerTribeOverlay.setVisible(false);
        playerTribeOverlay.getChildren().clear();
    }

    /**
     * Replaces the rendered event log with the given history and refreshes the
     * top-right "Event Log" button (and the open popup, if any).
     *
     * @param log full event history to display; null is treated as empty
     */
    public void setEventLog(List<EventLogEntry> log) {
        this.eventLog = log == null ? List.of() : log;
        rebuildEventLogPane();
        if (eventLogOverlay.isVisible()) {
            eventLogOverlay.getChildren().setAll(buildEventLogPopup());
        }
    }

    private void rebuildEventLogPane() {
        eventLogPane.getChildren().clear();
        Button button = new Button("Event Log (" + eventLog.size() + ")");
        button.getStyleClass().add("event-log-button");
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setOnAction(event -> showEventLogPopup());
        eventLogPane.getChildren().add(button);
    }

    private void showEventLogPopup() {
        hideOwnedDeckPreview();
        hidePlayerTribePopup();
        eventLogOverlay.getChildren().setAll(buildEventLogPopup());
        eventLogOverlay.setVisible(true);
    }

    private void hideEventLogPopup() {
        eventLogOverlay.setVisible(false);
        eventLogOverlay.getChildren().clear();
    }

    private VBox buildEventLogPopup() {
        HBox titleRow = new HBox(8);
        titleRow.getStyleClass().add("player-tribe-header");
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Event Log");
        title.getStyleClass().add("player-tribe-popup-title");

        Button close = new Button("Close");
        close.getStyleClass().addAll("secondary-button", "player-tribe-close-button");
        close.setOnAction(event -> hideEventLogPopup());

        titleRow.getChildren().addAll(title, spacer(), close);

        VBox entries = new VBox(12);
        entries.getStyleClass().add("event-log-entries");
        if (eventLog.isEmpty()) {
            entries.getChildren().add(playerTribeEmptyLabel("No events yet."));
        } else {
            // newest first so the most recent event is immediately visible
            for (int index = eventLog.size() - 1; index >= 0; index--) {
                entries.getChildren().add(buildEventLogEntryNode(eventLog.get(index)));
            }
        }

        ScrollPane scroll = new ScrollPane(entries);
        scroll.getStyleClass().add("player-tribe-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(360);

        VBox popup = new VBox(12, titleRow, scroll);
        popup.getStyleClass().addAll("player-tribe-popup", "event-log-popup");
        popup.setAlignment(Pos.TOP_LEFT);
        popup.setMaxWidth(520);
        popup.setMaxHeight(540);
        return popup;
    }

    private Node buildEventLogEntryNode(EventLogEntry entry) {
        Label title = new Label(EventMessageFormatter.title(entry));
        title.getStyleClass().add("subsection-title");

        Label body = new Label(EventMessageFormatter.body(entry));
        body.getStyleClass().add("details-label");
        body.setWrapText(true);

        VBox group = new VBox(4, title, body);
        group.getStyleClass().add("event-log-entry");
        return group;
    }

    private void showInfoPopup() {
        hideOwnedDeckPreview();
        hidePlayerTribePopup();
        infoPopupOverlay.getChildren().setAll(buildInfoPopup());
        infoPopupOverlay.setVisible(true);
    }

    private void hideInfoPopup() {
        infoPopupOverlay.setVisible(false);
        infoPopupOverlay.getChildren().clear();
    }

    private HBox buildInfoPopup() {
        StackPane roundInfoCard = buildSurfaceArt(
                assetResolver.resolveRoundEventInfo(),
                INFO_CARD_WIDTH,
                INFO_CARD_HEIGHT,
                "Round info",
                ""
        );
        roundInfoCard.getStyleClass().add("info-popup-card");

        StackPane endGameInfoCard = buildSurfaceArt(
                assetResolver.resolveEndGameInfo(),
                INFO_CARD_WIDTH,
                INFO_CARD_HEIGHT,
                "End game info",
                ""
        );
        endGameInfoCard.getStyleClass().add("info-popup-card");

        HBox popup = new HBox(12, roundInfoCard, endGameInfoCard);
        popup.getStyleClass().addAll("player-tribe-popup", "info-popup-panel");
        popup.setAlignment(Pos.CENTER);
        popup.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        popup.setOnMouseClicked(event -> event.consume());
        return popup;
    }

    private VBox buildPlayerTribePopup(PlayerSummary player) {
        HBox titleRow = new HBox(8);
        titleRow.getStyleClass().add("player-tribe-header");
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(player.nickname() + "'s Tribe");
        title.getStyleClass().add("player-tribe-popup-title");

        Button close = new Button("Close");
        close.getStyleClass().addAll("secondary-button", "player-tribe-close-button");
        close.setOnAction(event -> hidePlayerTribePopup());

        titleRow.getChildren().addAll(
                buildColorIndicator(player.color(), "player-tribe-color-indicator"),
                title,
                spacer(),
                close
        );

        HBox resources = new HBox(8);
        resources.setAlignment(Pos.CENTER_LEFT);
        resources.getChildren().addAll(
                playerTribeResource("Food " + player.food()),
                playerTribeResource("Prestige " + player.prestigePoints())
        );

        VBox tribeCards = buildPlayerTribeCardGroups(player);
        ScrollPane scroll = new ScrollPane(tribeCards);
        scroll.getStyleClass().add("player-tribe-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(360);

        VBox popup = new VBox(12, titleRow, resources, scroll);
        popup.getStyleClass().add("player-tribe-popup");
        popup.setAlignment(Pos.TOP_LEFT);
        popup.setMaxWidth(760);
        popup.setMaxHeight(540);
        return popup;
    }

    private Label playerTribeResource(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("meta-badge", "player-tribe-resource");
        return label;
    }

    private VBox buildPlayerTribeCardGroups(PlayerSummary player) {
        VBox groups = new VBox(10);
        groups.getStyleClass().add("player-tribe-card-groups");

        Map<String, List<CardSummary>> cardsByType = new LinkedHashMap<>();
        for (CardSummary card : player.characterCards()) {
            cardsByType.computeIfAbsent(normalizeDeckKey(card.type()), ignored -> new ArrayList<>()).add(card);
        }

        boolean hasCharacterCards = false;
        for (OwnedCardDeckDefinition definition : OWNED_CARD_DECK_DEFINITIONS) {
            if (definition.buildingDeck()) {
                continue;
            }
            List<CardSummary> cards = cardsByType.getOrDefault(definition.key(), List.of());
            if (!cards.isEmpty()) {
                String extra = switch (definition.key()) {
                    case "shaman"   -> player.shamanStars() + " shamanic stars";
                    case "builder"  -> player.buildersDiscount() + " food discount" + "   ◆   " + player.buildersPrestige() + " prestige points";
                    case "gatherer" -> player.gatherersDiscount() + " food discount";
                    case "inventor" -> player.inventorTypeCount() + " unique types";
                    default         -> "";
                };
                groups.getChildren().add(buildPlayerTribeCardGroup(definition.label(), cards, extra));
                hasCharacterCards = true;
            }
        }

        if (!hasCharacterCards) {
            groups.getChildren().add(playerTribeEmptyLabel("No character cards"));
        }
        if (player.buildingCards().isEmpty()) {
            groups.getChildren().add(playerTribeEmptyLabel("No building cards"));
        } else {
            groups.getChildren().add(buildPlayerTribeCardGroup(
                    "Buildings", player.buildingCards(), player.buildingsPrestige() + " prestige points"));
        }
        return groups;
    }

    private Label playerTribeEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("muted-label", "player-tribe-empty");
        return label;
    }

    private VBox buildPlayerTribeCardGroup(String title, List<CardSummary> cards, String extra) {
        String suffix = extra.isEmpty() ? "" : "   ◆   " + extra;
        Label label = new Label(title + " (" + cards.size() + ")" + suffix);
        label.getStyleClass().add("player-tribe-card-group-title");

        FlowPane cardGrid = new FlowPane(6, 6);
        cardGrid.getStyleClass().add("player-tribe-card-grid");
        cardGrid.setMaxWidth(700);
        for (CardSummary card : cards) {
            cardGrid.getChildren().add(buildOwnedTribeCardSurface(
                    card,
                    PLAYER_TRIBE_CARD_WIDTH,
                    PLAYER_TRIBE_CARD_HEIGHT,
                    "player-tribe-card"
            ));
        }

        VBox group = new VBox(6, label, cardGrid);
        group.getStyleClass().add("player-tribe-card-group");
        return group;
    }

    private void rebuildOwnedTribeDecksPane() {
        ownedTribeDecksPane.getChildren().clear();
        if (snapshot == null) {
            hideOwnedDeckPreview();
            ownedTribeDecksPane.setVisible(false);
            ownedTribeDecksPane.setManaged(false);
            return;
        }

        Optional<PlayerSummary> localPlayer = snapshot.players().stream()
                .filter(player -> player.nickname().equals(localNickname))
                .findFirst();
        if (localPlayer.isEmpty()) {
            hideOwnedDeckPreview();
            ownedTribeDecksPane.setVisible(false);
            ownedTribeDecksPane.setManaged(false);
            return;
        }

        PlayerSummary player = localPlayer.get();
        Map<String, List<CardSummary>> cardsByType = new LinkedHashMap<>();
        for (CardSummary card : player.characterCards()) {
            cardsByType.computeIfAbsent(normalizeDeckKey(card.type()), ignored -> new ArrayList<>()).add(card);
        }

        for (OwnedCardDeckDefinition definition : OWNED_CARD_DECK_DEFINITIONS) {
            List<CardSummary> cards = definition.buildingDeck()
                    ? player.buildingCards()
                    : cardsByType.getOrDefault(definition.key(), List.of());
            ownedTribeDecksPane.getChildren().add(buildOwnedCardDeck(definition, cards));
        }

        ownedTribeDecksPane.setVisible(true);
        ownedTribeDecksPane.setManaged(true);
    }

    private StackPane buildOwnedCardDeck(OwnedCardDeckDefinition definition, List<CardSummary> cards) {
        StackPane deck = new StackPane();
        deck.getStyleClass().addAll("owned-tribe-deck", "owned-card-deck-slot");
        deck.getStyleClass().add(cards.isEmpty() ? "owned-card-deck-empty" : "owned-card-deck-occupied");
        deck.setAlignment(Pos.BOTTOM_CENTER);
        deck.setCursor(cards.isEmpty() ? Cursor.DEFAULT : Cursor.HAND);
        deck.setMinSize(OWNED_CARD_DECK_SLOT_WIDTH, OWNED_CARD_DECK_SLOT_HEIGHT);
        deck.setPrefSize(OWNED_CARD_DECK_SLOT_WIDTH, OWNED_CARD_DECK_SLOT_HEIGHT);
        deck.setMaxSize(OWNED_CARD_DECK_SLOT_WIDTH, OWNED_CARD_DECK_SLOT_HEIGHT);
        deck.getChildren().add(buildOwnedCardDeckPlaceholder(definition.label()));

        int visibleCards = Math.min(cards.size(), OWNED_CARD_DECK_MAX_VISIBLE_CARDS);
        int firstVisibleCard = Math.max(0, cards.size() - visibleCards);
        for (int index = firstVisibleCard; index < cards.size(); index++) {
            int visibleIndex = index - firstVisibleCard;
            int visibleOffset = visibleCards - 1 - visibleIndex;
            StackPane card = buildOwnedTribeCardSurface(
                    cards.get(index),
                    OWNED_TRIBE_CARD_WIDTH,
                    OWNED_TRIBE_CARD_HEIGHT,
                    "owned-card-deck-card"
            );
            card.getStyleClass().add("owned-tribe-deck-card");
            card.setTranslateX(-visibleOffset * OWNED_CARD_DECK_STACK_X);
            card.setTranslateY(-visibleOffset * OWNED_CARD_DECK_STACK_Y);
            deck.getChildren().add(card);
        }

        if (cards.size() > 1) {
            Label count = new Label(String.valueOf(cards.size()));
            count.getStyleClass().add("owned-tribe-deck-count");
            StackPane.setAlignment(count, Pos.TOP_RIGHT);
            StackPane.setMargin(count, new Insets(0, 2, 0, 0));
            deck.getChildren().add(count);
        }

        deck.setOnMouseEntered(event -> showOwnedDeckPreview(cards));
        deck.setOnMouseExited(event -> hideOwnedDeckPreview());
        return deck;
    }

    private StackPane buildOwnedCardDeckPlaceholder(String labelText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("owned-card-deck-placeholder-label");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(OWNED_TRIBE_CARD_WIDTH + 8);
        label.setAlignment(Pos.CENTER);

        StackPane placeholder = new StackPane(label);
        placeholder.getStyleClass().add("owned-card-deck-placeholder");
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMinSize(OWNED_TRIBE_CARD_WIDTH, OWNED_TRIBE_CARD_HEIGHT);
        placeholder.setPrefSize(OWNED_TRIBE_CARD_WIDTH, OWNED_TRIBE_CARD_HEIGHT);
        placeholder.setMaxSize(OWNED_TRIBE_CARD_WIDTH, OWNED_TRIBE_CARD_HEIGHT);
        return placeholder;
    }

    private void showOwnedDeckPreview(List<CardSummary> cards) {
        ownedDeckPreviewOverlay.getChildren().clear();
        if (cards.isEmpty()) {
            ownedDeckPreviewOverlay.setVisible(false);
            return;
        }

        FlowPane previewCards = new FlowPane(OWNED_CARD_PREVIEW_GAP, OWNED_CARD_PREVIEW_ROW_GAP);
        previewCards.getStyleClass().add("owned-card-preview-grid");
        previewCards.setAlignment(Pos.CENTER);
        int visibleColumns = Math.min(cards.size(), OWNED_CARD_PREVIEW_COLUMNS);
        int visibleRows = (int) Math.ceil(cards.size() / (double) OWNED_CARD_PREVIEW_COLUMNS);
        double contentWidth = (MARKET_CARD_WIDTH * visibleColumns)
                + (OWNED_CARD_PREVIEW_GAP * Math.max(0, visibleColumns - 1));
        double contentHeight = (MARKET_CARD_HEIGHT * visibleRows)
                + (OWNED_CARD_PREVIEW_ROW_GAP * Math.max(0, visibleRows - 1));
        double previewScale = Math.min(
                OWNED_CARD_PREVIEW_MAX_SCALE,
                Math.min(
                        OWNED_CARD_PREVIEW_MAX_WIDTH / contentWidth,
                        OWNED_CARD_PREVIEW_MAX_HEIGHT / contentHeight
                )
        );
        previewCards.scaleXProperty().bind(boardScale.xProperty().multiply(previewScale));
        previewCards.scaleYProperty().bind(boardScale.yProperty().multiply(previewScale));
        previewCards.setMaxWidth(contentWidth);

        for (CardSummary card : cards) {
            previewCards.getChildren().add(buildOwnedTribeCardSurface(
                    card,
                    MARKET_CARD_WIDTH,
                    MARKET_CARD_HEIGHT,
                    "owned-card-preview-card"
            ));
        }

        ownedDeckPreviewOverlay.getChildren().setAll(previewCards);
        ownedDeckPreviewOverlay.setVisible(true);
    }

    private void hideOwnedDeckPreview() {
        ownedDeckPreviewOverlay.setVisible(false);
        ownedDeckPreviewOverlay.getChildren().clear();
    }

    private String normalizeDeckKey(String type) {
        if (type == null) {
            return "";
        }
        return type.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private StackPane buildOwnedTribeCardSurface(
            CardSummary cardSummary,
            double width,
            double height,
            String styleClass
    ) {
        StackPane surface = buildSurfaceArt(
                assetResolver.resolveCard(cardSummary),
                width,
                height,
                cardSummary.type(),
                cardSummary.details()
        );
        surface.getStyleClass().add(styleClass);
        Tooltip tooltip = new Tooltip(buildTooltipText(cardSummary));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(280);
        Tooltip.install(surface, tooltip);
        return surface;
    }

    private Node buildCardNode(
            CardSummary cardSummary,
            boolean ownedCard,
            boolean clickable,
            Boolean topRow,
            double width,
            double height
    ) {
        StackPane surface = buildSurfaceArt(
                assetResolver.resolveCard(cardSummary),
                width,
                height,
                cardSummary.type(),
                cardSummary.details()
        );
        surface.getStyleClass().addAll("market-card", clickable ? "interactive-card" : "static-card");
        if (ownedCard) {
            surface.getStyleClass().add("owned-card");
        }

        if (clickable) {
            surface.setCursor(Cursor.HAND);
            surface.setOnMouseClicked(event -> {
                if (lockActionUntilNextSnapshot()) {
                    onChooseCard.accept(cardSummary.selectionIndex(), Boolean.TRUE.equals(topRow));
                }
            });
        }

        Tooltip tooltip = new Tooltip(buildTooltipText(cardSummary));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(280);
        if (ownedCard) {
            StackPane slot = new StackPane(surface);
            slot.getStyleClass().add("owned-card-slot");
            slot.setAlignment(Pos.BOTTOM_CENTER);
            setFixedSize(slot, width + 18, height + 44);
            slot.setOnMouseEntered(event -> {
                slot.toFront();
                surface.setScaleX(1.85);
                surface.setScaleY(1.85);
                surface.setTranslateY(-26);
            });
            slot.setOnMouseExited(event -> {
                surface.setScaleX(1.0);
                surface.setScaleY(1.0);
                surface.setTranslateY(0);
            });
            Tooltip.install(slot, tooltip);
            return slot;
        }

        StackPane slot = new StackPane(surface);
        slot.getStyleClass().add("market-card-slot");
        slot.setAlignment(Pos.CENTER);
        setFixedSize(slot, width, MARKET_CARD_SLOT_HEIGHT);
        Tooltip.install(slot, tooltip);
        return slot;
    }

    private void setFixedSize(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }

    private String buildTooltipText(CardSummary cardSummary) {
        StringBuilder builder = new StringBuilder(cardSummary.type())
                .append("\nEra: ").append(formatEra(cardSummary.era()));
        if (cardSummary.foodCost() != null) {
            builder.append("\nFood cost: ").append(cardSummary.foodCost());
        }
        if (cardSummary.prestigePoints() != null) {
            builder.append("\nPrestige: ").append(cardSummary.prestigePoints());
        }
        if (!cardSummary.details().isBlank()) {
            builder.append("\n").append(cardSummary.details());
        }
        return builder.toString();
    }

    private StackPane buildSurfaceArt(
            Optional<Image> image,
            double width,
            double height,
            String placeholderTitle,
            String placeholderSubtitle
    ) {
        StackPane surface = new StackPane();
        surface.getStyleClass().add("art-surface");
        surface.setMinSize(width, height);
        surface.setPrefSize(width, height);
        surface.setMaxSize(width, height);

        if (image.isPresent()) {
            ImageView imageView = new ImageView(image.get());
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setSmooth(true);
            surface.getChildren().add(imageView);
        } else {
            VBox placeholder = new VBox(6);
            placeholder.setAlignment(Pos.CENTER);
            placeholder.getStyleClass().add("placeholder-card");

            Label title = new Label(placeholderTitle);
            title.getStyleClass().add("card-title");
            title.setWrapText(true);

            Label subtitle = new Label(placeholderSubtitle == null || placeholderSubtitle.isBlank()
                    ? "Artwork unavailable"
                    : placeholderSubtitle);
            subtitle.getStyleClass().add("details-label");
            subtitle.setWrapText(true);

            placeholder.getChildren().addAll(title, subtitle);
            placeholder.setMaxWidth(width - 12);
            surface.getChildren().add(placeholder);
        }

        return surface;
    }

    private Node buildEmptyState(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted-label");
        label.setWrapText(true);

        StackPane wrapper = new StackPane(label);
        wrapper.getStyleClass().add("empty-state");
        wrapper.setPadding(new Insets(18));
        return wrapper;
    }

    private StackPane buildColorIndicator(
            it.polimi.ingsw.mesos.Model.Enums.Color color,
            String extraStyleClass
    ) {
        StackPane indicator = new StackPane();
        indicator.getStyleClass().add("player-color-indicator");
        if (extraStyleClass != null && !extraStyleClass.isBlank()) {
            indicator.getStyleClass().add(extraStyleClass);
        }
        indicator.setMinSize(12, 12);
        indicator.setPrefSize(12, 12);
        indicator.setMaxSize(12, 12);

        Circle dot = new Circle(5);
        dot.getStyleClass().add("player-color-indicator-dot");
        indicator.getChildren().add(dot);
        indicator.setUserData(dot);
        updateColorIndicator(indicator, color);
        return indicator;
    }

    private void updateColorIndicator(StackPane indicator, it.polimi.ingsw.mesos.Model.Enums.Color color) {
        Circle dot = (Circle) indicator.getUserData();
        if (dot != null) {
            dot.setFill(toIndicatorColor(color));
            dot.setStroke(Color.rgb(255, 236, 201, 0.72));
            dot.setStrokeWidth(1);
        }
    }

    private Color toIndicatorColor(it.polimi.ingsw.mesos.Model.Enums.Color color) {
        return switch (color == null ? it.polimi.ingsw.mesos.Model.Enums.Color.WHITE : color) {
            case BLUE -> Color.rgb(57, 146, 218);
            case YELLOW -> Color.rgb(244, 196, 48);
            case ORANGE -> Color.rgb(230, 92, 36);
            case WHITE -> Color.rgb(239, 232, 214);
            case BLACK -> Color.rgb(94, 53, 137);
        };
    }

    private Node buildTotem(it.polimi.ingsw.mesos.Model.Enums.Color color, double size) {
        Optional<Image> totem = assetResolver.resolveTotem(color);
        if (totem.isPresent()) {
            ImageView view = new ImageView(totem.get());
            view.setPreserveRatio(true);
            view.setFitWidth(size);
            view.setFitHeight(size);
            return view;
        }

        StackPane fallback = new StackPane();
        fallback.setMinSize(size, size);
        fallback.setPrefSize(size, size);
        fallback.setMaxSize(size, size);
        fallback.getStyleClass().add("totem-fallback");

        Label label = new Label(color == null ? "?" : color.name().substring(0, 1));
        label.setTextFill(Color.WHITE);
        label.getStyleClass().add("card-title");
        fallback.getChildren().add(label);
        return fallback;
    }

    private boolean canPickCard(CardSummary cardSummary, boolean topRow) {
        if (snapshot == null || cardSummary == null) {
            return false;
        }
        if (actionPending || !isLocalTurn() || snapshot.status() != GameStatus.ROUND_ACTIONS || cardSummary.event()) {
            return false;
        }
        return topRow ? snapshot.remainingTopChoices() > 0 : snapshot.remainingBottomChoices() > 0;
    }

    private boolean lockActionUntilNextSnapshot() {
        if (actionPending) {
            return false;
        }
        actionPending = true;
        return true;
    }

    private boolean isLocalTurn() {
        return snapshot != null
                && snapshot.currentPlayerNickname() != null
                && snapshot.currentPlayerNickname().equals(localNickname);
    }

    private boolean canUsePass() {
        return snapshot != null && !actionPending && isLocalTurn() && snapshot.passAvailable();
    }

    private String formatEra(Era era) {
        if (era == null) {
            return "-";
        }
        return switch (era) {
            case FIRST -> "I";
            case SECOND -> "II";
            case THIRD -> "III";
        };
    }

    private String safeText(Object value) {
        return value == null ? "-" : value.toString();
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

}
