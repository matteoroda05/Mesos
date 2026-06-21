package it.polimi.ingsw.mesos.controller;

import it.polimi.ingsw.mesos.model.CardLoader;
import it.polimi.ingsw.mesos.model.cards.TribeCard;
import it.polimi.ingsw.mesos.model.cards.events.HuntEvent;
import it.polimi.ingsw.mesos.model.cards.events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.model.cards.events.SustenanceEvent;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.GameContext;
import it.polimi.ingsw.mesos.model.Player;
import it.polimi.ingsw.mesos.dto.game.CardSummary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameSetupTest {

    @Test
    void randomizeCardsWithinEraReversesEraOrderForTribeCards() {
        List<TribeCard> input = List.of(
                new TestTribeCard(Era.SECOND),
                new TestTribeCard(Era.FIRST),
                new TestTribeCard(Era.THIRD),
                new TestTribeCard(Era.FIRST),
                new TestTribeCard(Era.SECOND)
        );

        List<TribeCard> output = GameSetup.selectTribeCards(input, 5);

        assertSameCards(input, output);
        assertEraOrder(output, true);
    }

    @Test
    void selectTribeCardsPlacesFinalEventsWhereTribeDeckDrawsThemLast() {
        SustenanceEvent finalSustenance = new SustenanceEvent(Era.THIRD, -1, 1);
        ShamanicRitualEvent finalRitual = new ShamanicRitualEvent(Era.THIRD, 5, -3);
        HuntEvent regularThirdEraEvent = new HuntEvent(Era.THIRD, 1, 1);
        TestTribeCard firstEraCard = new TestTribeCard(Era.FIRST);
        TestTribeCard secondEraCard = new TestTribeCard(Era.SECOND);

        List<TribeCard> output = GameSetup.selectTribeCards(
                List.of(firstEraCard, finalSustenance, secondEraCard, finalRitual, regularThirdEraEvent),
                5);

        assertSameCards(List.of(firstEraCard, finalSustenance, secondEraCard, finalRitual, regularThirdEraEvent), output);
        assertTrue(output.subList(0, 2).containsAll(List.of(finalSustenance, finalRitual)));
        assertEquals(firstEraCard, output.getLast());
    }

    @Test
    void createGameInitializesCoreComponentsForSupportedPlayerCounts() throws Exception {
        GameSetup setup = new GameSetup();
        CardLoader.CardData cardData = CardLoader.load();

        for (int playerCount : List.of(2, 3, 4, 5)) {
            GameContext context = setup.createGame("Game" + playerCount, playerCount, cardData);

            assertEquals("Game" + playerCount, context.getGameName());
            assertEquals(playerCount, context.getGameSize());
            assertNotNull(context.getGameTable());
            assertNotNull(context.getGameTable().getOfferTrack());
            assertNotNull(context.getGameTable().getTurnOrderTile());
            assertNotNull(context.getGameTable().getTopRow());
            assertNotNull(context.getGameTable().getBottomRow());
            assertEquals(playerCount, context.getGameTable().getTurnOrderTile().spaces().size());
        }
    }

    @Test
    void createGameRejectsInvalidInputsAndMalformedCardData() throws Exception {
        GameSetup setup = new GameSetup();
        CardLoader.CardData validData = CardLoader.load();

        assertThrows(IllegalArgumentException.class, () -> setup.createGame(null, 2, validData));
        assertThrows(IllegalArgumentException.class, () -> setup.createGame(" ", 2, validData));
        assertThrows(IllegalArgumentException.class, () -> setup.createGame("Game", 1, validData));
        assertThrows(IllegalArgumentException.class, () -> setup.createGame("Game", 6, validData));
        assertThrows(IllegalArgumentException.class, () -> setup.createGame("Game", 2, null));
        assertThrows(IllegalArgumentException.class,
                () -> setup.createGame("Game", 2, new CardLoader.CardData(null, validData.building_cards(), validData.offer_tiles())));
        assertThrows(IllegalArgumentException.class,
                () -> setup.createGame("Game", 2, new CardLoader.CardData(validData.tribe_cards(), null, validData.offer_tiles())));
        assertThrows(IllegalArgumentException.class,
                () -> setup.createGame("Game", 2, new CardLoader.CardData(validData.tribe_cards(), validData.building_cards(), null)));
    }

    @Test
    void createMapOfPlayersPopulatesPlayersAndTurnOrderFromTwoToFive() throws Exception {
        GameSetup setup = new GameSetup();
        CardLoader.CardData cardData = CardLoader.load();

        for (int playerCount : List.of(2, 3, 4, 5)) {
            GameContext context = setup.createGame("DispatcherFlow" + playerCount, playerCount, cardData);
            List<String> nicknames = createNicknames(playerCount);

            setup.createMapOfPlayers(context, nicknames);

            List<Player> players = context.getPlayers();
            assertEquals(playerCount, players.size());

            List<Color> assignedColors = new ArrayList<>();
            for (Player player : players) {
                assignedColors.add(player.getColor());
            }

            assertEquals(playerCount, new HashSet<>(assignedColors).size());

            for (Player player : players) {
                assertEquals(player, context.getPlayer(player.getColor()));
            }

            assertIterableEquals(
                    assignedColors,
                    context.getGameTable().getTurnOrderTile().currentOrderTopToBottom());
        }
    }

    @Test
    void createMapOfPlayersRejectsInvalidInputs() throws Exception {
        GameSetup setup = new GameSetup();
        GameContext context = setup.createGame("PlayerValidation", 2, CardLoader.load());

        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(context, null));
        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(null, List.of("Alice", "Bob")));
        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(context, List.of("Alice")));
        List<String> nullNickname = new ArrayList<>();
        nullNickname.add("Alice");
        nullNickname.add(null);
        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(context, nullNickname));
        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(context, List.of("Alice", " ")));
    }

    @Test
    void selectTribeCardsRejectsNullCardList() {
        assertThrows(IllegalArgumentException.class, () -> GameSetup.selectTribeCards(null, 2));
    }

    @Test
    void createMapOfPlayersShufflesColorOrderAtLeastOnce() throws Exception {
        GameSetup setup = new GameSetup();
        CardLoader.CardData cardData = CardLoader.load();
        List<Color> enumOrder = List.of(Color.values());
        Set<List<Color>> observedOrders = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            GameContext context = setup.createGame("ShuffleCheck" + i, 5, cardData);
            setup.createMapOfPlayers(context, createNicknames(5));

            List<Color> order = context.getGameTable().getTurnOrderTile().currentOrderTopToBottom();
            observedOrders.add(new ArrayList<>(order));
        }

        assertTrue(observedOrders.stream().anyMatch(order -> !order.equals(enumOrder)));
    }

    @Test
    void createMapOfPlayersHonorsReservedColorsAndAssignsUniqueFallbacks() throws Exception {
        GameSetup setup = new GameSetup();
        GameContext context = setup.createGame("ReservedColors", 4, CardLoader.load());
        List<String> nicknames = List.of("Alice", "Bob", "Charlie", "Dana");
        Map<String, Color> reservedColors = Map.of(
                "Alice", Color.BLACK,
                "Charlie", Color.YELLOW
        );

        setup.createMapOfPlayers(context, nicknames, reservedColors);

        Map<String, Color> assignedColors = new java.util.LinkedHashMap<>();
        for (Player player : context.getPlayers()) {
            assignedColors.put(player.getNickname(), player.getColor());
        }

        assertEquals(Color.BLACK, assignedColors.get("Alice"));
        assertEquals(Color.YELLOW, assignedColors.get("Charlie"));
        assertEquals(4, new HashSet<>(assignedColors.values()).size());
        assertNotSame(Color.BLACK, assignedColors.get("Bob"));
        assertNotSame(Color.YELLOW, assignedColors.get("Bob"));
        assertNotSame(Color.BLACK, assignedColors.get("Dana"));
        assertNotSame(Color.YELLOW, assignedColors.get("Dana"));
    }

    @Test
    void createMapOfPlayers_invalidReservedColors_areRejected() throws Exception {
        GameSetup setup = new GameSetup();
        CardLoader.CardData data = CardLoader.load();
        List<String> nicknames = List.of("Alice", "Bob", "Charlie");

        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(
                setup.createGame("ReservedNull", 3, data), nicknames, null));

        java.util.Map<String, Color> blankNickname = new java.util.HashMap<>();
        blankNickname.put("  ", Color.BLUE);
        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(
                setup.createGame("ReservedBlankNick", 3, data), nicknames, blankNickname));

        java.util.Map<String, Color> nullColor = new java.util.HashMap<>();
        nullColor.put("Alice", null);
        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(
                setup.createGame("ReservedNullColor", 3, data), nicknames, nullColor));

        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(
                setup.createGame("ReservedUnknownNick", 3, data), nicknames,
                java.util.Map.of("Zoe", Color.BLUE)));

        java.util.Map<String, Color> duplicateColors = new java.util.HashMap<>();
        duplicateColors.put("Alice", Color.BLUE);
        duplicateColors.put("Bob", Color.BLUE);
        assertThrows(IllegalArgumentException.class, () -> setup.createMapOfPlayers(
                setup.createGame("ReservedDupColor", 3, data), nicknames, duplicateColors));
    }

    private static List<String> createNicknames(int playerCount) {
        List<String> nicknames = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            nicknames.add("Player" + i);
        }
        return nicknames;
    }

    private static <T> void assertSameCards(List<T> input, List<T> output) {
        assertEquals(input.size(), output.size());
        assertTrue(output.containsAll(input));
        assertTrue(input.containsAll(output));
    }

    private static <T extends it.polimi.ingsw.mesos.model.cards.Card> void assertEraOrder(List<T> cards, boolean reverse) {
        int previous = reverse ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        for (T card : cards) {
            int current = card.getEra().ordinal();
            if (reverse) {
                assertTrue(current <= previous);
            } else {
                assertTrue(current >= previous);
            }
            previous = current;
        }
    }

    private static final class TestTribeCard extends TribeCard {
        private TestTribeCard(Era era) {
            super(era, 2);
        }

        @Override
        public String accept(it.polimi.ingsw.mesos.model.cards.CardDescriptionVisitor visitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CardSummary getSummary(int selectionIndex) {
            return null;
        }

        @Override
        public String getReadableName() {
            return "";
        }
    }
}
