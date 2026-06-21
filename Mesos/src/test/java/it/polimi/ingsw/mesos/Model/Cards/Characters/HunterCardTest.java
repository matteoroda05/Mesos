package it.polimi.ingsw.mesos.model.cards.characters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.model.enums.Era;

public class HunterCardTest {
    @Test
    void constructorStoresActiveHunterFlag(){
        HunterCard active_hunter = new HunterCard(Era.FIRST, 2, true);
        HunterCard inactive_hunter = new HunterCard(Era.THIRD, 4, false);

        assertTrue(active_hunter.isActiveHunter());
        assertFalse(inactive_hunter.isActiveHunter());
    }
}
