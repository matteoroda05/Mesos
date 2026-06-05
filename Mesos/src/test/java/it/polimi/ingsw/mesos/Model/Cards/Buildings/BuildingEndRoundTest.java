package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import it.polimi.ingsw.mesos.Controller.States.ActionState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;

import static org.junit.jupiter.api.Assertions.*;

class BuildingEndRoundTest extends BuildingTestSupport {

    @Test
    void constructorInitializesEndRoundTrigger() {
        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.SECOND, 2, 3, 4);

        assertEquals(BuildingTrigger.ON_AFTER_EVERYONE, building.getTrigger());
    }

    @Test
    void effectCreatesExtraTopActionWhenTheTopRowHasTakeableCards() {
        Fixture fixture = createFixture();
        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.SECOND, 2, 3, 4);

        building.setOwner(fixture.owner());

        ActionState action = building.effect(fixture.table(), fixture.controller());

        assertInstanceOf(ActionState.class, action);
        assertEquals(fixture.owner().getColor(), action.getActivePlayer());
        assertEquals(1, action.getRemainingTopChoices());
        assertEquals(0, action.getRemainingBottomChoices());
        assertFalse(building.isActive());
    }

    @Test
    void effectReturnsNullWhenNoTopRowCardIsTakeableAndResetReactivates() {
        Fixture fixture = createFixture();
        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.SECOND, 2, 3, 4);

        fixture.owner().loseFood(fixture.owner().getFood());
        fixture.table().getTopRow().removeTribeCards();
        building.setOwner(fixture.owner());

        assertNull(building.effect(fixture.table(), fixture.controller()));
        assertFalse(building.isActive());

        building.reset();

        assertTrue(building.isActive());
    }
}
