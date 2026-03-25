package it.polimi.ingsw.mesos.Model.Cards.Events;

import it.polimi.ingsw.mesos.Model.Cards.Characters.ShamanCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void calcOutComeReturnsCorrectly1() {
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

        List<Tribe> tribes = List.of(tribe1, tribe2, tribe3);
        List<Integer> output = shamanic_ritual_event.calculateOutcome(tribes);
        List<Integer> expected = List.of(-5, 7, 0);

        assertIterableEquals(expected, output);
    }

    @Test
    void calcOutComeReturnsCorrectly2() {
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

        List<Tribe> tribes = List.of(tribe1, tribe2, tribe3);
        List<Integer> output = shamanic_ritual_event.calculateOutcome(tribes);
        List<Integer> expected = List.of(-5, 7, 7);

        assertIterableEquals(expected, output);
    }

    @Test
    void calcOutComeReturnsCorrectly3() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        ShamanCard shaman1 = new ShamanCard(Era.SECOND, 2, 1);
        ShamanCard shaman2 = new ShamanCard(Era.SECOND, 2, 2);

        tribe1.addCard(shaman1);
        tribe1.addCard(shaman2);

        List<Tribe> tribes = List.of(tribe1, tribe2, tribe3);
        List<Integer> output = shamanic_ritual_event.calculateOutcome(tribes);
        List<Integer> expected = List.of(7, -5, -5);

        assertIterableEquals(expected, output);
    }
}
