package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndRound;
import it.polimi.ingsw.mesos.Model.Enums.Era;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingDeckTest {
    private BuildingEndRound first_era_card_one;
    private BuildingEndRound first_era_card_two;
    private BuildingEndRound second_era_card;
    private BuildingEndRound third_era_card;

    @BeforeEach
    void setUp() {
        first_era_card_one = new BuildingEndRound(Era.FIRST, 0, 1, 1);
        first_era_card_two = new BuildingEndRound(Era.FIRST, 0, 2, 2);
        second_era_card = new BuildingEndRound(Era.SECOND, 0, 3, 3);
        third_era_card = new BuildingEndRound(Era.THIRD, 0, 4, 4);
    }

    @Test
    void constructorThrowsWhenBuildingCardsIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new BuildingDeck(null));
    }

    @Test
    void constructorThrowsWhenBuildingCardsContainsNull() {
        List<BuildingCard> cards = new ArrayList<>();
        cards.add(first_era_card_one);
        cards.add(null);

        assertThrows(IllegalArgumentException.class, () -> new BuildingDeck(cards));
    }

    @Test
    void isEmptyReturnsTrueWhenDeckStartsEmpty() {
        BuildingDeck building_deck = new BuildingDeck(List.of());

        assertTrue(building_deck.isEmpty());
    }

    @Test
    void isEmptyReturnsFalseWhenDeckHasCards() {
        BuildingDeck building_deck = new BuildingDeck(List.of(first_era_card_one));

        assertFalse(building_deck.isEmpty());
    }

    @Test
    void drawBuildingCardsReturnsAllCardsOfEarliestAvailableEraInInputOrder() {
        BuildingDeck building_deck = new BuildingDeck(List.of(
                second_era_card,
                first_era_card_one,
                third_era_card,
                first_era_card_two));

        assertIterableEquals(List.of(first_era_card_one, first_era_card_two), building_deck.drawBuildingCards());
        assertFalse(building_deck.isEmpty());
        assertIterableEquals(List.of(second_era_card), building_deck.drawBuildingCards());
        assertFalse(building_deck.isEmpty());
        assertIterableEquals(List.of(third_era_card), building_deck.drawBuildingCards());
        assertTrue(building_deck.isEmpty());
    }

    @Test
    void drawBuildingCardsSkipsMissingEarlierEras() {
        BuildingDeck building_deck = new BuildingDeck(List.of(second_era_card, third_era_card));

        assertIterableEquals(List.of(second_era_card), building_deck.drawBuildingCards());
        assertIterableEquals(List.of(third_era_card), building_deck.drawBuildingCards());
        assertTrue(building_deck.isEmpty());
    }

    @Test
    void drawBuildingCardsThrowsWhenDeckIsEmpty() {
        BuildingDeck building_deck = new BuildingDeck(List.of());

        assertThrows(IllegalAccessError.class, building_deck::drawBuildingCards);
    }
}
