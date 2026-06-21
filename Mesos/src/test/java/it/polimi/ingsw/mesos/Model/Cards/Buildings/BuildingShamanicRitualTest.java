package it.polimi.ingsw.mesos.model.cards.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;

import it.polimi.ingsw.mesos.model.Result;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.enums.EventPhase;

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
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 3, 0);

        assertEquals(3, building.getStarBonus());
    }

    @Test
    void postEventWinEffectDoublesPointsForASingleWinner() {
        Fixture fixture = createFixture();
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 2);
        Map<Color, Result> values = new EnumMap<>(Color.class);
        values.put(Color.BLUE, new Result(0,6,0));
        values.put(Color.YELLOW, new Result(0,0,0));

        building.setOwner(fixture.owner());
        building.effect(values);

        assertEquals(12, values.get(Color.BLUE).prestige());
    }

    @Test
    void postEventWinEffectDoesNothingWithMultipleWinners() {
        Fixture fixture = createFixture();
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 2);
        Map<Color, Result> values = new EnumMap<>(Color.class);
        values.put(Color.BLUE, new Result(0, 6, 0));
        values.put(Color.YELLOW, new Result(0, 1, 0));

        building.setOwner(fixture.owner());
        building.effect(values);

        assertEquals(6, values.get(Color.BLUE).prestige());
    }

    @Test
    void lastEffectCancelsOwnersPenaltyWheneverTheOwnerLoses() {
        Fixture fixture = createFixture();
        BuildingShamanicRitual building = new BuildingShamanicRitual(Era.SECOND, 2, 3, 4, 0, 0);
        Map<Color, Result> singleLoser = new EnumMap<>(Color.class);
        singleLoser.put(Color.BLUE, new Result(0, -5, 0));
        singleLoser.put(Color.YELLOW, new Result(0, 8, 0));
        Map<Color, Result> multipleLosers = new EnumMap<>(Color.class);
        multipleLosers.put(Color.BLUE, new Result(0, -5, 0));
        multipleLosers.put(Color.YELLOW, new Result(0, -3, 0));

        building.setOwner(fixture.owner());
        building.effect(singleLoser);
        building.effect(multipleLosers);

        assertEquals(0, singleLoser.get(Color.BLUE).prestige());
        assertEquals(0, multipleLosers.get(Color.BLUE).prestige());
    }
}
