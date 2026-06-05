package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
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
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public final class MesosGameBoardView extends BorderPane {
    private static final double DESIGN_WIDTH = 920;
    private static final double DESIGN_HEIGHT = 370;
    private static final double MARKET_CARD_WIDTH = 85;
    private static final double MARKET_CARD_HEIGHT = 130;
    private static final double OFFER_TILE_WIDTH = 85;
    private static final double OFFER_TILE_HEIGHT = 130;
    private static final double OWNED_TRIBE_CARD_WIDTH = 54;
    private static final double OWNED_TRIBE_CARD_HEIGHT = 82;
    private static final double OWNED_TRIBE_POPUP_CARD_WIDTH = 68;
    private static final double OWNED_TRIBE_POPUP_CARD_HEIGHT = 104;
    private final String localNickname;
    private final MesosAssetResolver assetResolver;
    private final Runnable onRefresh;
    private final Runnable onDisconnect;
    private final Runnable onPass;
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
    private final VBox minimalTurnBarPane;
    private final StackPane infoPanelColorIndicator;
    private final StackPane viewport;
    private final VBox tabletopCanvas;
    private final Scale boardScale;

    private GameSnapshot snapshot;

    public MesosGameBoardView(
            String localNickname,
            MesosAssetResolver assetResolver,
            Runnable onRefresh,
            Runnable onDisconnect,
            IntConsumer onChooseOffer,
            BiConsumer<Integer, Boolean> onChooseCard,
            Runnable onPass
    ) {
        this.localNickname = localNickname;
        this.assetResolver = assetResolver;
        this.onRefresh = onRefresh;
        this.onDisconnect = onDisconnect;
        this.onChooseOffer = onChooseOffer;
        this.onChooseCard = onChooseCard;
        this.onPass = onPass;

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

        this.topRowPane = new HBox(4);
        this.bottomRowPane = new HBox(4);
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

        this.infoPanelColorIndicator = buildColorIndicator(null, "info-panel-color-indicator");
        this.infoPanelColorIndicator.setVisible(false);
        this.infoPanelColorIndicator.setManaged(false);
        this.minimalTurnBarPane = buildMinimalTurnOrderBar();
        this.tabletopCanvas = buildTabletopCanvas();
        this.boardScale = new Scale(1, 1, 0, 0);
        this.viewport = buildViewport();

        buildLayout();
    }

    public void updateSnapshot(GameSnapshot snapshot, String updateMessage) {
        this.snapshot = snapshot;
        if (snapshot == null) {
            return;
        }

        titleLabel.setText("Game Status:");
        subtitleLabel.setText("Round " + snapshot.currentRound() + "/3");
        turnLabel.setText("Era: " + formatEraName(snapshot.currentEra()));
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
        // apply a 7% top offset to the tabletop (cards) so the background stays fixed
        viewport.heightProperty().addListener((observable, oldValue, newValue) -> {
            double top = Math.max(0, newValue.doubleValue() * 0.07);
            tabletopCanvas.setTranslateY(top);
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
        BorderPane bottomHudOverlay = buildBottomHudOverlay();
        StackPane.setAlignment(bottomHudOverlay, Pos.BOTTOM_CENTER);
        pane.getChildren().add(bottomHudOverlay);
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
        tabletopCanvas.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> updateBoardScale(pane));
        return pane;
    }

    private void updateBoardScale(Region viewport) {
        double availableWidth = Math.max(0,
                viewport.getWidth() - viewport.getPadding().getLeft() - viewport.getPadding().getRight());
        double availableHeight = Math.max(0,
                viewport.getHeight() - viewport.getPadding().getTop() - viewport.getPadding().getBottom());
        double contentWidth = Math.max(DESIGN_WIDTH, tabletopCanvas.getLayoutBounds().getWidth());
        double contentHeight = Math.max(DESIGN_HEIGHT, tabletopCanvas.getLayoutBounds().getHeight());

        double scale = 1.0;
        if (availableWidth > 0 && availableHeight > 0) {
            scale = Math.min(availableWidth / contentWidth, availableHeight / contentHeight);
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

    private BorderPane buildBottomHudOverlay() {
        BorderPane bottomHudOverlay = new BorderPane();
        bottomHudOverlay.getStyleClass().add("bottom-hud-overlay");
        bottomHudOverlay.setPadding(new Insets(0, 12, 12, 0));
        bottomHudOverlay.setMaxWidth(Double.MAX_VALUE);
        bottomHudOverlay.setMaxHeight(Region.USE_PREF_SIZE);
        bottomHudOverlay.setPickOnBounds(false);

        ownedCardsDebugBox.setMaxHeight(Region.USE_PREF_SIZE);
        bottomHudOverlay.setLeft(ownedCardsDebugBox);
        BorderPane.setAlignment(ownedCardsDebugBox, Pos.BOTTOM_LEFT);

        VBox statusPanel = buildCompactInfoPanel();
        statusPanel.setMaxHeight(Region.USE_PREF_SIZE);
        bottomHudOverlay.setCenter(statusPanel);
        BorderPane.setAlignment(statusPanel, Pos.BOTTOM_CENTER);

        ownedTribeDecksPane.setMaxHeight(Region.USE_PREF_SIZE);
        bottomHudOverlay.setRight(ownedTribeDecksPane);
        BorderPane.setAlignment(ownedTribeDecksPane, Pos.BOTTOM_RIGHT);
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
            tileNode.setOnMouseClicked(event -> onChooseOffer.accept(tile.index()));
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
        ranking.sort(Comparator.comparingInt(PlayerSummary::prestigePoints).reversed()
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
        Label title = new Label("Owned Cards:");
        title.getStyleClass().add("subsection-title");
        ownedCardsDebugBox.getChildren().add(title);

        ownedCardsDebugBox.getChildren().addAll(
                ownedCardsStatRow("Food " + player.food()),
                ownedCardsStatRow("Prestige " + player.prestigePoints()),
                ownedCardsStatRow("Characters " + player.characterCount()),
                ownedCardsStatRow("Buildings " + player.buildingCount())
        );

        ownedCardsDebugBox.setVisible(true);
        ownedCardsDebugBox.setManaged(true);
    }

    private Label ownedCardsStatRow(String text) {
        Label row = new Label(text);
        row.getStyleClass().add("owned-cards-debug-row");
        return row;
    }

    private void rebuildOwnedTribeDecksPane() {
        ownedTribeDecksPane.getChildren().clear();
        if (snapshot == null) {
            ownedTribeDecksPane.setVisible(false);
            ownedTribeDecksPane.setManaged(false);
            return;
        }

        Optional<PlayerSummary> localPlayer = snapshot.players().stream()
                .filter(player -> player.nickname().equals(localNickname))
                .findFirst();
        if (localPlayer.isEmpty()) {
            ownedTribeDecksPane.setVisible(false);
            ownedTribeDecksPane.setManaged(false);
            return;
        }

        Map<String, List<CardSummary>> cardsByType = new LinkedHashMap<>();
        for (CardSummary card : localPlayer.get().characterCards()) {
            cardsByType.computeIfAbsent(card.type(), ignored -> new ArrayList<>()).add(card);
        }

        cardsByType.values().stream()
                .map(this::buildOwnedTribeDeck)
                .forEach(ownedTribeDecksPane.getChildren()::add);

        boolean hasCards = !ownedTribeDecksPane.getChildren().isEmpty();
        ownedTribeDecksPane.setVisible(hasCards);
        ownedTribeDecksPane.setManaged(hasCards);
    }

    private StackPane buildOwnedTribeDeck(List<CardSummary> cards) {
        StackPane deck = new StackPane();
        deck.getStyleClass().add("owned-tribe-deck");
        deck.setAlignment(Pos.BOTTOM_CENTER);
        deck.setCursor(Cursor.HAND);
        deck.setMinSize(OWNED_TRIBE_CARD_WIDTH + 14, OWNED_TRIBE_CARD_HEIGHT + 14);
        deck.setPrefSize(OWNED_TRIBE_CARD_WIDTH + 14, OWNED_TRIBE_CARD_HEIGHT + 14);
        deck.setMaxSize(OWNED_TRIBE_CARD_WIDTH + 14, OWNED_TRIBE_CARD_HEIGHT + 14);

        int firstVisibleCardIndex = Math.max(0, cards.size() - 3);
        for (int index = firstVisibleCardIndex; index < cards.size(); index++) {
            int distanceFromFront = cards.size() - 1 - index;
            StackPane card = buildOwnedTribeCardSurface(
                    cards.get(index),
                    OWNED_TRIBE_CARD_WIDTH,
                    OWNED_TRIBE_CARD_HEIGHT,
                    "owned-tribe-deck-card"
            );
            card.setTranslateX(-distanceFromFront * 5.0);
            card.setTranslateY(-distanceFromFront * 2.0);
            deck.getChildren().add(card);
        }

        if (cards.size() > 1) {
            Label count = new Label(String.valueOf(cards.size()));
            count.getStyleClass().add("owned-tribe-deck-count");
            StackPane.setAlignment(count, Pos.TOP_RIGHT);
            StackPane.setMargin(count, new Insets(0, 2, 0, 0));
            deck.getChildren().add(count);
        }

        HBox popup = buildOwnedTribeDeckPopup(cards);
        popup.setVisible(false);
        popup.setManaged(false);
        StackPane.setAlignment(popup, Pos.BOTTOM_RIGHT);
        popup.setTranslateY(-OWNED_TRIBE_CARD_HEIGHT - 18);
        popup.setTranslateX(-Math.max(0, cards.size() - 1) * 28.0);
        deck.getChildren().add(popup);

        deck.setOnMouseEntered(event -> {
            popup.setVisible(true);
            deck.toFront();
        });
        deck.setOnMouseExited(event -> popup.setVisible(false));
        return deck;
    }

    private HBox buildOwnedTribeDeckPopup(List<CardSummary> cards) {
        HBox popup = new HBox(-10);
        popup.getStyleClass().add("owned-tribe-deck-popup");
        popup.setAlignment(Pos.BOTTOM_CENTER);
        popup.setPickOnBounds(false);
        for (CardSummary card : cards) {
            popup.getChildren().add(buildOwnedTribeCardSurface(
                    card,
                    OWNED_TRIBE_POPUP_CARD_WIDTH,
                    OWNED_TRIBE_POPUP_CARD_HEIGHT,
                    "owned-tribe-popup-card"
            ));
        }
        return popup;
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
            surface.setOnMouseClicked(event -> onChooseCard.accept(cardSummary.selectionIndex(), Boolean.TRUE.equals(topRow)));
        }

        Tooltip tooltip = new Tooltip(buildTooltipText(cardSummary));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(280);
        if (ownedCard) {
            StackPane slot = new StackPane(surface);
            slot.getStyleClass().add("owned-card-slot");
            slot.setAlignment(Pos.BOTTOM_CENTER);
            slot.setMinSize(width + 18, height + 44);
            slot.setPrefSize(width + 18, height + 44);
            slot.setMaxSize(width + 18, height + 44);
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

        Tooltip.install(surface, tooltip);
        return surface;
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
        updateColorIndicator(indicator, color);
        return indicator;
    }

    private void updateColorIndicator(StackPane indicator, it.polimi.ingsw.mesos.Model.Enums.Color color) {
        if (!indicator.getChildren().isEmpty() && indicator.getChildren().getFirst() instanceof Circle dot) {
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
        if (!isLocalTurn() || snapshot.status() != GameStatus.ROUND_ACTIONS || cardSummary.event()) {
            return false;
        }
        return topRow ? snapshot.remainingTopChoices() > 0 : snapshot.remainingBottomChoices() > 0;
    }

    private boolean isLocalTurn() {
        return snapshot != null
                && snapshot.currentPlayerNickname() != null
                && snapshot.currentPlayerNickname().equals(localNickname);
    }

    private boolean canUsePass() {
        return snapshot != null && isLocalTurn() && snapshot.passAvailable();
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

    private String formatEraName(Era era) {
        if (era == null) {
            return "-";
        }
        return switch (era) {
            case FIRST -> "Paleolithic";
            case SECOND -> "Mesolithic";
            case THIRD -> "Neolithic";
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
