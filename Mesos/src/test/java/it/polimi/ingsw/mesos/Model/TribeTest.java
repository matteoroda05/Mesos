package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TribeTest {


    private Tribe tribe;
    private HunterCard hunter1;
    private BuilderCard builder1;
    private ArtistCard artist1;
    private GathererCard gatherer1;
    private InventorCard inventor1;
    private ShamanCard shaman1;
    private BuildingCard building1;

    @BeforeEach
    void setUp() {
        tribe = new Tribe();
        hunter1 = new HunterCard(Era.FIRST,2,true);
        builder1 = new BuilderCard(Era.SECOND,2,3,3);
        artist1 = new ArtistCard(Era.THIRD,2);
        gatherer1 = new GathererCard(Era.FIRST,2);
        inventor1 = new InventorCard(Era.SECOND,2, InventionIcon.ARROW);
        shaman1 = new ShamanCard(Era.FIRST,2,3);
        building1 = new BuildingEndGame(Era.FIRST,2,6,5, false, 0, 0, null, 25);
    }

    @Test
    void constructorInitializesEmptyTribe() {

        assertEquals(0, tribe.getNumCharacters());
        assertEquals(0, tribe.getNumBuildings());

        for (Integer num_characters : tribe.getNumCharactersOfEachType().values()) {
            assertEquals(0, num_characters);
        }

        assertEquals(0, tribe.buildingsPrestige());
        assertEquals(0, tribe.buildingsEffectPrestige());
        assertEquals(0, tribe.countShamanStars());
        assertEquals(0, tribe.buildersDiscount());
        assertEquals(0, tribe.gatherersDiscount());
        assertTrue(tribe.inventionIcons().isEmpty());
        assertEquals(0, tribe.tribeEndGamePrestige());
    }

    @Test
    void addCharacterCardIncreasesTotalCharactersAndTypeCount() {

        tribe.addCard(hunter1);

        assertEquals(1, tribe.getNumCharacters());
        assertEquals(1, tribe.getNumCharacterOfType(HunterCard.class));
        assertEquals(0, tribe.getNumCharacterOfType(BuilderCard.class));
    }

    @Test
    void addBuildingCardIncreasesBuildingCount() {

        tribe.addCard(building1);

        assertEquals(1, tribe.getNumBuildings());
    }

    @Test
    void getBuildingsSameTrigger() {
        BuildingCard building2 = new BuildingEndGame(Era.FIRST,2,4,6, true, 0, 0, null, 0);
        BuildingCard building3 = new BuildingOnCard(Era.FIRST,2,4,6, 5, true, false);

        tribe.addCard(building1);
        tribe.addCard(building2);
        tribe.addCard(building3);

        List<BuildingCard> result = tribe.getBuildingsWithTrigger(BuildingTrigger.ON_END_GAME);

        assertEquals(2, result.size());
        assertTrue(result.contains(building1));
        assertTrue(result.contains(building2));
        assertFalse(result.contains(building3));
    }

    @Test
    void buildingsPrestigeSumsPrestigeOfAllBuildings() {
        BuildingCard building2 = new BuildingEndGame(Era.FIRST,2,4,6, false, 6, 0, null, 0);

        tribe.addCard(building1);
        tribe.addCard(building2);

        assertEquals(11, tribe.buildingsPrestige());
    }

    @Test

    void countShamanStars(){
        assertEquals(0, tribe.countShamanStars());
        tribe.addCard(shaman1);
        ShamanCard shaman2 = new ShamanCard(Era.FIRST,2,1);
        tribe.addCard(shaman2);
        assertEquals(4,tribe.countShamanStars());
    }

    @Test
    void buildersDiscount(){
        BuilderCard builder2 = new BuilderCard(Era.FIRST,2,4,3);

        assertEquals(0, tribe.buildersDiscount());
        tribe.addCard(builder1);
        tribe.addCard(builder2);
        assertEquals(6,tribe.buildersDiscount());
    }

    @Test
    void gatherersDiscount(){
        GathererCard gatherer2 = new GathererCard(Era.FIRST,2);

        assertEquals(0, tribe.gatherersDiscount());
        tribe.addCard(gatherer1);
        tribe.addCard(gatherer2);
        assertEquals(6,tribe.gatherersDiscount());
    }

    @Test
    void inventionIconsReturnsDistinctIconsOnly() {

        InventorCard inventor2 = new InventorCard(Era.SECOND,2, InventionIcon.ARROW);
        InventorCard inventor3 = new InventorCard(Era.SECOND,2, InventionIcon.BOAT);

        tribe.addCard(inventor1);
        tribe.addCard(inventor2);
        tribe.addCard(inventor3);

        Set<InventionIcon> icons = tribe.inventionIcons();

        assertEquals(2, icons.size());
        assertTrue(icons.contains(InventionIcon.ARROW));
        assertTrue(icons.contains(InventionIcon.BOAT));
    }

    @Test
    void calculateEndGamePrestige() {
        InventorCard inventor2 = new InventorCard(Era.SECOND,2, InventionIcon.BOAT);
        ArtistCard artist2 = new ArtistCard(Era.FIRST,2);

        tribe.addCard(gatherer1);
        tribe.addCard(artist1);
        tribe.addCard(builder1);
        tribe.addCard(shaman1);
        tribe.addCard(hunter1);
        tribe.addCard(inventor1);
        tribe.addCard(building1);
        tribe.addCard(inventor2);
        tribe.addCard(artist2);

        assertEquals(22,tribe.tribeEndGamePrestige());
    }






}