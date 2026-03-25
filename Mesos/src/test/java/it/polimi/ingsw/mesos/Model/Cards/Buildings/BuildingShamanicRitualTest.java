package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.EventPhase;

class BuildingShamanicRitualTest extends BuildingTestSupport {

    @Test
    void constructorSetsTriggerAndPhaseFromConfiguredEffect() {
        BuildingShamanicRitual preEvent = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 3, 0);
        BuildingShamanicRitual postEvent = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 2);

        assertEquals(BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL, preEvent.getTrigger());
        assertEquals(EventPhase.PRE_EVENT, preEvent.getPhase());
        assertEquals(EventPhase.POST_EVENT, postEvent.getPhase());
    }

    @Test
    void constructorRejectsInvalidConfigurations() {
        assertThrows(IllegalArgumentException.class, () -> new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 1));
    }

    @Test
    void preEventEffectAddsStarsToTheOwner() {
        Fixture fixture = createFixture();
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 3, 0);
        Map<Color, Integer> values = new EnumMap<>(Color.class);
        values.put(Color.BLUE, 2);
        values.put(Color.YELLOW, 1);

        building.setOwner(fixture.owner());
        building.effect(values);

        assertEquals(5, values.get(Color.BLUE));
    }

    @Test
    void postEventWinEffectDoublesPointsForASingleWinner() {
        Fixture fixture = createFixture();
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 2);
        Map<Color, Integer> values = new EnumMap<>(Color.class);
        values.put(Color.BLUE, 6);
        values.put(Color.YELLOW, 0);

        building.setOwner(fixture.owner());
        building.effect(values);

        assertEquals(12, values.get(Color.BLUE));
    }

    @Test
    void postEventWinEffectDoesNothingWithMultipleWinners() {
        Fixture fixture = createFixture();
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 2);
        Map<Color, Integer> values = new EnumMap<>(Color.class);
        values.put(Color.BLUE, 6);
        values.put(Color.YELLOW, 1);

        building.setOwner(fixture.owner());
        building.effect(values);

        assertEquals(6, values.get(Color.BLUE));
    }

    @Test
    void lastEffectCancelsPenaltyForASingleLoserOnly() {
        Fixture fixture = createFixture();
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 0);
        Map<Color, Integer> singleLoser = new EnumMap<>(Color.class);
        singleLoser.put(Color.BLUE, -5);
        singleLoser.put(Color.YELLOW, 8);
        Map<Color, Integer> multipleLosers = new EnumMap<>(Color.class);
        multipleLosers.put(Color.BLUE, -5);
        multipleLosers.put(Color.YELLOW, -3);

        building.setOwner(fixture.owner());
        building.effect(singleLoser);
        building.effect(multipleLosers);

        assertEquals(0, singleLoser.get(Color.BLUE));
        assertEquals(-5, multipleLosers.get(Color.BLUE));
    }
}
