package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSetupTest {

    @Test
    void randomizeCardsWithinEraKeepsEraOrderForBuildingCards() {
        List<BuildingCard> input = List.of(
                new TestBuildingCard(Era.SECOND),
                new TestBuildingCard(Era.FIRST),
                new TestBuildingCard(Era.THIRD),
                new TestBuildingCard(Era.FIRST),
                new TestBuildingCard(Era.SECOND)
        );

        List<BuildingCard> output = GameSetup.randomizeCardsWithinEra(input, false);

        assertSameCards(input, output);
        assertEraOrder(output, false);
    }

    @Test
    void randomizeCardsWithinEraReversesEraOrderForTribeCards() {
        List<TribeCard> input = List.of(
                new TestTribeCard(Era.SECOND),
                new TestTribeCard(Era.FIRST),
                new TestTribeCard(Era.THIRD),
                new TestTribeCard(Era.FIRST),
                new TestTribeCard(Era.SECOND)
        );

        List<TribeCard> output = GameSetup.randomizeCardsWithinEra(input, true);

        assertSameCards(input, output);
        assertEraOrder(output, true);
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
