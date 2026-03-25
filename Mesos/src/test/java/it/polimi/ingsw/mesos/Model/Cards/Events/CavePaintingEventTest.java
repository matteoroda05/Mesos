package it.polimi.ingsw.mesos.Model.Cards.Events;

import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CavePaintingEventTest {
    private CavePaintingEvent cave_painting_event;
    
    @BeforeEach
    void setUp() {
        cave_painting_event = new CavePaintingEvent(Era.THIRD, 3, 3, -2);
    }
    
    @Test
    void constructorInitialisesCorrectly() {
        assertEquals(Era.THIRD, cave_painting_event.getEra());
        assertEquals(BuildingTrigger.ON_EVENT_CAVE_PAINTINGS, cave_painting_event.asTrigger());
    }

    @Test
    void constructorThrowsCorrectly(){
        assertThrows(IllegalArgumentException.class, () -> new CavePaintingEvent(Era.THIRD, 0, 3, -2));
        assertThrows(IllegalArgumentException.class, () -> new CavePaintingEvent(Era.THIRD, 3, 5, -2));
        assertThrows(IllegalArgumentException.class, () -> new CavePaintingEvent(Era.THIRD, 3, 3, -1));
    }

    @Test
    void calcOutComeReturnsCorrectly() {
        Tribe tribe1 = new Tribe();
        Tribe tribe2 = new Tribe();
        Tribe tribe3 = new Tribe();

        ArtistCard artist1 = new ArtistCard(Era.SECOND, 2);
        ArtistCard artist2 = new ArtistCard(Era.SECOND, 2);
        ArtistCard artist3 = new ArtistCard(Era.FIRST, 2);
        ArtistCard artist4 = new ArtistCard(Era.THIRD, 2);
        ArtistCard artist5 = new ArtistCard(Era.THIRD, 2);
        ArtistCard artist6 = new ArtistCard(Era.FIRST, 2);

        tribe2.addCard(artist1);
        tribe2.addCard(artist2);
        tribe2.addCard(artist3);
        tribe2.addCard(artist4);
        tribe3.addCard(artist5);
        tribe3.addCard(artist6);

        List<Tribe> tribes = List.of(tribe1, tribe2, tribe3);
        List<Integer> output = cave_painting_event.calculateOutcome(tribes);
        List<Integer> expected = List.of(-2, 12, -2);

        assertIterableEquals(expected, output);
    }
}
