package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.CardSummary;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import it.polimi.ingsw.mesos.Network.TurnOrderSpaceSummary;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

            view.updateSnapshot(snapshot(), "Offer tile selected.");

            BorderPane bottomBar = singleNode(view, ".bottom-hud-overlay", BorderPane.class);
            assertEquals(0.0, view.getPadding().getLeft());
            assertTrue(bottomBar.getParent().getStyleClass().contains("board-viewport"));
            assertEquals(Pos.BOTTOM_CENTER, StackPane.getAlignment(bottomBar));
            VBox leftColumn = (VBox) bottomBar.getLeft();
            assertTrue(leftColumn.getStyleClass().contains("left-hud-column"));
            assertTrue(leftColumn.getChildren().get(0).getStyleClass().contains("info-panel"));
            assertTrue(leftColumn.getChildren().get(1).getStyleClass().contains("owned-cards-debug-box"));
            assertNull(bottomBar.getCenter());
            assertTrue(bottomBar.getRight().getStyleClass().contains("owned-tribe-decks-strip"));
            assertEquals(Region.USE_PREF_SIZE, bottomBar.getMaxHeight());
            assertEquals(Region.USE_PREF_SIZE, leftColumn.getMaxHeight());
            assertEquals(Region.USE_PREF_SIZE, ((Region) leftColumn.getChildren().get(0)).getMaxHeight());
            assertEquals(Region.USE_PREF_SIZE, ((Region) leftColumn.getChildren().get(1)).getMaxHeight());
            assertEquals(1, view.lookupAll(".info-panel-color-indicator").size());
            return null;
        });
    }

    @Test
    void ownedCardsUseStableDeckSlotsAndCenteredHoverPreview() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshot(), "Offer tile selected.");

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
            assertEquals(2.5, previewGrid.getScaleX());
            assertEquals(2.5, previewGrid.getScaleY());
            assertEquals(0, previewOverlay.lookupAll(".owned-card-preview-panel").size());

            hunterDeck.getOnMouseExited().handle(null);

            assertFalse(previewOverlay.isVisible());
            assertEquals(originalOrder, decksStrip.getChildren());
            return null;
        });
    }

    @Test
    void turnOrderEntriesShowColorIndicatorOnlyWhenOccupied() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshot(), "Offer tile selected.");

            assertEquals(1, view.lookupAll(".turn-order-color-indicator").size());
            assertEquals(2, view.lookupAll(".turn-order-tile-entry").size());
            return null;
        });
    }

    @Test
    void refreshedSnapshotKeepsRemainingCardPickClickableAndClearsDeckPreview() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger pickedCards = new AtomicInteger();
            MesosGameBoardView view = buildView((ignoredIndex, ignoredTopRow) -> pickedCards.incrementAndGet());

            view.updateSnapshot(snapshotWithRows(0, 2), "Offer tile selected.");
            StackPane previewOverlay = singleNode(view, ".owned-card-preview-overlay", StackPane.class);
            Node hunterDeck = singleNode(view, ".owned-tribe-decks-strip", HBox.class).getChildren().getFirst();
            hunterDeck.getOnMouseEntered().handle(null);
            assertTrue(previewOverlay.isVisible());

            view.updateSnapshot(snapshotWithRows(0, 1), "Card selected.");

            assertFalse(previewOverlay.isVisible());
            Node remainingBottomCard = view.lookupAll(".interactive-card").iterator().next();
            remainingBottomCard.getOnMouseClicked().handle(null);

            assertEquals(1, pickedCards.get());
            return null;
        });
    }

    private static MesosGameBoardView buildView() {
        return buildView((ignoredIndex, ignoredTopRow) -> { });
    }

    private static MesosGameBoardView buildView(java.util.function.BiConsumer<Integer, Boolean> onChooseCard) {
        return new MesosGameBoardView(
                "dio",
                new MesosAssetResolver(),
                () -> { },
                () -> { },
                ignored -> { },
                onChooseCard,
                () -> { }
        );
    }

    private static GameSnapshot snapshot() {
        return snapshot(List.of(), List.of(), 1, 0);
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

    private static GameSnapshot snapshot(
            List<CardSummary> topRow,
            List<CardSummary> bottomRow,
            int remainingTopChoices,
            int remainingBottomChoices
    ) {
        List<CardSummary> characterCards = List.of(
                tribeCard(0, "Hunter", "hunter_era_1_a"),
                tribeCard(1, "Hunter", "hunter_era_1_b"),
                tribeCard(2, "Shaman", "shaman_era_1_a")
        );
        List<CardSummary> buildingCards = List.of(
                tribeCard(3, "Building", "building_era_1_01")
        );
        List<PlayerSummary> players = List.of(
                new PlayerSummary("dio", Color.ORANGE, 3, 0, 3, 1, characterCards, buildingCards),
                new PlayerSummary("Carlo2", Color.BLUE, 0, 0, 0, 0)
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
                false,
                GameStatus.ROUND_ACTIONS,
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
                false
        );
    }

    private static CardSummary tribeCard(int selectionIndex, String type, String presentationId) {
        return new CardSummary(selectionIndex, type, Era.FIRST, false, type, null, null, presentationId);
    }

    private static <T> T singleNode(MesosGameBoardView view, String selector, Class<T> expectedType) {
        Set<Node> matches = view.lookupAll(selector);
        assertEquals(1, matches.size());
        return expectedType.cast(matches.iterator().next());
    }

    private static <T> T runOnFxThread(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }
}
