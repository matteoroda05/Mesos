package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public class GathererCardTest {
    private GathererCard gatherer;

    @BeforeEach
    void setUp(){
        this.gatherer = new GathererCard(Era.SECOND, 0);
    }

    @Test
    void getCharacterTypeReturnsGatherer(){
        assertEquals(CharacterType.GATHERER, gatherer.getCharacterType());
    }

    @Test
    void constructorTestDiscount(){
        assertEquals(3, gatherer.getDiscount());
    }

    @Test
    void constructorTestThrowsExceptionForZeroDiscount(){
        assertThrows(IllegalArgumentException.class,
                () -> new GathererCard(Era.FIRST, 0));
    }

    @Test
    void constructorTestThrowsExceptionForNegativeDiscount(){
        assertThrows(IllegalArgumentException.class,
                () -> new GathererCard(Era.SECOND, 2));
    }
}