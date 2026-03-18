package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class ShamanCardTest {

    @Test
    void getCharacterTypeReturnsShaman(){
        ShamanCard shaman = new ShamanCard(Era.SECOND, 0, 3);
        assertEquals(CharacterType.SHAMAN, shaman.getCharacterType());
    }

    @Test
    void constructorTestOneStar(){
        ShamanCard shamanOneStar = new ShamanCard(Era.FIRST, 2, 1);
        assertEquals(1, shamanOneStar.getStars());
    }

    @Test
    void constructorTestTwoStars(){
        ShamanCard shamanTwoStars = new ShamanCard(Era.SECOND, 0, 2);
        assertEquals(2, shamanTwoStars.getStars());
    }

    @Test
    void constructorTestThreeStars(){
        ShamanCard shamanThreeStars = new ShamanCard(Era.THIRD, 4, 3);
        assertEquals(3, shamanThreeStars.getStars());
    }

    @Test
    void constructorTestThrowsExceptionForZeroStars(){
        assertThrows(IllegalArgumentException.class,
                () -> new ShamanCard(Era.FIRST, 0, 0));
    }

    @Test
    void constructorTestThrowsExceptionForNegativeStars(){
        assertThrows(IllegalArgumentException.class,
                () -> new ShamanCard(Era.SECOND, 2, -1));
    }
}
