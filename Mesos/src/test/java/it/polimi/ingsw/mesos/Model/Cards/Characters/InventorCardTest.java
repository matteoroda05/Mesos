package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class InventorCardTest {

    @Test
    void getCharacterTypeReturnsInventor(){
        InventorCard inventor = new InventorCard(Era.SECOND, 0, InventionIcon.BOAT);
        assertEquals(CharacterType.INVENTOR, inventor.getCharacterType());
    }

    @Test
    void constructorTestInventionIconBoat(){
        InventorCard inventorBoat = new InventorCard(Era.FIRST, 2, InventionIcon.BOAT);
        assertEquals(InventionIcon.BOAT, inventorBoat.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconArrow(){
        InventorCard inventorArrow = new InventorCard(Era.THIRD, 4, InventionIcon.ARROW);
        assertEquals(InventionIcon.ARROW, inventorArrow.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconHook(){
        InventorCard inventorHook = new InventorCard(Era.FIRST, 0, InventionIcon.HOOK);
        assertEquals(InventionIcon.HOOK, inventorHook.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconNecklace(){
        InventorCard inventorNecklace = new InventorCard(Era.SECOND, 2, InventionIcon.NECKLACE);
        assertEquals(InventionIcon.NECKLACE, inventorNecklace.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconBowl(){
        InventorCard inventorBowl = new InventorCard(Era.THIRD, 0, InventionIcon.BOWL);
        assertEquals(InventionIcon.BOWL, inventorBowl.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconRope(){
        InventorCard inventorRope = new InventorCard(Era.FIRST, 4, InventionIcon.ROPE);
        assertEquals(InventionIcon.ROPE, inventorRope.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconDoll(){
        InventorCard inventorDoll = new InventorCard(Era.SECOND, 2, InventionIcon.DOLL);
        assertEquals(InventionIcon.DOLL, inventorDoll.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconStick(){
        InventorCard inventorStick = new InventorCard(Era.THIRD, 0, InventionIcon.STICK);
        assertEquals(InventionIcon.STICK, inventorStick.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconLeather(){
        InventorCard inventorLeather = new InventorCard(Era.FIRST, 2, InventionIcon.LEATHER);
        assertEquals(InventionIcon.LEATHER, inventorLeather.getInventionIcon());
    }

    @Test
    void constructorTestInventionIconBread(){
        InventorCard inventorBread = new InventorCard(Era.SECOND, 4, InventionIcon.BREAD);
        assertEquals(InventionIcon.BREAD, inventorBread.getInventionIcon());
    }

}
