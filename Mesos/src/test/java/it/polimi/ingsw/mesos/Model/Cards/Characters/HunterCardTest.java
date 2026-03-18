package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class HunterCardTest {
    @Test
    void constructorTestActiveHunterTrue(){
        HunterCard active_hunter = new HunterCard(Era.FIRST, 2, true);
        assertTrue(active_hunter.isActiveHunter());
    }

    @Test
    void constructorTestActiveHunterFalse(){
        HunterCard inactive_hunter = new HunterCard(Era.THIRD, 4, false);
        assertFalse(inactive_hunter.isActiveHunter());
    }
}
