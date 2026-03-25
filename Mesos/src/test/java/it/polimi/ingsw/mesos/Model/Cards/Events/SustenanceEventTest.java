package it.polimi.ingsw.mesos.Model.Cards.Events;

import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.GathererCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class SustenanceEventTest {
    private SustenanceEvent sustenance_event;

    @BeforeEach
    void setUp() {
        sustenance_event = new SustenanceEvent(Era.SECOND, -2, 1);
    }

    @Test
    void constructorInitialisesCorrectly() {
        assertEquals(Era.SECOND, sustenance_event.getEra());
        assertEquals(BuildingTrigger.ON_EVENT_SUSTENANCE, sustenance_event.asTrigger());
        assertEquals(-2, sustenance_event.getPointPenalty());
    }

    @Test
    void constructorThrowsCorrectly(){
        assertThrows(IllegalArgumentException.class, () -> new SustenanceEvent(Era.THIRD, -4, 1));
        assertThrows(IllegalArgumentException.class, () -> new SustenanceEvent(Era.THIRD, -2, 0));
    }

    @Test
    void calcOutComeReturnsCorrectly1() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        GathererCard gatherer1 = new GathererCard(Era.SECOND, 2);

        tribe2.addCard(gatherer1);

        ArtistCard artist1 = new ArtistCard(Era.SECOND, 2);
        ArtistCard artist2 = new ArtistCard(Era.SECOND, 2);
        ArtistCard artist3 = new ArtistCard(Era.FIRST, 2);
        ArtistCard artist4 = new ArtistCard(Era.THIRD, 2);
        ArtistCard artist5 = new ArtistCard(Era.THIRD, 2);
        ArtistCard artist6 = new ArtistCard(Era.FIRST, 2);

        tribe1.addCard(artist1);
        tribe2.addCard(artist2);
        tribe2.addCard(artist3);
        tribe2.addCard(artist4);
        tribe3.addCard(artist5);
        tribe3.addCard(artist6);

        List<Tribe> tribes = List.of(tribe1, tribe2, tribe3);
        List<Integer> output = sustenance_event.calculateOutcome(tribes);
        List<Integer> expected = List.of(1, 1, 2);

        assertIterableEquals(expected, output);
    }
}
