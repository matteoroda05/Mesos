package it.polimi.ingsw.mesos.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Cards.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public class HunterCardTest {
    private HunterCard hunter;

    @BeforeEach
    void setUp(){
        this.hunter = new HunterCard(Era.SECOND, 0, true);
    }

    @Test
    void constructorTestActiveHunterTrue(){
        HunterCard activeHunter = new HunterCard(Era.FIRST, 2, true);
        assertTrue(activeHunter.isactive_hunter());
    }

    @Test
    void constructorTestActiveHunterFalse(){
        HunterCard inactiveHunter = new HunterCard(Era.THIRD, 4, false);
        assertFalse(inactiveHunter.isactive_hunter());
    }

    @Test
    void getCharacterTypeReturnsHunter(){
        assertEquals(CharacterType.HUNTER, hunter.getCharacterType());
    }
}
