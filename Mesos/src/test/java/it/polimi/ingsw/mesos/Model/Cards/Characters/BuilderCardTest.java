package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class BuilderCardTest {

    @Test
    void constructorTestInitialisation() {
        BuilderCard card = new BuilderCard(Era.SECOND, 3, 5, 2);
        assertEquals(5, card.getPrestigePoints());
        assertEquals(2, card.getBuildingDiscount());
    }

    @Test
    void constructorTestThrowsExceptionNegativePrestigePoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new BuilderCard(Era.FIRST, 2, -1, 2));
    }

    @Test
    void constructorTestThrowsExceptionTooManyPrestigePoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new BuilderCard(Era.FIRST, 2, 6, 2));
    }

    @Test
    void constructorTestThrowsExceptionNoDiscount() {
        assertThrows(IllegalArgumentException.class,
                () -> new BuilderCard(Era.FIRST, 2, 5, 0));
    }

    @Test
    void constructorTestThrowsExceptionTooMuchDiscount() {
        assertThrows(IllegalArgumentException.class,
                () -> new BuilderCard(Era.FIRST, 2, 2, 3));
    }
}
