package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;


public class HuntEventTest {
    private HuntEvent hunt_event;

    @BeforeEach
    void setUp() {
        hunt_event = new HuntEvent(Era.SECOND, 2, 1);
    }

    @Test
    void constructorInitialisesCorrectly() {
        assertEquals(Era.SECOND, hunt_event.getEra());
        assertEquals(0.5, hunt_event.getMultiplier());
        assertEquals(BuildingTrigger.ON_EVENT_HUNT, hunt_event.asTrigger());
    }

    @Test
    void construtorThrowsWhenPrestigeIsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new HuntEvent(Era.SECOND, 4, 1));
    }

    @Test
    void construtorThrowsWhenFoodIsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new HuntEvent(Era.SECOND, 2, 2));
    }

    @Test
    void calcOutComeReturnsCorrectly() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        HunterCard hunter1 = new HunterCard(Era.SECOND, 2, true);
        HunterCard hunter2 = new HunterCard(Era.SECOND, 2, false);
        HunterCard hunter3 = new HunterCard(Era.FIRST, 2, true);

        tribe2.addCard(hunter1);
        tribe2.addCard(hunter2);
        tribe3.addCard(hunter3);

        Map<Color, Tribe> tribes = Map.of(Color.BLACK, tribe1, Color.BLUE, tribe2, Color.ORANGE, tribe3);
        Map<Color, Integer> output = hunt_event.calculateOutcome(tribes);
        Map<Color, Integer> expected = Map.of(Color.BLACK, 0, Color.BLUE, 4, Color.ORANGE, 2);

        assertEquals(expected, output);
    }
}
