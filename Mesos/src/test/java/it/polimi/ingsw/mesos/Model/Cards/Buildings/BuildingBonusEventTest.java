package it.polimi.ingsw.mesos.model.cards.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;

import it.polimi.ingsw.mesos.model.Result;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.model.cards.characters.ArtistCard;
import it.polimi.ingsw.mesos.model.cards.characters.HunterCard;
import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;

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
    void effectDoesNotRewardPlayersOnEvent() {
        Fixture fixture = createFixture();
        BuildingBonusEvent building = new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 1, 1, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT);

        fixture.owner().getTribe().addCard(new HunterCard(Era.FIRST, 2, true));
        fixture.owner().getTribe().addCard(new HunterCard(Era.FIRST, 2, false));
        building.setOwner(fixture.owner());
        int foodBefore = fixture.owner().getFood();
        Map<Color, Result> output = new EnumMap<>(Color.class);
        output.put(Color.BLUE, new Result(3, 4, 0));
        output.put(Color.YELLOW, new Result(1, 2, 0));

        building.effect(output, fixture.controller());

        assertEquals(foodBefore, fixture.owner().getFood());
        assertEquals(0, fixture.owner().getPrestigePoints());
    }

    @Test
    void effectDoesNotChangePlayerFoodOnEvent() {
        Fixture fixture = createFixture();
        BuildingBonusEvent building = new BuildingBonusEvent(
                Era.SECOND, 2, 4, 3, 1, 1, ArtistCard.class, BuildingTrigger.ON_EVENT_CAVE_PAINTINGS);

        building.setOwner(fixture.owner());
        Map<Color, Result> output = new EnumMap<>(Color.class);
        output.put(Color.BLUE, new Result(4, 0, 0));
        output.put(Color.YELLOW, new Result(2, 0, 0));
        building.effect(output, fixture.controller());

        assertEquals(10, fixture.owner().getFood());
        assertEquals(0, fixture.owner().getPrestigePoints());
    }
}
