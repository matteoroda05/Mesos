package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
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
        GameTable table_game = createRealGameTable();

        GameContext context_game = new GameContext("MyGame", 2, table_game);

        assertEquals("MyGame", context_game.getGameName());
        assertEquals(2, context_game.getGameSize());
        assertEquals(0, context_game.getRoundNumber());
        assertEquals(Era.FIRST, context_game.getCurrentEra());
        assertSame(table_game, context_game.getGameTable());
        assertTrue(context_game.getPlayers().isEmpty());
    }

    @Test
    void setupPlayersThrowsWhenMapIsNull() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());

        assertThrows(IllegalArgumentException.class, () -> context_game.setupPlayers(null));
    }

    @Test
    void setupPlayersThrowsWhenMapSizeDiffersFromGameSize() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());

        assertThrows(IllegalArgumentException.class, () -> context_game.setupPlayers(new EnumMap<>(Color.class)));
    }

    @Test
    void setupPlayersAcceptsValidMapAndPlayersCanBeRetrieved() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players_map = createPlayersForTwoPlayerGame();

        context_game.setupPlayers(players_map);

        assertEquals(2, context_game.getPlayers().size());
        assertSame(players_map.get(Color.BLUE), context_game.getPlayer(Color.BLUE));
        assertSame(players_map.get(Color.YELLOW), context_game.getPlayer(Color.YELLOW));
    }

    @Test
    void setupPlayersPlacesTotemsOnTurnOrderTile() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players_map = createPlayersForTwoPlayerGame();

        context_game.setupPlayers(players_map);

        assertIterableEquals(
                List.of(Color.BLUE, Color.YELLOW),
                context_game.getGameTable().getTurnOrderTile().currentOrderTopToBottom());
    }

    @Test
    void setupPlayersUsesInputMapIterationOrderForTurnOrderPlacement() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players_map = new LinkedHashMap<>();
        players_map.put(Color.YELLOW, new Player("Bob", Color.YELLOW, 3));
        players_map.put(Color.BLUE, new Player("Alice", Color.BLUE, 3));

        context_game.setupPlayers(players_map);

        assertIterableEquals(
                List.of(Color.YELLOW, Color.BLUE),
                context_game.getGameTable().getTurnOrderTile().currentOrderTopToBottom());
    }

    @Test
    void setupPlayersThrowsWhenCalledTwiceOnSameContext() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players_map = createPlayersForTwoPlayerGame();

        context_game.setupPlayers(players_map);

        assertThrows(IllegalStateException.class, () -> context_game.setupPlayers(players_map));
    }

    @Test
    void getPlayersReturnsImmutableListSnapshot() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players_map = createPlayersForTwoPlayerGame();
        context_game.setupPlayers(players_map);

        List<Player> players_from_context = context_game.getPlayers();

        assertThrows(UnsupportedOperationException.class,
            () -> players_from_context.add(new Player("Extra", Color.BLACK, 2)));
    }

    @Test
    void getPlayerThrowsWhenColorIsMissing() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        context_game.setupPlayers(createPlayersForTwoPlayerGame());

        assertThrows(IllegalArgumentException.class, () -> context_game.getPlayer(null));
        assertThrows(IllegalArgumentException.class, () -> context_game.getPlayer(Color.BLACK));
    }

    @Test
    void setupPlayersRejectsNullPlayerValue() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players_map = new EnumMap<>(Color.class);
        players_map.put(Color.BLUE, new Player("Alice", Color.BLUE, 3));
        players_map.put(Color.YELLOW, null);

        assertThrows(IllegalArgumentException.class, () -> context_game.setupPlayers(players_map));
    }

    @Test
    void newRoundDoesNotAdvanceEraAfterMultipleCalls() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());

        context_game.newRound();
        context_game.newRound();

        assertEquals(2, context_game.getRoundNumber());
        assertEquals(Era.FIRST, context_game.getCurrentEra());
    }

    @Test
    void newEraAdvancesUntilLastEraThenRejectsFurtherAdvancement() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());

        context_game.newEra();
        assertEquals(Era.SECOND, context_game.getCurrentEra());

        context_game.newEra();
        assertEquals(Era.THIRD, context_game.getCurrentEra());

        assertThrows(IllegalStateException.class, context_game::newEra);
    }

    @Test
    void computeFinalScoresAddsTribePrestigeToEachPlayer() {
        GameContext context_game = new GameContext("MyGame", 2, createRealGameTable());
        Map<Color, Player> players_map = createPlayersForTwoPlayerGame();

        players_map.get(Color.BLUE).changePrestigePoints(3);
        players_map.get(Color.YELLOW).changePrestigePoints(1);

        players_map.get(Color.BLUE).getTribe().addCard(new BuildingAfterEveryone(Era.FIRST, 2, 1, 4));
        players_map.get(Color.YELLOW).getTribe().addCard(new BuildingAfterEveryone(Era.FIRST, 2, 1, 7));

        context_game.setupPlayers(players_map);
        context_game.computeFinalScores();

        assertEquals(7, players_map.get(Color.BLUE).getPrestigePoints());
        assertEquals(8, players_map.get(Color.YELLOW).getPrestigePoints());
    }

    private static Map<Color, Player> createPlayersForTwoPlayerGame() {
        Map<Color, Player> players_map = new EnumMap<>(Color.class);
        players_map.put(Color.BLUE, new Player("Alice", Color.BLUE, 3));
        players_map.put(Color.YELLOW, new Player("Bob", Color.YELLOW, 3));
        return players_map;
    }

    private static GameTable createRealGameTable() {
        return new GameTable(createOfferTrack(), createTurnOrderTile(), createBuildingDeck(), createTribeDeck());
    }

    private static OfferTrack createOfferTrack() {
        List<OfferTile> offer_tiles = List.of(
                new OfferTile('A', 0, 0, 1, 2),
                new OfferTile('B', 1, 0, 0, 2),
                new OfferTile('C', 0, 1, 0, 2),
                new OfferTile('D', 1, 1, 0, 2)
        );
        return new OfferTrack(offer_tiles, 2);
    }

    private static TurnOrderTile createTurnOrderTile() {
        return new TurnOrderTile(List.of(new TurnOrderSpace(1), new TurnOrderSpace(-1)));
    }

    private static BuildingDeck createBuildingDeck() {
        return new BuildingDeck(List.of(new BuildingAfterEveryone(Era.FIRST, 2, 1, 1)));
    }

    private static TribeDeck createTribeDeck() {
        List<TribeCard> tribes = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            tribes.add(new HunterCard(Era.FIRST, 2, i % 2 == 0));
        }
        return new TribeDeck(tribes, 2);
    }

}
