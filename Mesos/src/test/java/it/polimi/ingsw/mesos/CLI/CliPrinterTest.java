package it.polimi.ingsw.mesos.CLI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.CardSummary;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.OfferTileSummary;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import it.polimi.ingsw.mesos.Network.TurnOrderSpaceSummary;

class CliPrinterTest {
    @Test
    void renderSnapshotShowsBoardAreasAndCommands() {
        String previousWidth = System.getProperty("mesos.cli.width");
        System.setProperty("mesos.cli.width", "180");
        try {
            CliPrinter printer = new CliPrinter();
            GameSnapshot snapshot = new GameSnapshot(
                    "test-room",
                    2,
                    2,
                    true,
                    false,
                    GameStatus.ROUND_ACTIONS,
                    2,
                    Era.SECOND,
                    "Alice",
                    Color.BLUE,
                    1,
                    2,
                    List.of(Color.BLUE, Color.YELLOW),
                    List.of(
                            new PlayerSummary("Alice", Color.BLUE, 6, 12, 5, 2),
                            new PlayerSummary("Bob", Color.YELLOW, 4, 10, 4, 1)
                    ),
                    List.of(
                            new OfferTileSummary(0, 'A', 1, 0, 0, false, Color.BLUE, "Alice"),
                            new OfferTileSummary(1, 'B', 0, 2, 1, true, null, null)
                    ),
                    List.of(
                            new TurnOrderSpaceSummary(0, 0, Color.BLUE, "Alice"),
                            new TurnOrderSpaceSummary(1, 1, Color.YELLOW, "Bob")
                    ),
                    List.of(
                            new CardSummary(0, "Hunter", Era.SECOND, false, "active hunter", null, null),
                            new CardSummary(1, "Building End Game", Era.SECOND, false, "end game: +4 pp", 2, 4),
                            new CardSummary(2, "Shaman", Era.SECOND, false, "2 shamanic stars", null, null),
                            new CardSummary(3, "Builder", Era.SECOND, false, "building discount 1", null, null),
                            new CardSummary(4, "Inventor", Era.SECOND, false, "icon BOAT", null, null),
                            new CardSummary(5, "Sustenance Event", Era.SECOND, true, "2 food per character", null, null),
                            new CardSummary(6, "Building Bonus Event", Era.SECOND, false, "on hunt: hunter gives +1 food", 3, 1),
                            new CardSummary(7, "Gatherer", Era.SECOND, false, "sustenance discount 1 food", null, null)
                    ),
                    List.of(
                            new CardSummary(0, "Shamanic Ritual Event", Era.SECOND, true, "winners +6 pp", null, null)
                    )
            );

            String rendering = printer.renderSnapshot(snapshot, "Alice");

            assertTrue(rendering.contains("MESOS | test-room"));
            assertTrue(rendering.contains("GAME TABLE"));
            assertTrue(rendering.contains("TOP ROW"));
            assertTrue(rendering.contains("BOTTOM ROW"));
            assertTrue(rendering.contains("TURN ORDER"));
            assertTrue(rendering.contains("card top 0"));
            assertTrue(rendering.contains("card top 7"));
            assertTrue(rendering.contains("offer 1"));
            assertTrue(rendering.contains("'pass'"));
            assertTrue(rendering.contains("active hunter"));
            assertTrue(rendering.contains("Shamanic Ritual Event"));
            assertTrue(rendering.contains("2 food per character"));
            assertTrue(rendering.contains("Top   : 0"));
            assertTrue(rendering.contains("Bottom: 2"));
            assertTrue(rendering.contains("Food  : +1"));
            assertTrue(rendering.lines().mapToInt(String::length).max().orElse(0) <= 180);
        } finally {
            if (previousWidth == null) {
                System.clearProperty("mesos.cli.width");
            } else {
                System.setProperty("mesos.cli.width", previousWidth);
            }
        }
    }

    @Test
    void renderSnapshotTagsEverySharedWinner() {
        CliPrinter printer = new CliPrinter();
        GameSnapshot snapshot = new GameSnapshot(
                "tie-room",
                2,
                2,
                true,
                true,
                GameStatus.END_GAME,
                5,
                Era.THIRD,
                null,
                null,
                0,
                0,
                List.of(Color.BLUE, Color.YELLOW),
                List.of(
                        new PlayerSummary("Alice", Color.BLUE, 5, 10, 0, 0,
                                List.of(), List.of(), 0, 0, 0, 0, 0, 0, true),
                        new PlayerSummary("Bob", Color.YELLOW, 5, 10, 0, 0,
                                List.of(), List.of(), 0, 0, 0, 0, 0, 0, true)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        String rendering = printer.renderSnapshot(snapshot, "Alice");

        assertTrue(rendering.contains("WINNER: Alice"));
        assertTrue(rendering.contains("WINNER: Bob"));
        assertEquals(2, rendering.split("WINNER:", -1).length - 1);
    }

    @Test
    void renderHelpIncludesPassCommand() {
        CliPrinter printer = new CliPrinter();

        assertTrue(printer.renderHelp().contains("pass | passa"));
    }
}
