package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class GathererCardTest {

    @Test
    void constructorTestDiscount(){
        GathererCard gatherer = new GathererCard(Era.SECOND, 0);
        assertEquals(3, gatherer.getDiscount());
    }
}