package it.polimi.ingsw.mesos.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.controller.enums.GameStatus;
import it.polimi.ingsw.mesos.database.LeaderboardEntry;
import it.polimi.ingsw.mesos.dto.event.EventLogEntry;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.dto.game.CardSummary;
import it.polimi.ingsw.mesos.dto.game.GameSnapshot;
import it.polimi.ingsw.mesos.dto.game.PlayerSummary;
import it.polimi.ingsw.mesos.dto.game.TurnOrderSpaceSummary;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class MesosGameBoardViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS));
    }

    @Test
    void bottomInfoBarStartsWithPanelsAndShowsLocalPlayerColorIndicator() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshot());

            StackPane bottomBar = singleNode(view, ".bottom-hud-overlay", StackPane.class);
            assertEquals(0.0, view.getPadding().getLeft());
            assertTrue(bottomBar.getParent().getStyleClass().contains("board-viewport"));
            assertEquals(Pos.BOTTOM_CENTER, StackPane.getAlignment(bottomBar));
            VBox leftColumn = singleNode(view, ".left-hud-column", VBox.class);
            assertTrue(leftColumn.getStyleClass().contains("left-hud-column"));
            assertTrue(leftColumn.getChildren().get(0).getStyleClass().contains("info-panel"));
            assertTrue(leftColumn.getChildren().get(1).getStyleClass().contains("tribe-stats-box"));
            assertEquals(Pos.BOTTOM_LEFT, StackPane.getAlignment(leftColumn));
            assertEquals(Pos.BOTTOM_RIGHT, StackPane.getAlignment(singleNode(view, ".owned-tribe-decks-strip", HBox.class)));
            assertEquals(Region.USE_PREF_SIZE, bottomBar.getMaxHeight());
            assertEquals(Region.USE_PREF_SIZE, leftColumn.getMaxHeight());
            assertEquals(Region.USE_PREF_SIZE, ((Region) leftColumn.getChildren().get(0)).getMaxHeight());
            assertEquals(Region.USE_PREF_SIZE, ((Region) leftColumn.getChildren().get(1)).getMaxHeight());
            assertEquals(1, view.lookupAll(".info-panel-color-indicator").size());
            assertTrue(hasLabelText(leftColumn.getChildren().get(0), "Round 1/10"));
            assertTrue(hasLabelText(leftColumn.getChildren().get(0), "Era: I"));
            return null;
        });
    }

    @Test
    void ownedCardsUseStableDeckSlotsAndCenteredHoverPreview() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshot());

            HBox decksStrip = singleNode(view, ".owned-tribe-decks-strip", HBox.class);
            StackPane previewOverlay = singleNode(view, ".owned-card-preview-overlay", StackPane.class);
            assertEquals(7, decksStrip.getChildren().size());
            assertEquals(7, view.lookupAll(".owned-card-deck-slot").size());
            assertEquals(4, view.lookupAll(".owned-card-deck-empty").size());
            assertEquals(4, view.lookupAll(".owned-card-deck-card").size());
            assertEquals(0, view.lookupAll(".owned-tribe-deck-popup").size());

            List<Node> originalOrder = new ArrayList<>(decksStrip.getChildren());
            Node hunterDeck = originalOrder.getFirst();
            hunterDeck.getOnMouseEntered().handle(null);

            assertEquals(originalOrder, decksStrip.getChildren());
            assertTrue(previewOverlay.isVisible());
            assertEquals(Pos.CENTER, StackPane.getAlignment(previewOverlay));
            assertEquals(2, previewOverlay.lookupAll(".owned-card-preview-card").size());
            Set<Node> previewGridNodes = previewOverlay.lookupAll(".owned-card-preview-grid");
            assertEquals(1, previewGridNodes.size());
            FlowPane previewGrid = (FlowPane) previewGridNodes.iterator().next();
            assertTrue(previewGrid.scaleXProperty().isBound());
            assertTrue(previewGrid.scaleYProperty().isBound());
            assertEquals(2.25, previewGrid.getScaleX());
            assertEquals(2.25, previewGrid.getScaleY());
            assertEquals(0, previewOverlay.lookupAll(".owned-card-preview-panel").size());

            hunterDeck.getOnMouseExited().handle(null);

            assertFalse(previewOverlay.isVisible());
            assertEquals(originalOrder, decksStrip.getChildren());
            return null;
        });
    }

    @Test
    void playerButtonsDisableLocalPlayerAndOpenOtherTribePopup() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshotWithOpponentTribe());

            HBox buttonsPane = singleNode(view, ".player-buttons-pane", HBox.class);
            assertEquals(Pos.TOP_LEFT, StackPane.getAlignment(buttonsPane));
            assertEquals(2, view.lookupAll(".player-summary-button").size());
            for (Node buttonNode : buttonsPane.getChildren()) {
                Button button = (Button) buttonNode;
                assertTrue(button.getGraphic().getStyleClass().contains("player-summary-color-indicator"));
            }

            Button localPlayerButton = (Button) buttonsPane.getChildren().getFirst();
            assertTrue(localPlayerButton.isDisabled());
            StackPane popupOverlay = singleNode(view, ".player-tribe-overlay", StackPane.class);
            localPlayerButton.fire();
            assertFalse(popupOverlay.isVisible());

            Button otherPlayerButton = (Button) buttonsPane.getChildren().get(1);
            otherPlayerButton.fire();
            view.applyCss();
            view.layout();

            assertTrue(popupOverlay.isVisible());
            assertEquals(Pos.CENTER, StackPane.getAlignment(popupOverlay));
            assertEquals(1, popupOverlay.lookupAll(".player-tribe-popup").size());
            assertEquals(2, popupOverlay.lookupAll(".player-tribe-resource").size());
            ScrollPane tribeScroll = singleNode(popupOverlay, ".player-tribe-scroll", ScrollPane.class);
            assertEquals(2, tribeScroll.getContent().lookupAll(".player-tribe-card").size());
            return null;
        });
    }

    @Test
    void playerButtonsDoNotBlockTopLeftMarketCards() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(snapshotWithManyRows(10, 0));
            view.applyCss();
            view.layout();

            HBox buttonsPane = singleNode(view, ".player-buttons-pane", HBox.class);
            HBox topCards = singleNode(singleNode(view, ".table-row-top", HBox.class),
                    ".table-row-cards", HBox.class);
            Node firstTopCard = topCards.getChildren().getFirst();
            Bounds buttonsBounds = buttonsPane.localToScene(buttonsPane.getBoundsInLocal());
            Bounds cardBounds = firstTopCard.localToScene(firstTopCard.getBoundsInLocal());

            assertEquals(Region.USE_PREF_SIZE, buttonsPane.getMaxHeight());
            assertFalse(buttonsPane.isPickOnBounds());
            assertFalse(buttonsBounds.intersects(cardBounds),
                    "player buttons " + buttonsBounds + " overlap top-left card " + cardBounds);
            return null;
        });
    }

    @Test
    void infoButtonOpensTopRightPopupWithTwoCardsAndClosesOnOutsideClick() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(snapshot());
            view.applyCss();
            view.layout();

            Button infoButton = singleNode(view, ".info-popup-button", Button.class);
            // the Instruction Cards button shares a top-right bar with the Event Log button,
            // sitting to its right
            HBox topRightBar = (HBox) infoButton.getParent();
            assertEquals(Pos.TOP_RIGHT, StackPane.getAlignment(topRightBar));
            assertSame(infoButton, topRightBar.getChildren().get(topRightBar.getChildren().size() - 1));

            infoButton.getOnAction().handle(null);
            view.applyCss();
            view.layout();

            StackPane overlay = singleNode(view, ".info-popup-overlay", StackPane.class);
            HBox popup = singleNode(overlay, ".info-popup-panel", HBox.class);
            assertTrue(overlay.isVisible());
            assertEquals(2, overlay.lookupAll(".info-popup-card").size());
            Bounds overlayBounds = overlay.localToScene(overlay.getBoundsInLocal());
            Bounds popupBounds = popup.localToScene(popup.getBoundsInLocal());
            assertTrue(popupBounds.getWidth() < overlayBounds.getWidth());
            assertTrue(popupBounds.getHeight() < overlayBounds.getHeight());

            overlay.getOnMouseClicked().handle(null);

            assertFalse(overlay.isVisible());
            assertTrue(overlay.getChildren().isEmpty());
            return null;
        }, 15);
    }

    @Test
    void eventLogPopupIsExclusiveAndClosesOnOutsideClick() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(snapshotWithOpponentTribe());
            view.setEventLog(List.of(sampleEventEntry()));
            view.applyCss();
            view.layout();

            Button eventLogButton = (Button) singleNode(view, ".event-log-pane", VBox.class).getChildren().getFirst();
            eventLogButton.fire();

            StackPane eventLogOverlay = singleNode(view, ".event-log-overlay", StackPane.class);
            assertTrue(eventLogOverlay.isVisible());

            Button infoButton = singleNode(view, ".info-popup-button", Button.class);
            infoButton.fire();

            StackPane infoOverlay = singleNode(view, ".info-popup-overlay", StackPane.class);
            assertFalse(eventLogOverlay.isVisible());
            assertTrue(infoOverlay.isVisible());

            infoOverlay.getOnMouseClicked().handle(null);
            eventLogButton.fire();

            assertTrue(eventLogOverlay.isVisible());
            assertNotNull(eventLogOverlay.getOnMouseClicked());
            eventLogOverlay.getOnMouseClicked().handle(null);
            assertFalse(eventLogOverlay.isVisible());
            assertTrue(eventLogOverlay.getChildren().isEmpty());
            return null;
        });
    }

    @Test
    void globalLeaderboardUsesCssClassesInsteadOfInlineStyles() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(finishedSnapshot());
            view.updateGlobalLeaderboard(List.of(
                    new LeaderboardEntry(
                            1,
                            "dio",
                            12,
                            Timestamp.valueOf("2026-01-02 03:04:05"),
                            2
                    )
            ));
            view.applyCss();
            view.layout();

            ScrollPane scrollPane = singleNode(view, ".transparent-scroll", ScrollPane.class);
            assertTrue(scrollPane.getStyle().isBlank());

            Label dateLabel = singleNode(view, ".leaderboard-date-label", Label.class);
            assertEquals("02/01/2026 03:04", dateLabel.getText());
            assertTrue(dateLabel.getStyle().isBlank());
            return null;
        });
    }

    @Test
    void largeOwnedCardDeckPreviewWrapsFivePerRowAndScalesDown() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            List<CardSummary> manyHunters = new ArrayList<>();
            for (int index = 0; index < 15; index++) {
                manyHunters.add(tribeCard(index, "Hunter", "hunter_era_1_a"));
            }

            view.updateSnapshot(snapshotWithOwnedCharacterCards(manyHunters));

            Node hunterDeck = singleNode(view, ".owned-tribe-decks-strip", HBox.class).getChildren().getFirst();
            hunterDeck.getOnMouseEntered().handle(null);
            FlowPane previewGrid = singleNode(view, ".owned-card-preview-grid", FlowPane.class);

            assertEquals(15, previewGrid.lookupAll(".owned-card-preview-card").size());
            assertEquals(465.0, previewGrid.getMaxWidth());
            assertTrue(previewGrid.getScaleX() < 1.0);
            assertTrue(previewGrid.getScaleY() < 1.0);
            return null;
        });
    }

    @Test
    void ownedDeckStacksShowAtMostThreeCardsEvenWithLargeDecks() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            List<CardSummary> manyHunters = new ArrayList<>();
            for (int index = 0; index < 10; index++) {
                manyHunters.add(tribeCard(index, "Hunter", "hunter_era_1_a"));
            }

            view.updateSnapshot(snapshotWithOwnedCharacterCards(manyHunters));

            Node hunterDeck = singleNode(view, ".owned-tribe-decks-strip", HBox.class).getChildren().getFirst();

            assertEquals(3, hunterDeck.lookupAll(".owned-card-deck-card").size());
            assertEquals(1, hunterDeck.lookupAll(".owned-tribe-deck-count").size());
            return null;
        });
    }

    @Test
    void bottomHudDoesNotCoverBottomRowCards() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(snapshotWithRows(0, 1));
            view.applyCss();
            view.layout();

            Node bottomCard = singleNode(singleNode(view, ".table-row-bottom", HBox.class), ".interactive-card", Node.class);
            Bounds bottomCardBounds = bottomCard.localToScene(bottomCard.getBoundsInLocal());
            Bounds leftHudBounds = singleNode(view, ".left-hud-column", VBox.class)
                    .localToScene(singleNode(view, ".left-hud-column", VBox.class).getBoundsInLocal());
            Bounds deckStripBounds = singleNode(view, ".owned-tribe-decks-strip", HBox.class)
                    .localToScene(singleNode(view, ".owned-tribe-decks-strip", HBox.class).getBoundsInLocal());

            assertFalse(leftHudBounds.intersects(bottomCardBounds),
                    "left HUD " + leftHudBounds + " overlaps bottom card " + bottomCardBounds);
            assertFalse(deckStripBounds.intersects(bottomCardBounds),
                    "deck strip " + deckStripBounds + " overlaps bottom card " + bottomCardBounds);
            return null;
        });
    }

    @Test
    void leftHudPanelsStayWithinTheirColumnAndClearOfTurnOrder() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(snapshot());
            view.applyCss();
            view.layout();

            VBox leftColumn = singleNode(view, ".left-hud-column", VBox.class);
            Region statusPanel = (Region) leftColumn.getChildren().get(0);
            Region tribePanel = (Region) leftColumn.getChildren().get(1);
            Pane turnOrder = singleNode(view, ".graphic-turn-bar", Pane.class);
            Bounds statusBounds = statusPanel.localToScene(statusPanel.getBoundsInLocal());
            Bounds tribeBounds = tribePanel.localToScene(tribePanel.getBoundsInLocal());
            Bounds turnOrderBounds = turnOrder.localToScene(turnOrder.getBoundsInLocal());

            assertTrue(statusPanel.getMaxWidth() <= leftColumn.getPrefWidth());
            assertTrue(tribePanel.getMaxWidth() <= leftColumn.getPrefWidth());
            assertFalse(statusBounds.intersects(turnOrderBounds),
                    "status panel " + statusBounds + " overlaps turn order " + turnOrderBounds);
            assertFalse(tribeBounds.intersects(turnOrderBounds),
                    "tribe panel " + tribeBounds + " overlaps turn order " + turnOrderBounds);
            return null;
        });
    }

    @Test
    void leftHudClearsTurnOrderAtScreenshotViewport() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1470, 850);
            view.resize(1470, 850);

            view.updateSnapshot(snapshot());
            view.applyCss();
            view.layout();

            VBox leftColumn = singleNode(view, ".left-hud-column", VBox.class);
            Pane turnOrder = singleNode(view, ".graphic-turn-bar", Pane.class);
            Bounds leftHudBounds = leftColumn.localToScene(leftColumn.getBoundsInLocal());
            Bounds turnOrderBounds = turnOrder.localToScene(turnOrder.getBoundsInLocal());

            assertTrue(leftHudBounds.getHeight() <= 250,
                    "left HUD should stay compact but was " + leftHudBounds.getHeight() + " px tall");
            assertFalse(leftHudBounds.intersects(turnOrderBounds),
                    "left HUD " + leftHudBounds + " overlaps turn order " + turnOrderBounds);
            return null;
        });
    }

    @Test
    void marketRowsScaleDownWhenManyCardsWouldReachHudPanels() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(snapshotWithManyRows(10, 10));
            view.applyCss();
            view.layout();

            HBox topCards = singleNode(singleNode(view, ".table-row-top", HBox.class), ".table-row-cards", HBox.class);
            HBox bottomCards = singleNode(singleNode(view, ".table-row-bottom", HBox.class), ".table-row-cards", HBox.class);
            Bounds bottomBounds = bottomCards.localToScene(bottomCards.getBoundsInLocal());
            Bounds leftHudBounds = singleNode(view, ".left-hud-column", VBox.class)
                    .localToScene(singleNode(view, ".left-hud-column", VBox.class).getBoundsInLocal());
            Bounds deckStripBounds = singleNode(view, ".owned-tribe-decks-strip", HBox.class)
                    .localToScene(singleNode(view, ".owned-tribe-decks-strip", HBox.class).getBoundsInLocal());

            assertTrue(topCards.getScaleX() < 1.0);
            assertTrue(bottomCards.getScaleX() < topCards.getScaleX());
            assertFalse(leftHudBounds.intersects(bottomBounds),
                    "left HUD " + leftHudBounds + " overlaps bottom row " + bottomBounds);
            assertFalse(deckStripBounds.intersects(bottomBounds),
                    "deck strip " + deckStripBounds + " overlaps bottom row " + bottomBounds);
            return null;
        });
    }

    @Test
    void finishedGameShowsCenteredLeaderboardsAndLobbyButton() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger lobbyClicks = new AtomicInteger();
            MesosGameBoardView view = buildView((ignoredIndex, ignoredTopRow) -> { }, lobbyClicks::incrementAndGet);
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(finishedSnapshot());
            view.applyCss();
            view.layout();

            StackPane finalOverlay = singleNode(view, ".final-leaderboard-overlay", StackPane.class);
            assertTrue(finalOverlay.isVisible());
            assertEquals(Pos.CENTER, StackPane.getAlignment(finalOverlay));
            assertFalse(singleNode(view, ".game-canvas", VBox.class).isVisible());
            assertFalse(singleNode(view, ".bottom-hud-overlay", StackPane.class).isVisible());
            assertFalse(singleNode(view, ".player-buttons-pane", HBox.class).isVisible());
            assertFalse(singleNode(view, ".top-right-bar", HBox.class).isVisible());
            assertEquals(2, finalOverlay.lookupAll(".final-leaderboard-row").size());
            assertEquals(2, finalOverlay.lookupAll(".final-leaderboard-player-name").size());
            List<VBox> leaderboardBoxes = finalOverlay.lookupAll(".final-leaderboard-box").stream()
                    .filter(VBox.class::isInstance)
                    .map(VBox.class::cast)
                    .toList();
            assertEquals(2, leaderboardBoxes.size());
            VBox leaderboardBox = leaderboardBoxes.stream()
                    .filter(box -> box.lookupAll(".final-leaderboard-row").size() == 2)
                    .findFirst()
                    .orElseThrow();
            Bounds overlayBounds = finalOverlay.localToScene(finalOverlay.getBoundsInLocal());
            Bounds leaderboardBounds = leaderboardBox.localToScene(leaderboardBox.getBoundsInLocal());
            assertTrue(leaderboardBounds.getHeight() < overlayBounds.getHeight() * 0.45);

            Button lobbyButton = singleNode(finalOverlay, ".final-leaderboard-lobby-button", Button.class);
            assertEquals(Pos.TOP_RIGHT, StackPane.getAlignment(lobbyButton));
            lobbyButton.fire();

            assertEquals(1, lobbyClicks.get());
            return null;
        });
    }

    @Test
    void finishedGameHighlightsEveryWinnerRow() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();
            new Scene(view, 1920, 1080);
            view.resize(1920, 1080);

            view.updateSnapshot(sharedWinnersSnapshot());
            view.applyCss();
            view.layout();

            StackPane finalOverlay = singleNode(view, ".final-leaderboard-overlay", StackPane.class);
            assertEquals(2, finalOverlay.lookupAll(".final-leaderboard-row").size());
            assertEquals(2, finalOverlay.lookupAll(".final-leaderboard-row-winner").size());
            return null;
        });
    }

    @Test
    void turnOrderEntriesShowColorIndicatorOnlyWhenOccupied() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshot());

            Pane turnBar = singleNode(view, ".graphic-turn-bar", Pane.class);
            long occupiedRowWrappers = turnBar.getChildren().stream()
                    .filter(StackPane.class::isInstance)
                    .count();
            assertEquals(1, occupiedRowWrappers);
            return null;
        });
    }

    @Test
    void marketCardsDoNotUseRoundedClipForRenderedArtwork() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshotWithRows(1, 0));

            HBox topRow = singleNode(view, ".table-row-top", HBox.class);
            StackPane marketCard = singleNode(topRow, ".market-card", StackPane.class);
            assertNull(marketCard.getClip());
            return null;
        });
    }

    @Test
    void refreshedSnapshotKeepsRemainingCardPickClickableAndClearsDeckPreview() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger pickedCards = new AtomicInteger();
            MesosGameBoardView view = buildView((ignoredIndex, ignoredTopRow) -> pickedCards.incrementAndGet());

            view.updateSnapshot(snapshotWithRows(0, 2));
            StackPane previewOverlay = singleNode(view, ".owned-card-preview-overlay", StackPane.class);
            Node hunterDeck = singleNode(view, ".owned-tribe-decks-strip", HBox.class).getChildren().getFirst();
            hunterDeck.getOnMouseEntered().handle(null);
            assertTrue(previewOverlay.isVisible());

            view.updateSnapshot(snapshotWithRows(0, 1));

            assertFalse(previewOverlay.isVisible());
            Node remainingBottomCard = view.lookupAll(".interactive-card").iterator().next();
            remainingBottomCard.getOnMouseClicked().handle(null);

            assertEquals(1, pickedCards.get());
            return null;
        });
    }

    @Test
    void cardClicksAreLockedUntilNextSnapshotArrives() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger pickedCards = new AtomicInteger();
            MesosGameBoardView view = buildView((ignoredIndex, ignoredTopRow) -> pickedCards.incrementAndGet());

            view.updateSnapshot(snapshotWithRows(2, 0));
            Node topCard = singleNode(singleNode(view, ".table-row-top", HBox.class), ".interactive-card", Node.class);

            topCard.getOnMouseClicked().handle(null);
            topCard.getOnMouseClicked().handle(null);

            assertEquals(1, pickedCards.get());
            view.updateSnapshot(snapshotWithRows(1, 0));
            Node refreshedTopCard = singleNode(singleNode(view, ".table-row-top", HBox.class), ".interactive-card", Node.class);
            refreshedTopCard.getOnMouseClicked().handle(null);

            assertEquals(2, pickedCards.get());
            return null;
        });
    }

    @Test
    void optionalExtraChoiceShowsPassButton() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshotWithPassAvailable());

            VBox leftColumn = singleNode(view, ".left-hud-column", VBox.class);
            assertTrue(hasVisibleButtonText(leftColumn.getChildren().get(0), "Pass"));
            return null;
        });
    }

    private static MesosGameBoardView buildView() {
        return buildView((ignoredIndex, ignoredTopRow) -> { });
    }

    private static MesosGameBoardView buildView(java.util.function.BiConsumer<Integer, Boolean> onChooseCard) {
        return buildView(onChooseCard, () -> { });
    }

    private static MesosGameBoardView buildView(
            java.util.function.BiConsumer<Integer, Boolean> onChooseCard,
            Runnable onBackToLobby
    ) {
        return new MesosGameBoardView(
                "dio",
                new MesosAssetResolver(),
                () -> { },
                () -> { },
                ignored -> { },
                onChooseCard,
                () -> { },
                onBackToLobby
        );
    }

    private static GameSnapshot snapshot() {
        return snapshot(List.of(), List.of(), 1, 0);
    }

    private static GameSnapshot snapshotWithOwnedCharacterCards(List<CardSummary> characterCards) {
        return snapshot(List.of(), List.of(), 1, 0, characterCards, List.of());
    }

    private static GameSnapshot snapshotWithOpponentTribe() {
        return snapshot(
                List.of(),
                List.of(),
                1,
                0,
                List.of(
                        tribeCard(0, "Hunter", "hunter_era_1_a"),
                        tribeCard(1, "Hunter", "hunter_era_1_b"),
                        tribeCard(2, "Shaman", "shaman_era_1_a")
                ),
                List.of(
                        tribeCard(3, "Building", "building_era_1_01")
                ),
                List.of(
                        tribeCard(4, "Gatherer", "gatherer_era_1_a")
                ),
                List.of(
                        tribeCard(5, "Building", "building_era_1_01")
                )
        );
    }

    private static GameSnapshot snapshotWithRows(int remainingTopChoices, int remainingBottomChoices) {
        return snapshot(
                List.of(
                        tribeCard(10, "Hunter", "hunter_era_1_a")
                ),
                List.of(
                        tribeCard(20, "Inventor", "inventor_era_1_a")
                ),
                remainingTopChoices,
                remainingBottomChoices
        );
    }

    private static GameSnapshot snapshotWithPassAvailable() {
        return snapshot(
                List.of(
                        tribeCard(10, "Hunter", "hunter_era_1_a")
                ),
                List.of(),
                1,
                0,
                List.of(
                        tribeCard(0, "Hunter", "hunter_era_1_a")
                ),
                List.of(),
                List.of(),
                List.of(),
                GameStatus.ROUND_ACTIONS,
                false,
                true
        );
    }

    private static GameSnapshot snapshotWithManyRows(int topCount, int bottomCount) {
        return snapshot(
                tribeCards(topCount, 100, "Hunter", "hunter_era_1_a"),
                tribeCards(bottomCount, 200, "Inventor", "inventor_era_1_a"),
                1,
                1
        );
    }

    private static GameSnapshot finishedSnapshot() {
        return snapshot(
                List.of(),
                List.of(),
                0,
                0,
                List.of(tribeCard(0, "Hunter", "hunter_era_1_a")),
                List.of(),
                List.of(tribeCard(1, "Gatherer", "gatherer_era_1_a")),
                List.of(),
                GameStatus.END_GAME,
                true
        );
    }

    private static GameSnapshot snapshot(
            List<CardSummary> topRow,
            List<CardSummary> bottomRow,
            int remainingTopChoices,
            int remainingBottomChoices
    ) {
        return snapshot(
                topRow,
                bottomRow,
                remainingTopChoices,
                remainingBottomChoices,
                List.of(
                        tribeCard(0, "Hunter", "hunter_era_1_a"),
                        tribeCard(1, "Hunter", "hunter_era_1_b"),
                        tribeCard(2, "Shaman", "shaman_era_1_a")
                ),
                List.of(
                        tribeCard(3, "Building", "building_era_1_01")
                )
        );
    }

    private static GameSnapshot snapshot(
            List<CardSummary> topRow,
            List<CardSummary> bottomRow,
            int remainingTopChoices,
            int remainingBottomChoices,
            List<CardSummary> characterCards,
            List<CardSummary> buildingCards
    ) {
        return snapshot(topRow, bottomRow, remainingTopChoices, remainingBottomChoices, characterCards, buildingCards,
                List.of(), List.of(), GameStatus.ROUND_ACTIONS, false, false);
    }

    private static GameSnapshot snapshot(
            List<CardSummary> topRow,
            List<CardSummary> bottomRow,
            int remainingTopChoices,
            int remainingBottomChoices,
            List<CardSummary> characterCards,
            List<CardSummary> buildingCards,
            List<CardSummary> opponentCharacterCards,
            List<CardSummary> opponentBuildingCards
    ) {
        return snapshot(
                topRow,
                bottomRow,
                remainingTopChoices,
                remainingBottomChoices,
                characterCards,
                buildingCards,
                opponentCharacterCards,
                opponentBuildingCards,
                GameStatus.ROUND_ACTIONS,
                false,
                false
        );
    }

    private static GameSnapshot snapshot(
            List<CardSummary> topRow,
            List<CardSummary> bottomRow,
            int remainingTopChoices,
            int remainingBottomChoices,
            List<CardSummary> characterCards,
            List<CardSummary> buildingCards,
            List<CardSummary> opponentCharacterCards,
            List<CardSummary> opponentBuildingCards,
            GameStatus status,
            boolean finished
    ) {
        return snapshot(
                topRow,
                bottomRow,
                remainingTopChoices,
                remainingBottomChoices,
                characterCards,
                buildingCards,
                opponentCharacterCards,
                opponentBuildingCards,
                status,
                finished,
                false
        );
    }

    private static GameSnapshot sharedWinnersSnapshot() {
        List<PlayerSummary> players = List.of(
                new PlayerSummary("dio", Color.ORANGE, 5, 10, 0, 0,
                        List.of(), List.of(), 0, 0, 0, 0, 0, 0, true),
                new PlayerSummary("Carlo2", Color.BLUE, 5, 10, 0, 0,
                        List.of(), List.of(), 0, 0, 0, 0, 0, 0, true)
        );
        return new GameSnapshot(
                "dio", 2, 2, true, true, GameStatus.END_GAME, 1, Era.FIRST,
                "dio", Color.ORANGE, 0, 0, List.of(Color.ORANGE, Color.BLUE),
                players, List.of(), List.of(), List.of(), List.of(), false
        );
    }

    private static GameSnapshot snapshot(
            List<CardSummary> topRow,
            List<CardSummary> bottomRow,
            int remainingTopChoices,
            int remainingBottomChoices,
            List<CardSummary> characterCards,
            List<CardSummary> buildingCards,
            List<CardSummary> opponentCharacterCards,
            List<CardSummary> opponentBuildingCards,
            GameStatus status,
            boolean finished,
            boolean passAvailable
    ) {
        List<PlayerSummary> players = List.of(
                new PlayerSummary(
                        "dio",
                        Color.ORANGE,
                        3,
                        0,
                        characterCards.size(),
                        buildingCards.size(),
                        characterCards,
                        buildingCards
                ),
                new PlayerSummary(
                        "Carlo2",
                        Color.BLUE,
                        1,
                        2,
                        opponentCharacterCards.size(),
                        opponentBuildingCards.size(),
                        opponentCharacterCards,
                        opponentBuildingCards
                )
        );
        List<TurnOrderSpaceSummary> turnOrder = List.of(
                new TurnOrderSpaceSummary(0, 0, Color.ORANGE, "dio"),
                new TurnOrderSpaceSummary(1, 0, null, null)
        );
        return new GameSnapshot(
                "dio",
                2,
                2,
                true,
                finished,
                status,
                1,
                Era.FIRST,
                "dio",
                Color.ORANGE,
                remainingTopChoices,
                remainingBottomChoices,
                List.of(Color.ORANGE, Color.BLUE),
                players,
                List.of(),
                turnOrder,
                topRow,
                bottomRow,
                passAvailable
        );
    }

    private static CardSummary tribeCard(int selectionIndex, String type, String presentationId) {
        return new CardSummary(selectionIndex, type, Era.FIRST, false, type, null, null, presentationId);
    }

    private static EventLogEntry sampleEventEntry() {
        return new EventLogEntry.Notice("Hunt", "A hunt resolved.");
    }

    private static List<CardSummary> tribeCards(int count, int startIndex, String type, String presentationId) {
        List<CardSummary> cards = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            cards.add(tribeCard(startIndex + index, type, presentationId));
        }
        return cards;
    }

    private static <T> T singleNode(MesosGameBoardView view, String selector, Class<T> expectedType) {
        return singleNode((Node) view, selector, expectedType);
    }

    private static <T> T singleNode(Node root, String selector, Class<T> expectedType) {
        Set<Node> matches = root.lookupAll(selector);
        assertEquals(1, matches.size());
        return expectedType.cast(matches.iterator().next());
    }

    private static boolean hasLabelText(Node root, String text) {
        return root.lookupAll(".label").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(label -> text.equals(label.getText()));
    }

    private static boolean hasVisibleButtonText(Node root, String text) {
        return root.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .anyMatch(button -> button.isVisible() && text.equals(button.getText()));
    }

    private static <T> T runOnFxThread(Callable<T> callable) throws Exception {
        return runOnFxThread(callable, 5);
    }

    private static <T> T runOnFxThread(Callable<T> callable, int timeoutSeconds) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(timeoutSeconds, TimeUnit.SECONDS);
    }
}
