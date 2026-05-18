package it.polimi.ingsw.mesos.CLI;

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
                            new CardSummary(0, "Hunter", Era.SECOND, false, "cacciatore attivo", null, null),
                            new CardSummary(1, "Building End Game", Era.SECOND, false, "fine partita: +4 pp", 2, 4),
                            new CardSummary(2, "Shaman", Era.SECOND, false, "2 stelle sciamaniche", null, null),
                            new CardSummary(3, "Builder", Era.SECOND, false, "sconto edifici 1", null, null),
                            new CardSummary(4, "Inventor", Era.SECOND, false, "icona BOAT", null, null),
                            new CardSummary(5, "Sustenance Event", Era.SECOND, true, "2 cibo per personaggio", null, null),
                            new CardSummary(6, "Building Bonus Event", Era.SECOND, false, "su hunt: hunter da' +1 cibo", 3, 1),
                            new CardSummary(7, "Gatherer", Era.SECOND, false, "sconto sostentamento 1 cibo", null, null)
                    ),
                    List.of(
                            new CardSummary(0, "Shamanic Ritual Event", Era.SECOND, true, "vincitori +6 pp", null, null)
                    )
            );

            String rendering = printer.renderSnapshot(snapshot, "Alice");

            assertTrue(rendering.contains("MESOS | test-room"));
            assertTrue(rendering.contains("Tavolo di gioco"));
            assertTrue(rendering.contains("RIGA ALTA"));
            assertTrue(rendering.contains("RIGA BASSA"));
            assertTrue(rendering.contains("ORDINE DI TURNO"));
            assertTrue(rendering.contains("TESSERE OFFERTA"));
            assertTrue(rendering.contains("card top 0"));
            assertTrue(rendering.contains("card top 7"));
            assertTrue(rendering.contains("offer 1"));
            assertTrue(rendering.contains("'pass'"));
            assertTrue(rendering.contains("cacciatore attivo"));
            assertTrue(rendering.contains("Shamanic Ritual Event"));
            assertTrue(rendering.contains("2 cibo per personaggio"));
            assertTrue(rendering.contains("Alto  : 0"));
            assertTrue(rendering.contains("Basso : 2"));
            assertTrue(rendering.contains("Cibo  : +1"));
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
    void renderHelpIncludesPassCommand() {
        CliPrinter printer = new CliPrinter();

        assertTrue(printer.renderHelp().contains("pass | passa"));
    }
}
