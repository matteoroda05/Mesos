package it.polimi.ingsw.mesos.model.cards.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.model.cards.characters.ArtistCard;
import it.polimi.ingsw.mesos.model.cards.characters.BuilderCard;
import it.polimi.ingsw.mesos.model.cards.CharacterCard;
import it.polimi.ingsw.mesos.model.cards.characters.GathererCard;
import it.polimi.ingsw.mesos.model.cards.characters.HunterCard;
import it.polimi.ingsw.mesos.model.cards.characters.InventorCard;
import it.polimi.ingsw.mesos.model.cards.characters.ShamanCard;
import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.enums.InventionIcon;
import it.polimi.ingsw.mesos.model.Tribe;

class BuildingOnCardTest extends BuildingTestSupport {

    @Test
    void constructorInitializesAcquireCharacterTrigger() {
        BuildingOnCard building = new BuildingOnCard(Era.FIRST, 2, 3, 4, 5, true, false);

        assertEquals(BuildingTrigger.ON_ACQUIRE_CHARACTER, building.getTrigger());
        assertEquals(3, building.getFoodCost());
        assertEquals(4, building.getPrestigePoints());
    }

    @Test
    void constructorRejectsInvalidConfigurations() {
        assertThrows(IllegalArgumentException.class, () -> new BuildingOnCard(Era.FIRST, 2, 3, 4, 0, true, false));
        assertThrows(IllegalArgumentException.class, () -> new BuildingOnCard(Era.FIRST, 2, 3, 4, 6, true, false));
        assertThrows(IllegalArgumentException.class, () -> new BuildingOnCard(Era.FIRST, 2, 3, 4, 5, true, true));
        assertThrows(IllegalArgumentException.class, () -> new BuildingOnCard(Era.FIRST, 2, 3, 4, 5, false, false));
    }

    @Test
    void setCompletionEffectRewardsTheNewUniqueMinorityType() {
        Fixture fixture = createFixture();
        BuildingOnCard building = new BuildingOnCard(Era.FIRST, 2, 3, 4, 5, true, false);
        Tribe tribe = fixture.owner().getTribe();

        addTwoCardsOfEachNonHunterType(tribe);
        building.setOwner(fixture.owner());

        CharacterCard acquired = new HunterCard(Era.FIRST, 2, true);
        tribe.addCard(acquired);
        int foodBefore = fixture.owner().getFood();

        building.effect(acquired, fixture.controller());

        assertEquals(foodBefore + 5, fixture.owner().getFood());
    }

    @Test
    void setCompletionEffectRewardsWhenTheAcquiredTypeWasTheOnlyMissingOne() {
        Fixture fixture = createFixture();
        BuildingOnCard building = new BuildingOnCard(Era.FIRST, 2, 3, 4, 5, true, false);
        Tribe tribe = fixture.owner().getTribe();

        tribe.addCard(new ArtistCard(Era.FIRST, 2));
        tribe.addCard(new BuilderCard(Era.FIRST, 2, 2, 1));
        tribe.addCard(new GathererCard(Era.FIRST, 2));
        tribe.addCard(new InventorCard(Era.FIRST, 2, InventionIcon.BOAT));
        tribe.addCard(new ShamanCard(Era.FIRST, 2, 2));
        building.setOwner(fixture.owner());

        CharacterCard acquired = new HunterCard(Era.FIRST, 2, false);
        tribe.addCard(acquired);
        int foodBefore = fixture.owner().getFood();

        building.effect(acquired, fixture.controller());

        assertEquals(foodBefore + 5, fixture.owner().getFood());
    }

    @Test
    void inventorPairEffectRewardsDuplicateIconsOnly() {
        Fixture fixture = createFixture();
        BuildingOnCard building = new BuildingOnCard(Era.FIRST, 2, 3, 4, 3, false, true);
        Tribe tribe = fixture.owner().getTribe();

        tribe.addCard(new InventorCard(Era.FIRST, 2, InventionIcon.BOAT));
        building.setOwner(fixture.owner());
        int foodBefore = fixture.owner().getFood();

        building.effect(new InventorCard(Era.FIRST, 2, InventionIcon.BOAT), fixture.controller());

        assertEquals(foodBefore + 3, fixture.owner().getFood());
    }

    @Test
    void inventorPairEffectIgnoresNonInventors() {
        Fixture fixture = createFixture();
        BuildingOnCard building = new BuildingOnCard(Era.FIRST, 2, 3, 4, 3, false, true);
        building.setOwner(fixture.owner());
        int foodBefore = fixture.owner().getFood();

        building.effect(new ArtistCard(Era.FIRST, 2), fixture.controller());

        assertEquals(foodBefore, fixture.owner().getFood());
    }

    private void addTwoCardsOfEachNonHunterType(Tribe tribe) {
        for (int i = 0; i < 2; i++) {
            tribe.addCard(new ArtistCard(Era.FIRST, 2));
            tribe.addCard(new BuilderCard(Era.FIRST, 2, 2, 1));
            tribe.addCard(new GathererCard(Era.FIRST, 2));
            tribe.addCard(new ShamanCard(Era.FIRST, 2, 2));
        }
        tribe.addCard(new InventorCard(Era.FIRST, 2, InventionIcon.BOAT));
        tribe.addCard(new InventorCard(Era.FIRST, 2, InventionIcon.ARROW));
    }
}
