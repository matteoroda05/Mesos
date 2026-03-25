package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;

class BuildingBonusEventTest extends BuildingTestSupport {

    @Test
    void constructorStoresEventTriggerAndBonuses() {
        BuildingBonusEvent building = new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 1, 1, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT);

        assertEquals(BuildingTrigger.ON_EVENT_HUNT, building.getTrigger());
        assertEquals(4, building.getFoodCost());
        assertEquals(3, building.getPrestigePoints());
    }

    @Test
    void constructorRejectsInvalidConfigurations() {
        assertThrows(IllegalArgumentException.class, () -> new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 0, 0, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT));
        assertThrows(IllegalArgumentException.class, () -> new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 2, 0, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT));
        assertThrows(IllegalArgumentException.class, () -> new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 0, 2, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT));
        assertThrows(IllegalArgumentException.class, () -> new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 1, 1, HunterCard.class, BuildingTrigger.ON_END_GAME));
    }

    @Test
    void effectRewardsFoodAndPrestigePerMatchingCharacter() {
        Fixture fixture = createFixture();
        BuildingBonusEvent building = new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 1, 1, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT);

        fixture.owner().getTribe().addCard(new HunterCard(Era.FIRST, 2, true));
        fixture.owner().getTribe().addCard(new HunterCard(Era.FIRST, 2, false));
        building.setOwner(fixture.owner());
        int foodBefore = fixture.owner().getFood();

        building.effect(Map.of(Color.BLUE, 3, Color.YELLOW, 1), fixture.controller());

        assertEquals(foodBefore + 6, fixture.owner().getFood());
        assertEquals(2, fixture.owner().getPrestigePoints());
    }

    @Test
    void effectDoesNothingWhenOwnerHasNoMatchingCharacters() {
        Fixture fixture = createFixture();
        BuildingBonusEvent building = new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 1, 1, ArtistCard.class, BuildingTrigger.ON_EVENT_CAVE_PAINTINGS);

        building.setOwner(fixture.owner());
        building.effect(Map.of(Color.BLUE, 4, Color.YELLOW, 2), fixture.controller());

        assertEquals(10, fixture.owner().getFood());
        assertEquals(0, fixture.owner().getPrestigePoints());
    }
}
