package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingBonusEvent;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndTurn;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.GathererCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ShamanCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.CavePaintingEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardDescriptionVisitorImplTest {

    private final CardDescriptionVisitor visitor = CardDescriptionVisitorImpl.INSTANCE;

    @Test
    void describesCharacterCards() {
        assertTrue(new BuilderCard(Era.FIRST, 2, 1, 2).accept(visitor).contains("discount"));
        assertTrue(new GathererCard(Era.FIRST, 2).accept(visitor).contains("Sustenance discount"));
        assertEquals("Active hunter", new HunterCard(Era.FIRST, 2, true).accept(visitor));
        assertEquals("Hunter", new HunterCard(Era.FIRST, 2, false).accept(visitor));
        assertTrue(new InventorCard(Era.FIRST, 2, InventionIcon.BOAT).accept(visitor).contains("BOAT"));
        assertTrue(new ShamanCard(Era.FIRST, 2, 3).accept(visitor).contains("3 shamanic stars"));
        assertEquals("", new ArtistCard(Era.FIRST, 2).accept(visitor));
    }

    @Test
    void describesEventCards() {
        assertTrue(new HuntEvent(Era.FIRST, 2, 1).accept(visitor).contains("per hunter"));
        assertTrue(new CavePaintingEvent(Era.FIRST, 2, 1, -2).accept(visitor).contains("artists"));
        assertTrue(new ShamanicRitualEvent(Era.FIRST, 5, -3).accept(visitor).contains("Winners"));
        assertTrue(new SustenanceEvent(Era.FIRST, -1, 1).accept(visitor).contains("food per character"));
    }

    @Test
    void describesBuildingBonusEventAndOnCardAndEndTurn() {
        assertTrue(new BuildingBonusEvent(Era.FIRST, 2, 1, 1, 1, 1, HunterCard.class,
                BuildingTrigger.ON_EVENT_HUNT).accept(visitor).contains("Hunter"));
        assertTrue(new BuildingEndTurn(Era.FIRST, 2, 1, 1, 1).accept(visitor).contains("End turn"));
        assertTrue(new BuildingOnCard(Era.FIRST, 2, 1, 1, 2, true, false)
                .accept(visitor).contains("complete a set"));
        assertTrue(new BuildingOnCard(Era.FIRST, 2, 1, 1, 2, false, true)
                .accept(visitor).contains("Inventor pair"));
    }

    @Test
    void describesAllShamanicRitualBuildingVariants() {
        assertTrue(new BuildingShamanicRitual(Era.FIRST, 2, 1, 1, 3, 0).accept(visitor).contains("+3"));
        assertTrue(new BuildingShamanicRitual(Era.FIRST, 2, 1, 1, 0, 2).accept(visitor).contains("x2"));
        assertTrue(new BuildingShamanicRitual(Era.FIRST, 2, 1, 1, 0, 0).accept(visitor).contains("cancel"));
    }

    @Test
    void describesAllEndGameBuildingVariants() {
        assertTrue(new BuildingEndGame(Era.FIRST, 2, 1, 1, true, 0, 0, null, 0)
                .accept(visitor).contains("double"));
        assertTrue(new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 5, 0, null, 0)
                .accept(visitor).contains("complete character set"));
        assertTrue(new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 0, 2, HunterCard.class, 0)
                .accept(visitor).contains("Hunter"));
        assertTrue(new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 0, 0, null, 25)
                .accept(visitor).contains("+25"));
    }

    @Test
    void describesBuildingAfterEveryoneAsEmpty() {
        assertEquals("", new BuildingAfterEveryone(Era.FIRST, 2, 1, 1).accept(visitor));
    }

    @Test
    void formatNameSplitsCamelCaseAndStripsSuffixes() {
        assertEquals("Shamanic Ritual Event", CardDescriptionVisitorImpl.formatName("ShamanicRitualEvent"));
        assertEquals("Builder", CardDescriptionVisitorImpl.formatName("BuilderCard"));
    }

    @Test
    void signedPrependsPlusForNonNegativeValues() {
        assertEquals("+2 food", CardDescriptionVisitorImpl.signed(2, " food"));
        assertEquals("-1 food", CardDescriptionVisitorImpl.signed(-1, " food"));
    }
}
