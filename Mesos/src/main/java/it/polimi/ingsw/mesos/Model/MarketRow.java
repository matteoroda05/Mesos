package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Cards.*;

public class MarketRow {
    private final List<TribeCard> tribe_market;
    private final List<BuildingCard> building_market;

    /**
     * Creates and initially fills a market row.
     * Top rows draw tribe cards and buildings, while bottom rows draw only the
     * initial non-event tribe cards.
     *
     * @param is_top_row true for the top market row, false for the bottom row
     * @param tribe_deck deck used to draw tribe cards
     * @param building_deck deck used to draw buildings for the top row
     * @throws IllegalArgumentException if the tribe deck is null, or if a top
     *                                  row is created without a building deck
     */
    public MarketRow(boolean is_top_row, TribeDeck tribe_deck, BuildingDeck building_deck) {
        if (tribe_deck == null) {
            throw new IllegalArgumentException("tribe_deck cannot be null");
        }
        if (is_top_row && building_deck == null) {
            throw new IllegalArgumentException("building_deck cannot be null for top row");
        }
        
        this.tribe_market = is_top_row ? tribe_deck.drawTopRow() : tribe_deck.drawInitialBottomRow();
        this.building_market = is_top_row ? building_deck.drawBuildingCards() : new ArrayList<>();
    }

    /**
     * Returns the tribe-card list backing this row.
     *
     * @return mutable tribe market list
     */
    public List<TribeCard> getTribeCards() {
        return tribe_market; //se usassimo una collection sarebbe read-only
    }

    /**
     * Returns the building-card list backing this row.
     *
     * @return mutable building market list
     */
    public List<BuildingCard> getBuildingCards() {
        return building_market;
    }

    /**
     * Returns a combined snapshot of tribe and building cards currently present
     * in this market row.
     *
     * @return a new list containing all cards in row order by card group
     */
    public List<Card> getCards() {
        List<Card> result = new ArrayList<>(tribe_market.size() + building_market.size());
        result.addAll(tribe_market);
        result.addAll(building_market);
        return result;
    }

    /**
     * Computes which cards the player can take from this row. Non-event tribe
     * cards are always takeable, while buildings are takeable only when the
     * player's food covers their discounted cost.
     *
     * @param player_food the amount of food available to the player
     * @param builders_discount the total discount applied to building costs
     * @return a list of cards that can currently be taken
     * @throws IllegalArgumentException if food or discount is negative
     */
    public List<Card> getTakeableCards(int player_food, int builders_discount) {
        if (player_food < 0) {
            throw new IllegalArgumentException("player_food cannot be negative");
        }
        if (builders_discount < 0) {
            throw new IllegalArgumentException("builders_discount cannot be negative");
        }

        List<Card> takeable = new ArrayList<>();

        for (TribeCard card : tribe_market) {
            if (!(card.isEvent())) {
                takeable.add(card);
            }
        }

        for (BuildingCard card : building_market) {
            int effectiveCost = Math.max(0, card.getFoodCost() - builders_discount);
            if (effectiveCost <= player_food) {
                takeable.add(card);
            }
        }

        return takeable;
    }

    /**
     * Removes and returns a building card at the requested position.
     *
     * @param index position in the building market
     * @return the removed building card
     * @throws IndexOutOfBoundsException if the index is outside the building
     *                                   market
     */
    public BuildingCard takeBuildingCard(int index) {
        checkIndex(index, building_market.size(), "building_market");
        return building_market.remove(index);
    }

    /**
     * Removes and returns a character card from the tribe market.
     *
     * @param index position in the tribe market
     * @return the removed character card
     * @throws IndexOutOfBoundsException if the index is outside the tribe
     *                                   market
     * @throws IllegalArgumentException if the card at the index is an event
     */
    public CharacterCard takeCharacterCard(int index) {
        checkIndex(index, tribe_market.size(), "tribe_market");
        if (tribe_market.get(index).isEvent()) throw new IllegalArgumentException("Cannot take event card");
        // DOWN CASTING
        return (CharacterCard) tribe_market.remove(index);
    }

    /**
     * Returns the printed food cost of a building in this row.
     *
     * @param index position in the building market
     * @return building food cost before discounts
     * @throws IndexOutOfBoundsException if the index is outside the building
     *                                   market
     */
    public int getBuildingCost(int index){
        checkIndex(index, building_market.size(), "building_market");
        return building_market.get(index).getFoodCost();
    }

    /**
     * Adds a card to the tribe market.
     *
     * @param tc card to place in the row
     * @throws IllegalArgumentException if the card is null
     */
    public void placeCard(TribeCard tc){
        if (tc == null){
            throw new IllegalArgumentException("tribe card cannot be null");
        }
        tribe_market.add(tc);
    }

    /**
     * Adds a card to the building market.
     *
     * @param bc card to place in the row
     * @throws IllegalArgumentException if the card is null
     */
    public void placeCard(BuildingCard bc){
        if (bc == null){
            throw new IllegalArgumentException("tribe card cannot be null");
        }
        building_market.add(bc);
    }

    /**
     * Removes every tribe card from the row and returns the removed cards.
     *
     * @return the tribe cards that were present before clearing, or an empty
     *         list if none were present
     */
    public List<TribeCard> removeTribeCards() {
        List<TribeCard> removed = new ArrayList<>(tribe_market);
        tribe_market.clear();
        return removed;
    }

    /**
     * Removes every building card from the row and returns the removed cards.
     *
     * @return the building cards that were present before clearing, or an
     *         empty list if none were present
     */
    public List<BuildingCard> removeBuildingCards() {
        List<BuildingCard> removed = new ArrayList<>(building_market);
        building_market.clear();
        return removed;
    }

    /**
     * Checks whether the row contains no tribe cards and no building cards.
     *
     * @return true when both markets are empty
     */
    public boolean isEmpty() {
        return tribe_market.isEmpty() && building_market.isEmpty();
    }

    /**
     * Validates that an index can be used against a list of the given size.
     *
     * @param index candidate index
     * @param size list size
     * @param listName name used in the exception message
     * @throws IndexOutOfBoundsException if the index is negative or not smaller
     *                                   than the list size
     */
    private static void checkIndex(int index, int size, String listName) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for " + listName + " with size " + size
            );
        }
    }

}
