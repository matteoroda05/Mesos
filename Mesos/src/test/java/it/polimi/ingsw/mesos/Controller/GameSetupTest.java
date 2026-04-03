package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static <T extends it.polimi.ingsw.mesos.Model.Cards.Card> void assertEraOrder(List<T> cards, boolean reverse) {
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
    }

    private static final class TestBuildingCard extends BuildingCard {
        private TestBuildingCard(Era era) {
            super(era, 2, 1, 1, BuildingTrigger.ON_END_TURN);
        }
    }
}
