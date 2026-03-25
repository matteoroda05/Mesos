package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Cards.Characters.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

class BuildingEndGameTest extends BuildingTestSupport {

    @Test
    void constructorRejectsInvalidEffectConfigurations() {
        assertThrows(IllegalArgumentException.class, () -> new BuildingEndGame(
                Era.THIRD, 2, 5, 4, true, 6, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuildingEndGame(
                Era.THIRD, 2, 5, 4, false, 9, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuildingEndGame(
                Era.THIRD, 2, 5, 4, false, 0, 6, HunterCard.class, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuildingEndGame(
                Era.THIRD, 2, 5, 4, false, 0, 0, null, 24));
    }

    @Test
    void effectAppliesFlatPrestigeBonus() {
        Fixture fixture = createFixture();
        BuildingEndGame building = new BuildingEndGame(Era.THIRD, 2, 5, 4, false, 0, 0, null, 25);

        building.setOwner(fixture.owner());
        building.effect(fixture.controller());

        assertEquals(25, fixture.owner().getPrestigePoints());
    }

    @Test
    void effectAddsBuildersPrestigeAgainWhenConfigured() {
        Fixture fixture = createFixture();
        BuildingEndGame building = new BuildingEndGame(Era.THIRD, 2, 5, 4, true, 0, 0, null, 0);

        fixture.owner().getTribe().addCard(new BuilderCard(Era.FIRST, 2, 2, 1));
        fixture.owner().getTribe().addCard(new BuilderCard(Era.FIRST, 2, 3, 1));
        building.setOwner(fixture.owner());
        building.effect(fixture.controller());

        assertEquals(5, fixture.owner().getPrestigePoints());
    }

    @Test
    void effectAddsCharacterMultiplierPrestige() {
        Fixture fixture = createFixture();
        BuildingEndGame building = new BuildingEndGame(Era.THIRD, 2, 5, 4, false, 0, 3, HunterCard.class, 0);

        fixture.owner().getTribe().addCard(new HunterCard(Era.FIRST, 2, true));
        fixture.owner().getTribe().addCard(new HunterCard(Era.FIRST, 2, false));
        building.setOwner(fixture.owner());
        building.effect(fixture.controller());

        assertEquals(6, fixture.owner().getPrestigePoints());
    }

    @Test
    void effectAddsPrestigeForCompletedCharacterSets() {
        Fixture fixture = createFixture();
        BuildingEndGame building = new BuildingEndGame(Era.THIRD, 2, 5, 4, false, 6, 0, null, 0);

        addOneOfEachCharacter(fixture.owner().getTribe());
        building.setOwner(fixture.owner());
        building.effect(fixture.controller());

        assertEquals(6, fixture.owner().getPrestigePoints());
    }
}
