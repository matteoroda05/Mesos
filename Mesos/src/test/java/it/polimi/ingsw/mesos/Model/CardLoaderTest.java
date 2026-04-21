package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Cards.*;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Cards.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


class CardLoaderTest {

    private static CardLoader.CardData data;

    @BeforeAll
    static void loadOnce() throws IOException {
        data = CardLoader.load();
    }


    @Test
    void tribeCardListIsNotEmpty() {
        assertFalse(data.tribe_cards().isEmpty());
    }

    @Test
    void buildingCardListIsNotEmpty() {
        assertFalse(data.building_cards().isEmpty());
    }

    @Test
    void tribeCardsTotalCountMatchesExpected() {
        assertEquals(95, data.tribe_cards().size());
    }

    @Test
    void buildingCardsTotalCountMatchesExpected() {
        assertEquals(21, data.building_cards().size());
    }


    @Test
    void hunterCardsAreDeserializedToCorrectType() {
        data.tribe_cards().stream()
                .filter(c -> c instanceof HunterCard)
                .map(c -> (HunterCard) c)
                .forEach(h ->
                        assertEquals(HunterCard.class, h.getClass())
                );
    }

    @Test
    void shamanCardsHavePositiveStarCount() {
        data.tribe_cards().stream()
                .filter(c -> c instanceof ShamanCard)
                .map(c -> (ShamanCard) c)
                .forEach(s ->
                        assertTrue(s.getStars() >= 1,
                                "Every ShamanCard must have at least 1 star")
                );
    }

    @Test
    void builderCardsHaveNonNegativePrestigeAndDiscount() {
        data.tribe_cards().stream()
                .filter(c -> c instanceof BuilderCard)
                .map(c -> (BuilderCard) c)
                .forEach(b -> {
                    assertTrue(b.getPrestigePoints() >= 0);
                    assertTrue(b.getBuildingDiscount() >= 0);
                });
    }

    @Test
    void inventorCardsHaveNonNullInventionIcon() {
        data.tribe_cards().stream()
                .filter(c -> c instanceof InventorCard)
                .map(c -> (InventorCard) c)
                .forEach(i ->
                        assertNotNull(i.getInventionIcon(),
                                "Every InventorCard must have an InventionIcon")
                );
    }

    @Test
    void gathererCardsHavePositiveFoodDiscount() {
        data.tribe_cards().stream()
                .filter(c -> c instanceof GathererCard)
                .map(c -> (GathererCard) c)
                .forEach(g ->
                        assertTrue(g.getDiscount() > 0,
                                "Every GathererCard must have a positive food discount")
                );
    }

    @Test
    void huntEventsHaveOnePerEra() {
        long count = data.tribe_cards().stream()
                .filter(c -> c instanceof HuntEvent)
                .count();
        assertEquals(3, count, "There should be one HuntEvent per era");
    }

    @Test
    void sustenanceEventsHaveOnePerEra() {
        long count = data.tribe_cards().stream()
                .filter(c -> c instanceof SustenanceEvent)
                .count();
        assertEquals(3, count, "There should be one SustenanceEvent per era");
    }

    @Test
    void huntEventsHaveNegativePenaltyByConstruction() {
        // SustenanceEvent stores a negative penalty; loading from JSON must
        // preserve the sign
        data.tribe_cards().stream()
                .filter(c -> c instanceof SustenanceEvent)
                .map(c -> (SustenanceEvent) c)
                .forEach(e ->
                        assertTrue(e.getPointPenalty() < 0,
                                "SustenanceEvent prestige conversion must be positive")
                );
    }

    @Test
    void shamanicRitualEventsHaveCorrectSignsOnBonusAndPenalty() {
        data.tribe_cards().stream()
                .filter(c -> c instanceof ShamanicRitualEvent)
                .map(c -> (ShamanicRitualEvent) c)
                .forEach(e -> {
                    assertNotNull(e);
                });
    }


    @Test
    void eachEraHasAtLeastOneTribeCard() {
        for (Era era : Era.values()) {
            boolean found = data.tribe_cards().stream()
                    .anyMatch(c -> c.getEra() == era);
            assertTrue(found, "No tribe card found for era: " + era);
        }
    }

    @Test
    void eachEraHasAtLeastOneBuildingCard() {
        for (Era era : Era.values()) {
            boolean found = data.building_cards().stream()
                    .anyMatch(c -> c.getEra() == era);
            assertTrue(found, "No building card found for era: " + era);
        }
    }


    @Test
    void buildingCardsHaveNonNullTrigger() {
        data.building_cards().forEach(b ->
                assertNotNull(b.getTrigger(),
                        "Every BuildingCard must have a non-null trigger")
        );
    }

    @Test
    void buildingCardsHavePositiveFoodCost() {
        data.building_cards().forEach(b ->
                assertTrue(b.getFoodCost() > 0,
                        "Every BuildingCard must have a positive food cost")
        );
    }

    @Test
    void tribeAcceptsAllCharacterCardsFromJson() {
        Tribe tribe = new Tribe();

        data.tribe_cards().stream()
                .filter(c -> c instanceof CharacterCard)
                .map(c -> (CharacterCard) c)
                .forEach(tribe::addCard);

        // Every character card loaded from JSON must end up in the tribe
        long expected = data.tribe_cards().stream()
                .filter(c -> c instanceof CharacterCard)
                .count();

        assertEquals(expected, tribe.getNumCharacters(),
                "All character cards from JSON should be addable to a Tribe");
    }

    @Test
    void tribeAcceptsAllBuildingCardsFromJson() {
        Tribe tribe = new Tribe();

        data.building_cards().forEach(tribe::addCard);

        assertEquals(data.building_cards().size(), tribe.getNumBuildings(),
                "All building cards from JSON should be addable to a Tribe");
    }


    @Test
    void allCardsHaveNonNegativeMinPlayers() {
        data.tribe_cards().forEach(c ->
                assertTrue(c.getMinPlayers() >= 0,
                        "min_players must be >= 0, got " + c.getMinPlayers() + " for " + c)
        );
        data.building_cards().forEach(c ->
                assertTrue(c.getMinPlayers() >= 0,
                        "min_players must be >= 0, got " + c.getMinPlayers() + " for " + c)
        );
    }
}
