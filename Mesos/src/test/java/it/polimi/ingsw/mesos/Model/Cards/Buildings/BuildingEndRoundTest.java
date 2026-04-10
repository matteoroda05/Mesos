package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;

class BuildingEndRoundTest extends BuildingTestSupport {

    @Test
    void constructorInitializesEndRoundTrigger() {
        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.SECOND, 2, 3, 4);

        assertEquals(BuildingTrigger.ON_AFTER_EVERYONE, building.getTrigger());
    }

    @Test
    void effectIsSafeWhenTheTopRowHasTakeableCards() {
        Fixture fixture = createFixture();
        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.SECOND, 2, 3, 4);

        building.setOwner(fixture.owner());

        assertDoesNotThrow(() -> building.effect(fixture.table()));
    }
}
