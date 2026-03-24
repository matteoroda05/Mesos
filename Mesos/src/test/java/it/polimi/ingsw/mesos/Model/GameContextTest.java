package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndRound;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextTest {

    @Test
    void constructorThrowsWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new GameContext(null, 2, createRealGameTable()));
    }

    @Test
    void constructorThrowsWhenNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new GameContext("   ", 2, createRealGameTable()));
    }

    @Test
    void constructorThrowsWhenGameSizeIsBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameContext("test", Globals.MIN_NUM_PLAYERS - 1, createRealGameTable()));
    }

    @Test
    void constructorThrowsWhenGameSizeIsAboveMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameContext("test", Globals.MAX_NUM_PLAYERS + 1, createRealGameTable()));
    }

    @Test
    void constructorThrowsWhenGameTableIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new GameContext("test", 2, null));
    }

    @Test
    void constructorInitializesDefaultsAndGetters() {
        GameTable table = createRealGameTable();

        GameContext context = new GameContext("MyGame", 2, table);

        assertEquals("MyGame", context.getGameName());
        assertEquals(2, context.getGameSize());
        assertEquals(0, context.getRoundNumber());
        assertEquals(Era.FIRST, context.getCurrentEra());
        assertSame(table, context.getGameTable());
        assertTrue(context.getPlayers().isEmpty());
    }

    @Test
    void setupPlayersThrowsWhenMapIsNull() {
        GameContext context = new GameContext("MyGame", 2, createRealGameTable());

        assertThrows(IllegalArgumentException.class, () -> context.setupPlayers(null));
    }

    @Test
    void setupPlayersThrowsWhenMapSizeDiffersFromGameSize() {
        GameContext context = new GameContext("MyGame", 2, createRealGameTable());

        assertThrows(IllegalArgumentException.class, () -> context.setupPlayers(new EnumMap<>(Color.class)));
    }

    @Test
    void setupPlayersAcceptsValidMapAndPlayersCanBeRetrieved() {
        GameContext context = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players = createPlayersForTwoPlayerGame();

        context.setupPlayers(players);

        assertEquals(2, context.getPlayers().size());
        assertSame(players.get(Color.BLUE), context.getPlayer(Color.BLUE));
        assertSame(players.get(Color.YELLOW), context.getPlayer(Color.YELLOW));
    }

    @Test
    void getPlayersReturnsImmutableListSnapshot() {
        GameContext context = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players = createPlayersForTwoPlayerGame();
        context.setupPlayers(players);

        List<Player> fromContext = context.getPlayers();

        assertThrows(UnsupportedOperationException.class,
                () -> fromContext.add(new Player("Extra", Color.BLACK, 2)));
    }

    @Test
    void getPlayerThrowsWhenColorIsMissing() {
        GameContext context = new GameContext("MyGame", 2, createRealGameTable());
        context.setupPlayers(createPlayersForTwoPlayerGame());

        assertThrows(IllegalArgumentException.class, () -> context.getPlayer(Color.BLACK));
    }

    @Test
    void newRoundAlwaysIncrementsRoundNumber() {
        StubGameTable table = new StubGameTable(false);
        GameContext context = new GameContext("MyGame", 2, table);

        context.newRound();

        assertEquals(1, context.getRoundNumber());
        assertEquals(1, table.getNewRoundCalls());
    }

    @Test
    void newRoundKeepsEraWhenTableDoesNotOpenNewEra() {
        StubGameTable table = new StubGameTable(false);
        GameContext context = new GameContext("MyGame", 2, table);

        context.newRound();

        assertEquals(Era.FIRST, context.getCurrentEra());
    }

    @Test
    void newRoundAdvancesEraWhenTableOpensNewEra() {
        StubGameTable table = new StubGameTable(true);
        GameContext context = new GameContext("MyGame", 2, table);

        context.newRound();

        assertEquals(Era.SECOND, context.getCurrentEra());
    }

    @Test
    void computeFinalScoresAddsTribePrestigeToEachPlayer() {
        GameContext context = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players = createPlayersForTwoPlayerGame();

        players.get(Color.BLUE).changePrestigePoints(3);
        players.get(Color.YELLOW).changePrestigePoints(1);

        players.get(Color.BLUE).getTribe().addCard(new BuildingEndRound(Era.FIRST, 2, 1, 4));
        players.get(Color.YELLOW).getTribe().addCard(new BuildingEndRound(Era.FIRST, 2, 1, 7));

        context.setupPlayers(players);
        context.computeFinalScores();

        assertEquals(7, players.get(Color.BLUE).getPrestigePoints());
        assertEquals(8, players.get(Color.YELLOW).getPrestigePoints());
    }

    private static Map<Color, Player> createPlayersForTwoPlayerGame() {
        Map<Color, Player> players = new EnumMap<>(Color.class);
        players.put(Color.BLUE, new Player("Alice", Color.BLUE, 3));
        players.put(Color.YELLOW, new Player("Bob", Color.YELLOW, 3));
        return players;
    }

    private static GameTable createRealGameTable() {
        return new GameTable(createOfferTrack(), createTurnOrderTile(), createBuildingDeck(), createTribeDeck());
    }

    private static OfferTrack createOfferTrack() {
        List<OfferTile> offerTiles = List.of(
                new OfferTile('A', 0, 0, 1, 2),
                new OfferTile('B', 1, 0, 0, 2),
                new OfferTile('C', 0, 1, 0, 2),
                new OfferTile('D', 1, 1, 0, 2)
        );
        return new OfferTrack(offerTiles, 2);
    }

    private static TurnOrderTile createTurnOrderTile() {
        return new TurnOrderTile(List.of(new TurnOrderSpace(1), new TurnOrderSpace(-1)));
    }

    private static BuildingDeck createBuildingDeck() {
        return new BuildingDeck(List.of(new BuildingEndRound(Era.FIRST, 2, 1, 1)));
    }

    private static TribeDeck createTribeDeck() {
        List<TribeCard> tribes = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            tribes.add(new HunterCard(Era.FIRST, 2, i % 2 == 0));
        }
        return new TribeDeck(tribes, 2);
    }

    private static final class StubGameTable extends GameTable {
        private final ArrayDeque<Boolean> newRoundResults;
        private int newRoundCalls;

        private StubGameTable(boolean... newRoundResults) {
            super(createOfferTrack(), createTurnOrderTile(), createBuildingDeck(), createTribeDeck());
            this.newRoundResults = new ArrayDeque<>();
            for (boolean result : newRoundResults) {
                this.newRoundResults.addLast(result);
            }
            this.newRoundCalls = 0;
        }

        @Override
        public boolean newRound() {
            newRoundCalls += 1;
            return newRoundResults.isEmpty() ? false : newRoundResults.removeFirst();
        }

        private int getNewRoundCalls() {
            return newRoundCalls;
        }
    }
}
