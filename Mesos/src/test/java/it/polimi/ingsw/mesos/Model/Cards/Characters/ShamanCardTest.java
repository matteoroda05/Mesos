package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class ShamanCardTest {

    @Test
    void constructorTestStar(){
        ShamanCard shamanOneStar = new ShamanCard(Era.FIRST, 2, 1);
        assertEquals(1, shamanOneStar.getStars());
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

    @Test
    void constructorTestThrowsExceptionForMoreThanThreeStars(){
        assertThrows(IllegalArgumentException.class, () -> new ShamanCard(Era.THIRD, 4, 7));
    }
}
