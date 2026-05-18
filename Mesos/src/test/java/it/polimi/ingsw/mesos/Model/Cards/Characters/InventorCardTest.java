package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class InventorCardTest {
    @Test
    void constructorStoresAnyInventionIcon(){
        for (InventionIcon icon : InventionIcon.values()) {
            assertEquals(icon, new InventorCard(Era.FIRST, 2, icon).getInventionIcon());
        }
    }

    @Test
    void constructorTestThrowsOnNullIcon(){
        assertThrows(IllegalArgumentException.class,
                () -> new InventorCard(Era.SECOND, 4, null));
    }
}
