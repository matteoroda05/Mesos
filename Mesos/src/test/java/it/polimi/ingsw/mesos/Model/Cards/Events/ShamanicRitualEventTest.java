package it.polimi.ingsw.mesos.Model.Cards.Events;

import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ShamanCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ShamanicRitualEventTest {
    private ShamanicRitualEvent shamanic_ritual_event;

    @BeforeEach
    void setUp() {
        shamanic_ritual_event = new ShamanicRitualEvent(Era.FIRST, 7, -5);
    }

    @Test
    void constructorInitialisesCorrectly() {
        assertEquals(Era.FIRST, shamanic_ritual_event.getEra());
        assertEquals(BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL, shamanic_ritual_event.asTrigger());
    }

    @Test
    void constructorThrowsCorrectly(){
        assertThrows(IllegalArgumentException.class, () -> new ShamanicRitualEvent(Era.THIRD, 18, -3));
        assertThrows(IllegalArgumentException.class, () -> new ShamanicRitualEvent(Era.THIRD, 15, 3));
    }

    @Test
    void calculateOutcome_distinctHighestAndLowestStarCounts_awardsBonusToTopAndPenaltyToBottom() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        ShamanCard shaman1 = new ShamanCard(Era.SECOND, 2, 1);
        ShamanCard shaman2 = new ShamanCard(Era.SECOND, 2, 2);
        ShamanCard shaman3 = new ShamanCard(Era.FIRST, 2, 2);
        ShamanCard shaman4 = new ShamanCard(Era.THIRD, 2, 1);
        ShamanCard shaman5 = new ShamanCard(Era.THIRD, 2, 1);
        ShamanCard shaman6 = new ShamanCard(Era.FIRST, 2, 2);

        tribe1.addCard(shaman1);
        tribe2.addCard(shaman2);
        tribe2.addCard(shaman3);
        tribe2.addCard(shaman4);
        tribe3.addCard(shaman5);
        tribe3.addCard(shaman6);

        Map<Color, Tribe> tribes = Map.of(Color.BLACK, tribe1, Color.BLUE, tribe2, Color.ORANGE, tribe3);
        Map<Color, Result> output = shamanic_ritual_event.calculateOutcome(tribes);

        assertEquals(-5, output.get(Color.BLACK).prestige());
        assertEquals(7, output.get(Color.BLUE).prestige());
        assertEquals(0, output.get(Color.ORANGE).prestige());
    }

    @Test
    void calculateOutcome_multiplePlayersTiedForMostStars_awardsBonusToEachWinner() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        ShamanCard shaman1 = new ShamanCard(Era.SECOND, 2, 1);
        ShamanCard shaman2 = new ShamanCard(Era.SECOND, 2, 2);
        ShamanCard shaman3 = new ShamanCard(Era.FIRST, 2, 2);
        ShamanCard shaman4 = new ShamanCard(Era.THIRD, 2, 1);
        ShamanCard shaman5 = new ShamanCard(Era.THIRD, 2, 1);
        ShamanCard shaman6 = new ShamanCard(Era.FIRST, 2, 2);

        tribe1.addCard(shaman1);
        tribe2.addCard(shaman2);
        tribe2.addCard(shaman3);
        tribe3.addCard(shaman4);
        tribe3.addCard(shaman5);
        tribe3.addCard(shaman6);

        Map<Color, Tribe> tribes = Map.of(Color.BLACK, tribe1, Color.BLUE, tribe2, Color.ORANGE, tribe3);
        Map<Color, Result> output = shamanic_ritual_event.calculateOutcome(tribes);

        assertEquals(-5, output.get(Color.BLACK).prestige());
        assertEquals(7, output.get(Color.BLUE).prestige());
        assertEquals(7, output.get(Color.ORANGE).prestige());
    }

    @Test
    void calculateOutcome_multiplePlayersTiedForFewestStars_appliesPenaltyToEachLoser() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        ShamanCard shaman1 = new ShamanCard(Era.SECOND, 2, 1);
        ShamanCard shaman2 = new ShamanCard(Era.SECOND, 2, 2);

        tribe1.addCard(shaman1);
        tribe1.addCard(shaman2);

        Map<Color, Tribe> tribes = Map.of(Color.BLACK, tribe1, Color.BLUE, tribe2, Color.ORANGE, tribe3);
        Map<Color, Result> output = shamanic_ritual_event.calculateOutcome(tribes);

        assertEquals(7, output.get(Color.BLACK).prestige());
        assertEquals(-5, output.get(Color.BLUE).prestige());
        assertEquals(-5, output.get(Color.ORANGE).prestige());
    }

    @Test
    void calculateOutcome_preEventStarBonus_appliesBeforeWinnerAndLoserAreDecided() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        // Base stars: BLACK = 1, BLUE = 3, ORANGE = 1.
        tribe1.addCard(new ShamanCard(Era.FIRST, 2, 1));
        tribe2.addCard(new ShamanCard(Era.FIRST, 2, 3));
        tribe3.addCard(new ShamanCard(Era.FIRST, 2, 1));

        // Pre-event building grants ORANGE +3 stars, lifting it from 1 to 4
        // and making it the sole highest, ahead of BLUE's 3.
        tribe3.addCard(new BuildingShamanicRitual(Era.FIRST, 2, 3, 4, 3, 0));

        Map<Color, Tribe> tribes = Map.of(Color.BLACK, tribe1, Color.BLUE, tribe2, Color.ORANGE, tribe3);
        Map<Color, Result> output = shamanic_ritual_event.calculateOutcome(tribes);

        assertEquals(-5, output.get(Color.BLACK).prestige()); // lowest (1)
        assertEquals(0, output.get(Color.BLUE).prestige());   // middle (3)
        assertEquals(7, output.get(Color.ORANGE).prestige()); // highest after bonus (4)
    }
}
