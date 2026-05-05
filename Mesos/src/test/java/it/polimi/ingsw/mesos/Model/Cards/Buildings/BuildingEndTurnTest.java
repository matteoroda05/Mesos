package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

class BuildingEndTurnTest extends BuildingTestSupport {

    @Test
    void constructorRejectsInvalidFoodBonus() {
        assertThrows(IllegalArgumentException.class, () -> new BuildingEndTurn(Era.FIRST, 2, 3, 4, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuildingEndTurn(Era.FIRST, 2, 3, 4, 2));
    }

    @Test
    void effectAddsExtraFoodWhenOwnerReturnsToPositiveBonusSpace() {
        Fixture fixture = createFixture(1, 0);
        BuildingEndTurn building = new BuildingEndTurn(Era.FIRST, 2, 3, 4, 1);
        TurnOrderTile order = fixture.table().getTurnOrderTile();

        order.clear();
        building.setOwner(fixture.owner());
        order.placeTotem(fixture.owner().getColor());
        int foodBeforeEffect = fixture.owner().getFood();

        building.effect(order, fixture.controller());

        assertEquals(foodBeforeEffect + 1, fixture.owner().getFood());
    }

    @Test
    void effectDoesNothingWhenOwnerReturnsToAZeroBonusSpace() {
        Fixture fixture = createFixture(0, 1);
        BuildingEndTurn building = new BuildingEndTurn(Era.FIRST, 2, 3, 4, 1);
        TurnOrderTile order = fixture.table().getTurnOrderTile();

        order.clear();
        building.setOwner(fixture.owner());
        order.placeTotem(fixture.owner().getColor());
        int foodBeforeEffect = fixture.owner().getFood();

        building.effect(order, fixture.controller());

        assertEquals(foodBeforeEffect, fixture.owner().getFood());
    }

    @Test
    void effectThrowsWhenOwnerIsNotOnTheTurnOrderTile() {
        Fixture fixture = createFixture(0, 0);
        BuildingEndTurn building = new BuildingEndTurn(Era.FIRST, 2, 3, 4, 1);
        TurnOrderTile order = fixture.table().getTurnOrderTile();

        order.clear();
        building.setOwner(fixture.owner());
        order.placeTotem(fixture.other().getColor());

        assertThrows(IllegalStateException.class, () -> building.effect(order, fixture.controller()));
    }
}
