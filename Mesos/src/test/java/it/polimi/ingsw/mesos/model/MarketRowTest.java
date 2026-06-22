package it.polimi.ingsw.mesos.model;

import it.polimi.ingsw.mesos.model.cards.BuildingCard;
import it.polimi.ingsw.mesos.model.cards.Card;
import it.polimi.ingsw.mesos.model.cards.TribeCard;
import it.polimi.ingsw.mesos.model.cards.buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.model.cards.characters.BuilderCard;
import it.polimi.ingsw.mesos.model.cards.characters.HunterCard;
import it.polimi.ingsw.mesos.model.cards.events.HuntEvent;
import it.polimi.ingsw.mesos.model.enums.Era;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketRowTest {

    private HunterCard tribe_card_one;
    private HunterCard tribe_card_two;
    private BuilderCard tribe_card_three;
    private HunterCard tribe_card_four;
    private HunterCard tribe_card_five;
    private BuilderCard tribe_card_six;

    private HuntEvent event_card_one;
    private HuntEvent event_card_two;

    private BuildingAfterEveryone building_card_low_cost;
    private BuildingAfterEveryone building_card_medium_cost;
    private BuildingAfterEveryone building_card_high_cost;

    @BeforeEach
    void setUp() {
        tribe_card_one = new HunterCard(Era.FIRST, 2, true);
        tribe_card_two = new HunterCard(Era.FIRST, 2, false);
        tribe_card_three = new BuilderCard(Era.FIRST, 2, 2, 1);
        tribe_card_four = new HunterCard(Era.SECOND, 2, true);
        tribe_card_five = new HunterCard(Era.SECOND, 2, false);
        tribe_card_six = new BuilderCard(Era.THIRD, 2, 3, 2);

        event_card_one = new HuntEvent(Era.FIRST, 1, 1);
        event_card_two = new HuntEvent(Era.SECOND, 2, 1);

        building_card_low_cost = new BuildingAfterEveryone(Era.FIRST, 0, 1, 1);
        building_card_medium_cost = new BuildingAfterEveryone(Era.FIRST, 0, 3, 2);
        building_card_high_cost = new BuildingAfterEveryone(Era.SECOND, 0, 7, 3);
    }

    @Test
    void constructorThrowsWhenTribeDeckIsNullForTopRow() {
        BuildingDeck building_deck = createBuildingDeck(List.of(building_card_low_cost), List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> new MarketRow(true, null, building_deck));
    }

    @Test
    void constructorThrowsWhenTribeDeckIsNullForBottomRow() {
        assertThrows(IllegalArgumentException.class, () -> new MarketRow(false, null, null));
    }

    @Test
    void constructorThrowsWhenTopRowHasNullBuildingDeck() {
        TribeDeck tribe_deck = createTribeDeckForTopRow();

        assertThrows(IllegalArgumentException.class, () -> new MarketRow(true, tribe_deck, null));
    }

    @Test
    void constructorInitializesTopRowWithTopTribesAndBuildings() {
        TribeDeck tribe_deck = createTribeDeckForTopRow();
        BuildingDeck building_deck = createBuildingDeck(
                List.of(building_card_low_cost, building_card_medium_cost),
                List.of(building_card_high_cost),
                List.of());

        MarketRow market_row = new MarketRow(true, tribe_deck, building_deck);

        assertIterableEquals(
                List.of(tribe_card_six, tribe_card_five, tribe_card_four, tribe_card_three, tribe_card_two,
                        tribe_card_one),
                market_row.getTribeCards());
        assertIterableEquals(List.of(building_card_low_cost, building_card_medium_cost), market_row.getBuildingCards());
    }

    @Test
    void constructorInitializesInitialBottomRowWithOnlyTribeCards() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();

        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        assertIterableEquals(List.of(tribe_card_four, tribe_card_three, tribe_card_two), market_row.getTribeCards());
        assertTrue(market_row.getBuildingCards().isEmpty());
    }

    @Test
    void bottomRowCanContainBuildingsInLaterEras() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        market_row.placeCard(building_card_high_cost);

        assertIterableEquals(List.of(building_card_high_cost), market_row.getBuildingCards());
        assertIterableEquals(
                List.of(tribe_card_four, tribe_card_three, tribe_card_two, building_card_high_cost),
                market_row.getCards());
    }

    @Test
    void getCardsReturnsTribesThenBuildings() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_low_cost);
        market_row.placeCard(building_card_medium_cost);

        List<Card> all_cards = market_row.getCards();

        assertIterableEquals(
                List.of(tribe_card_four, tribe_card_three, tribe_card_two, building_card_low_cost,
                        building_card_medium_cost),
                all_cards);
    }

    @Test
    void getTakeableCardsThrowsWhenPlayerFoodIsNegative() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        assertThrows(IllegalArgumentException.class, () -> market_row.getTakeableCards(-1, 0));
    }

    @Test
    void getTakeableCardsThrowsWhenBuildersDiscountIsNegative() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        assertThrows(IllegalArgumentException.class, () -> market_row.getTakeableCards(0, -1));
    }

    @Test
    void getTakeableCardsIncludesAllNonEventTribesAndExcludesEvents() {
        TribeDeck tribe_deck = new TribeDeck(2,
                List.of(tribe_card_one, event_card_one, tribe_card_two, event_card_two));
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        List<Card> takeable_cards = market_row.getTakeableCards(0, 0);

        assertIterableEquals(List.of(tribe_card_two, tribe_card_one), takeable_cards);
    }

    @Test
    void getTakeableCardsExcludesEventCardsWhenEventsAreActuallyInMarketRow() {
        TribeDeck tribe_deck = new TribeDeck(2, List.of(tribe_card_one, tribe_card_two, tribe_card_three));
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(event_card_one);

        List<Card> takeable_cards = market_row.getTakeableCards(0, 0);

        assertFalse(takeable_cards.contains(event_card_one));
        assertIterableEquals(List.of(tribe_card_three, tribe_card_two, tribe_card_one), takeable_cards);
    }

    @Test
    void getTakeableCardsIncludesOnlyAffordableBuildingsAfterDiscount() {
        TribeDeck tribe_deck = new TribeDeck(2, List.of(tribe_card_one, tribe_card_two, tribe_card_three));
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_low_cost);
        market_row.placeCard(building_card_medium_cost);
        market_row.placeCard(building_card_high_cost);

        List<Card> takeable_cards = market_row.getTakeableCards(2, 1);

        assertIterableEquals(
                List.of(tribe_card_three, tribe_card_two, tribe_card_one, building_card_low_cost,
                        building_card_medium_cost),
                takeable_cards);
    }

    @Test
    void getTakeableCardsAppliesZeroFloorToEffectiveCost() {
        TribeDeck tribe_deck = new TribeDeck(2, List.of(tribe_card_one, tribe_card_two, tribe_card_three));
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_low_cost);

        List<Card> takeable_cards = market_row.getTakeableCards(0, 5);

        assertTrue(takeable_cards.contains(building_card_low_cost));
    }

    @Test
    void typedTakeHelpersRemoveSelectedCardsAndRejectInvalidChoices() {
        TribeDeck tribe_deck = new TribeDeck(2, List.of(tribe_card_one, tribe_card_two, tribe_card_three));
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_low_cost);
        market_row.placeCard(building_card_medium_cost);

        assertSame(tribe_card_two, market_row.takeCharacterCard(1));
        assertIterableEquals(List.of(tribe_card_three, tribe_card_one), market_row.getTribeCards());
        assertSame(building_card_low_cost, market_row.takeBuildingCard(0));
        assertIterableEquals(List.of(building_card_medium_cost), market_row.getBuildingCards());

        assertThrows(IndexOutOfBoundsException.class, () -> market_row.takeCharacterCard(-1));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> market_row.takeCharacterCard(market_row.getTribeCards().size()));
        assertThrows(IndexOutOfBoundsException.class, () -> market_row.takeBuildingCard(-1));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> market_row.takeBuildingCard(market_row.getBuildingCards().size()));

        market_row.placeCard(event_card_one);
        assertThrows(
                IllegalArgumentException.class,
                () -> market_row.takeCharacterCard(market_row.getTribeCards().size() - 1));
    }

    @Test
    void getBuildingCostReturnsPrintedCostAndValidatesIndex() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_medium_cost);

        assertEquals(3, market_row.getBuildingCost(0));
        assertThrows(IndexOutOfBoundsException.class, () -> market_row.getBuildingCost(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> market_row.getBuildingCost(1));
    }

    @Test
    void placeCardPutsCardsInTheCorrectComponentList() {
        TribeDeck tribe_deck = new TribeDeck(2, List.of(tribe_card_one, tribe_card_two, tribe_card_three));
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        BuilderCard extra_tribe_card = new BuilderCard(Era.SECOND, 2, 1, 1);
        BuildingAfterEveryone extra_building_card = new BuildingAfterEveryone(Era.SECOND, 0, 2, 1);

        market_row.placeCard(extra_tribe_card);
        market_row.placeCard(extra_building_card);

        assertEquals(
                extra_tribe_card,
                market_row.getTribeCards().get(market_row.getTribeCards().size() - 1));
        assertEquals(
                extra_building_card,
                market_row.getBuildingCards().get(market_row.getBuildingCards().size() - 1));
    }

    @Test
    void placeCardThrowsWhenCardIsNull() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        assertThrows(IllegalArgumentException.class, () -> market_row.placeCard((TribeCard) null));
        assertThrows(IllegalArgumentException.class, () -> market_row.placeCard((BuildingCard) null));
    }

    @Test
    void removeTribeCardsReturnsRemovedCardsAndClearsOnlyTribeComponent() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_low_cost);

        List<TribeCard> removed_tribe_cards = market_row.removeTribeCards();

        assertIterableEquals(List.of(tribe_card_four, tribe_card_three, tribe_card_two), removed_tribe_cards);
        assertTrue(market_row.getTribeCards().isEmpty());
        assertIterableEquals(List.of(building_card_low_cost), market_row.getBuildingCards());
    }

    @Test
    void removeBuildingCardsReturnsRemovedCardsAndClearsOnlyBuildingComponent() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_low_cost);
        market_row.placeCard(building_card_medium_cost);

        List<BuildingCard> removed_building_cards = market_row.removeBuildingCards();

        assertIterableEquals(List.of(building_card_low_cost, building_card_medium_cost), removed_building_cards);
        assertTrue(market_row.getBuildingCards().isEmpty());
        assertFalse(market_row.getTribeCards().isEmpty());
    }

    @Test
    void removeTribeCardsOnEmptyComponentReturnsEmptyList() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.removeTribeCards();

        List<TribeCard> removed_tribe_cards = market_row.removeTribeCards();

        assertTrue(removed_tribe_cards.isEmpty());
        assertTrue(market_row.getTribeCards().isEmpty());
    }

    @Test
    void removeBuildingCardsOnEmptyComponentReturnsEmptyList() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        List<BuildingCard> removed_building_cards = market_row.removeBuildingCards();

        assertTrue(removed_building_cards.isEmpty());
        assertTrue(market_row.getBuildingCards().isEmpty());
    }

    @Test
    void isEmptyReturnsTrueOnlyWhenBothComponentsAreEmpty() {
        TribeDeck tribe_deck = createTribeDeckForInitialBottomRow();
        MarketRow market_row = new MarketRow(false, tribe_deck, null);

        assertFalse(market_row.isEmpty());

        market_row.removeTribeCards();
        assertTrue(market_row.isEmpty());

        market_row.placeCard(building_card_low_cost);
        assertFalse(market_row.isEmpty());
    }

    private TribeDeck createTribeDeckForTopRow() {
        return new TribeDeck(
                2,
                List.of(
                        tribe_card_one,
                        tribe_card_two,
                        tribe_card_three,
                        tribe_card_four,
                        tribe_card_five,
                        tribe_card_six));
    }

    private TribeDeck createTribeDeckForInitialBottomRow() {
        return new TribeDeck(
                2,
                List.of(
                        tribe_card_one,
                        event_card_one,
                        tribe_card_two,
                        tribe_card_three,
                        event_card_two,
                        tribe_card_four));
    }

    private BuildingDeck createBuildingDeck(
            List<BuildingCard> first_era_cards,
            List<BuildingCard> second_era_cards,
            List<BuildingCard> third_era_cards) {

        List<BuildingCard> all_cards = new ArrayList<>();
        all_cards.addAll(first_era_cards);
        all_cards.addAll(second_era_cards);
        all_cards.addAll(third_era_cards);

        return new BuildingDeck(all_cards);
    }

    @Test
    void getTakeableCardsCanReturnBothTribesAndBuildingsTogether() {
        TribeDeck tribe_deck = new TribeDeck(2, List.of(tribe_card_one, event_card_one, tribe_card_two));
        MarketRow market_row = new MarketRow(false, tribe_deck, null);
        market_row.placeCard(building_card_low_cost);
        market_row.placeCard(building_card_high_cost);

        List<Card> takeable_cards = market_row.getTakeableCards(1, 0);

        assertIterableEquals(
                List.of(tribe_card_two, tribe_card_one, building_card_low_cost),
                takeable_cards);
    }
}
