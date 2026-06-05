package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingBonusEvent;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.CavePaintingEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelValidationEdgesTest {
    @Test
    void eventConstructorsRejectBothSidesOfBoundedRanges() {
        assertThrows(IllegalArgumentException.class, () -> new CavePaintingEvent(Era.FIRST, 0, 1, -2));
        assertThrows(IllegalArgumentException.class, () -> new CavePaintingEvent(Era.FIRST, 4, 1, -2));
        assertThrows(IllegalArgumentException.class, () -> new CavePaintingEvent(Era.FIRST, 1, 0, -2));
        assertThrows(IllegalArgumentException.class, () -> new CavePaintingEvent(Era.FIRST, 1, 4, -2));
        assertThrows(IllegalArgumentException.class, () -> new HuntEvent(Era.FIRST, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new HuntEvent(Era.FIRST, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> new SustenanceEvent(Era.FIRST, -4, 1));
        assertThrows(IllegalArgumentException.class, () -> new SustenanceEvent(Era.FIRST, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShamanicRitualEvent(Era.FIRST, 4, -3));
        assertThrows(IllegalArgumentException.class, () -> new ShamanicRitualEvent(Era.FIRST, 16, -3));
        assertThrows(IllegalArgumentException.class, () -> new ShamanicRitualEvent(Era.FIRST, 5, -8));
        assertThrows(IllegalArgumentException.class, () -> new ShamanicRitualEvent(Era.FIRST, 5, -2));
    }

    @Test
    void buildingConstructorsRejectBothSidesOfBoundedRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingBonusEvent(Era.FIRST, 2, 1, 1, -2, 0, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingBonusEvent(Era.FIRST, 2, 1, 1, 2, 0, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingBonusEvent(Era.FIRST, 2, 1, 1, 0, -1, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingBonusEvent(Era.FIRST, 2, 1, 1, 0, 2, HunterCard.class, BuildingTrigger.ON_EVENT_HUNT));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingEndGame(Era.FIRST, 2, 1, 1, false, -1, 0, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 9, 0, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 0, -1, HunterCard.class, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 0, 6, HunterCard.class, 0));
    }

    @Test
    void tribeRejectsNullAndUnknownTypes() {
        Tribe tribe = new Tribe();

        assertThrows(IllegalArgumentException.class, () -> tribe.getNumCharacterOfType(null));
        assertThrows(IllegalStateException.class, () -> tribe.getNumCharacterOfType(UnknownCharacterCard.class));
        assertThrows(IllegalArgumentException.class, () -> tribe.getBuildingsWithTrigger(null));
        assertThrows(IllegalArgumentException.class, () -> tribe.addCard((CharacterCard) null));
        assertThrows(NullPointerException.class, () -> tribe.addCard(new UnknownCharacterCard()));
        assertThrows(IllegalArgumentException.class, () -> tribe.addCard((BuildingCard) null));
    }

    @Test
    void deckConstructorsAndStateGuardsRejectInvalidEdges() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new TribeDeck(null, 2));
        assertThrows(IllegalArgumentException.class, () -> new TribeDeck(1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TribeDeck(6, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TribeDeck(2, null));

        BuildingDeck emptyDeck = new BuildingDeck(List.of());
        assertThrows(IllegalAccessError.class, emptyDeck::drawBuildingCards);

        BuildingDeck nullDeck = new BuildingDeck(List.of());
        Field deckField = BuildingDeck.class.getDeclaredField("deck");
        deckField.setAccessible(true);
        deckField.set(nullDeck, null);
        assertThrows(IllegalStateException.class, nullDeck::drawBuildingCards);
        assertThrows(IllegalStateException.class, nullDeck::isEmpty);
    }

    private static final class UnknownCharacterCard extends CharacterCard {
        private UnknownCharacterCard() {
            super(Era.FIRST, 2);
        }
    }
}
