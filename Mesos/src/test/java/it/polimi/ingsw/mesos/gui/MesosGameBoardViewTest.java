package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import it.polimi.ingsw.mesos.Network.TurnOrderSpaceSummary;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

            HBox bottomBar = singleNode(view, ".bottom-info-bar", HBox.class);
            assertEquals(0.0, view.getPadding().getLeft());
            assertTrue(bottomBar.getParent().getStyleClass().contains("board-viewport"));
            assertEquals(Pos.BOTTOM_LEFT, StackPane.getAlignment(bottomBar));
            assertEquals(2, bottomBar.getChildren().size());
            assertTrue(bottomBar.getChildren().get(0).getStyleClass().contains("info-panel"));
            assertTrue(bottomBar.getChildren().get(1).getStyleClass().contains("owned-cards-debug-box"));
            assertEquals(1, view.lookupAll(".info-panel-color-indicator").size());
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

    private static MesosGameBoardView buildView() {
        return new MesosGameBoardView(
                "dio",
                new MesosAssetResolver(),
                () -> { },
                () -> { },
                ignored -> { },
                (ignoredIndex, ignoredTopRow) -> { },
                () -> { }
        );
    }

    private static GameSnapshot snapshot() {
        List<PlayerSummary> players = List.of(
                new PlayerSummary("dio", Color.ORANGE, 3, 0, 0, 0),
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
                1,
                0,
                List.of(Color.ORANGE, Color.BLUE),
                players,
                List.of(),
                turnOrder,
                List.of(),
                List.of(),
                false
        );
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
