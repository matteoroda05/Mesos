package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
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
        Map<Color, int[]> output = new EnumMap<>(Color.class);
        output.put(Color.BLUE, new int[]{4, 3});
        output.put(Color.YELLOW, new int[]{2, 1});

        building.effect(output, fixture.controller());

        assertEquals(foodBefore + 5, fixture.owner().getFood());
        assertEquals(2, fixture.owner().getPrestigePoints());
    }

    @Test
    void effectKeepsOnlyTheBaseFoodDeltaWhenOwnerHasNoMatchingCharacters() {
        Fixture fixture = createFixture();
        BuildingBonusEvent building = new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 1, 1, ArtistCard.class, BuildingTrigger.ON_EVENT_CAVE_PAINTINGS);

        building.setOwner(fixture.owner());
        Map<Color, int[]> output = new EnumMap<>(Color.class);
        output.put(Color.BLUE, new int[]{0, 4});
        output.put(Color.YELLOW, new int[]{0, 2});
        building.effect(output, fixture.controller());

        assertEquals(14, fixture.owner().getFood());
        assertEquals(0, fixture.owner().getPrestigePoints());
    }
}
