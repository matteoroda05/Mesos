package it.polimi.ingsw.mesos.model.cards.characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.model.enums.Era;

public class GathererCardTest {

    @Test
    void constructorTestDiscount(){
        GathererCard gatherer = new GathererCard(Era.SECOND, 0);
        assertEquals(3, gatherer.getDiscount());
    }
}
