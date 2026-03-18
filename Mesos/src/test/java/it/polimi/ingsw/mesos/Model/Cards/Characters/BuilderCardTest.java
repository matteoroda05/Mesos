package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class BuilderCardTest {

    @Test
    void getCharacterTypeReturnsBuilder(){
        BuilderCard builder = new BuilderCard(Era.SECOND, 0, 5, 2);
        assertEquals(CharacterType.BUILDER, builder.getCharacterType());
    }

    @Test
    void constructorTestPrestigePointsZero(){
        BuilderCard builderZeroPrestige = new BuilderCard(Era.FIRST, 2, 0, 1);
        assertEquals(0, builderZeroPrestige.getPrestigePoints());
    }

    @Test
    void constructorTestPrestigePointsPositive(){
        BuilderCard builderPositivePrestige = new BuilderCard(Era.SECOND, 0, 5, 3);
        assertEquals(5, builderPositivePrestige.getPrestigePoints());
    }

    @Test
    void constructorTestFoodDiscountZero(){
        BuilderCard builderZeroDiscount = new BuilderCard(Era.THIRD, 4, 5, 0);
        assertEquals(0, builderZeroDiscount.getBuildingDiscount());
    }

    @Test
    void constructorTestFoodDiscountPositive(){
        BuilderCard builderPositiveDiscount = new BuilderCard(Era.FIRST, 2, 3, 3);
        assertEquals(3, builderPositiveDiscount.getBuildingDiscount());
    }

    @Test
    void constructorTestBothZero(){
        BuilderCard builderBothZero = new BuilderCard(Era.SECOND, 0, 0, 0);
        assertEquals(0, builderBothZero.getPrestigePoints());
        assertEquals(0, builderBothZero.getBuildingDiscount());
    }

    @Test
    void constructorTestThrowsExceptionForNegativePrestige(){
        assertThrows(IllegalArgumentException.class,
                () -> new BuilderCard(Era.FIRST, 0, -1, 2));
    }

    @Test
    void constructorTestThrowsExceptionForNegativeDiscount(){
        assertThrows(IllegalArgumentException.class,
                () -> new BuilderCard(Era.SECOND, 2, 5, -1));
    }

    @Test
    void constructorTestThrowsExceptionForBothNegative(){
        assertThrows(IllegalArgumentException.class,
                () -> new BuilderCard(Era.THIRD, 4, -3, -2));
    }
}
