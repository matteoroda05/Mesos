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
    private final Label roundBadgeLabel;
    private final Label eraBadgeLabel;
    private final Label phaseBadgeLabel;
    private final Label currentBadgeLabel;
    private final Label picksBadgeLabel;
    private final FlowPane actionRow;
    private final Button passButton;
    private final HBox topRowPane;
    private final HBox bottomRowPane;
    private final HBox offerTilesPane;
    private final VBox rankingBox;
    private final VBox ownedCardsDebugBox;
    private final VBox minimalTurnBarPane;
    private final VBox totemWrapper;
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

        this.roundBadgeLabel = badgeLabel("Round -");
        this.eraBadgeLabel = badgeLabel("Era -");
        this.phaseBadgeLabel = badgeLabel("Phase -");
        this.currentBadgeLabel = badgeLabel("Active -");
        this.picksBadgeLabel = badgeLabel("Picks -");
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

        this.totemWrapper = new VBox(4);
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

        titleLabel.setText(snapshot.gameName());
        subtitleLabel.setText("Players " + snapshot.joinedPlayers() + "/" + snapshot.playerCount()
                + " | " + (snapshot.started() ? "Live match" : "Lobby view"));
        roundBadgeLabel.setText("Round " + snapshot.currentRound());
        eraBadgeLabel.setText("Era " + formatEra(snapshot.currentEra()));
        phaseBadgeLabel.setText(MesosUserMessageMapper.describePhase(snapshot.status()));
        currentBadgeLabel.setText("Active " + (snapshot.currentPlayerNickname() == null
                ? "-"
                : snapshot.currentPlayerNickname()));
        picksBadgeLabel.setText("Picks " + snapshot.remainingTopChoices() + "/" + snapshot.remainingBottomChoices());
        turnLabel.setText(buildTurnLine(snapshot));
        actionHintLabel.setText(updateMessage == null || updateMessage.isBlank()
                ? buildActionHint(snapshot)
                : updateMessage);
        boolean showPass = canUsePass();
        passButton.setVisible(showPass);
        passButton.setManaged(showPass);

        rebuildMarketRows();
        rebuildOfferTiles();
        rebuildRanking();
        rebuildMinimalTurnBar();
        rebuildTotemWrapper();
        rebuildOwnedCardsDebugBox();
    }

    private void rebuildMinimalTurnBar() {
        VBox refreshedBar = buildMinimalTurnOrderBar();
        minimalTurnBarPane.getChildren().setAll(refreshedBar.getChildren());
    }

    private void rebuildTotemWrapper() {
        totemWrapper.getChildren().clear();
        if (snapshot != null && snapshot.currentPlayerColor() != null) {
            Node playerTotem = buildTotem(snapshot.currentPlayerColor(), 36);
            totemWrapper.getChildren().add(playerTotem);
        }
    }

    private void buildLayout() {
        getStyleClass().add("board-root");
        // keep the main shell padding minimal; card vertical offset is applied to the tabletop only
        setPadding(new Insets(0, 20, 20, 20));
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

        // Player totem wrapper (left side, fixed identifier)
        totemWrapper.setAlignment(Pos.TOP_CENTER);
        totemWrapper.setPrefWidth(50);
        totemWrapper.setMinWidth(50);
        totemWrapper.setMaxWidth(50);
        
        HBox bottomInfoBar = new HBox(8);
        bottomInfoBar.setAlignment(Pos.BOTTOM_LEFT);
        bottomInfoBar.setPadding(new Insets(12, 0, 8, 8));
        bottomInfoBar.setStyle("-fx-border-color: transparent;");
        bottomInfoBar.getChildren().addAll(totemWrapper, buildCompactInfoPanel(), ownedCardsDebugBox);
        VBox.setVgrow(bottomInfoBar, Priority.ALWAYS);

        VBox shell = new VBox(tableContent, bottomInfoBar);
        shell.getStyleClass().add("tabletop-shell");
        shell.setAlignment(Pos.TOP_CENTER);
        shell.setPrefWidth(DESIGN_WIDTH);
        shell.setMinWidth(DESIGN_WIDTH);
        return shell;
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
        VBox titleSection = new VBox(2, titleLabel, subtitleLabel);
        titleSection.setMaxWidth(650);

        FlowPane metaWrap = new FlowPane(4, 4,
                roundBadgeLabel,
                eraBadgeLabel,
                phaseBadgeLabel,
                currentBadgeLabel,
                picksBadgeLabel
        );
        metaWrap.getStyleClass().add("turn-meta-wrap");
        metaWrap.setAlignment(Pos.CENTER_LEFT);
        metaWrap.setMaxWidth(600);

        VBox statsSection = new VBox(6, metaWrap);
        statsSection.setMaxWidth(650);

        VBox infoPanel = new VBox(12, titleSection, statsSection, turnLabel, actionHintLabel, actionRow);
        infoPanel.getStyleClass().add("info-panel");
        infoPanel.setAlignment(Pos.TOP_LEFT);
        infoPanel.setMaxWidth(650);
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
        Map<String, Integer> characterCounts = new LinkedHashMap<>();
        for (CardSummary card : player.characterCards()) {
            characterCounts.merge(card.type(), 1, Integer::sum);
        }

        Map<String, Integer> buildingCounts = new LinkedHashMap<>();
        for (CardSummary card : player.buildingCards()) {
            buildingCounts.merge(card.type(), 1, Integer::sum);
        }

        Label title = new Label("Owned cards");
        title.getStyleClass().add("subsection-title");
        ownedCardsDebugBox.getChildren().add(title);

        Label resources = new Label("Food: " + player.food() + " | Prestige: " + player.prestigePoints());
        resources.getStyleClass().add("owned-cards-debug-row");
        ownedCardsDebugBox.getChildren().add(resources);

        Label characterHeader = new Label("Characters: " + player.characterCount());
        characterHeader.getStyleClass().add("owned-cards-debug-title");
        ownedCardsDebugBox.getChildren().add(characterHeader);
        if (characterCounts.isEmpty()) {
            Label emptyCharacters = new Label("None");
            emptyCharacters.getStyleClass().add("owned-cards-debug-row");
            ownedCardsDebugBox.getChildren().add(emptyCharacters);
        } else {
            characterCounts.forEach((type, count) -> {
                Label row = new Label(count + " x " + type);
                row.getStyleClass().add("owned-cards-debug-row");
                row.setWrapText(true);
                ownedCardsDebugBox.getChildren().add(row);
            });
        }

        Label buildingHeader = new Label("Buildings: " + player.buildingCount());
        buildingHeader.getStyleClass().add("owned-cards-debug-title");
        ownedCardsDebugBox.getChildren().add(buildingHeader);
        if (buildingCounts.isEmpty()) {
            Label emptyBuildings = new Label("None");
            emptyBuildings.getStyleClass().add("owned-cards-debug-row");
            ownedCardsDebugBox.getChildren().add(emptyBuildings);
        } else {
            buildingCounts.forEach((type, count) -> {
                Label row = new Label(count + " x " + type);
                row.getStyleClass().add("owned-cards-debug-row");
                row.setWrapText(true);
                ownedCardsDebugBox.getChildren().add(row);
            });
        }

        ownedCardsDebugBox.setVisible(true);
        ownedCardsDebugBox.setManaged(true);
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

    private String buildTurnLine(GameSnapshot snapshot) {
        if (snapshot.status() == GameStatus.LOGIN) {
            return "Waiting for " + Math.max(0, snapshot.playerCount() - snapshot.joinedPlayers()) + " more player(s).";
        }
        if (snapshot.currentPlayerNickname() == null) {
            return "Observe the shared board state while the match advances.";
        }
        return "Current player: " + snapshot.currentPlayerNickname()
                + " | Remaining picks top/bottom: "
                + snapshot.remainingTopChoices() + "/" + snapshot.remainingBottomChoices();
    }

    private String buildActionHint(GameSnapshot snapshot) {
        if (snapshot.finished()) {
            return "The match has ended. Review the ranking and the final board layout.";
        }
        if (!isLocalTurn()) {
            return snapshot.currentPlayerNickname() == null
                    ? "Waiting for the server to advance the flow."
                    : "It is " + snapshot.currentPlayerNickname() + "'s turn.";
        }
        return switch (snapshot.status()) {
            case ROUND_OFFER -> "Select one available offer tile from the central strip.";
            case ROUND_ACTIONS -> snapshot.passAvailable()
                    ? "Choose one top-row card or use Pass to skip the optional extra pick."
                    : "Choose a card from the upper or lower row, depending on your remaining picks.";
            case ROUND_EVENTS, ROUND_END, FINAL_EVENTS -> "The server is resolving automatic effects. Watch the board update.";
            case LOGIN -> "Waiting for the lobby to fill before the match starts.";
            default -> "Observe the board or use Refresh if you need a fresh snapshot.";
        };
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
