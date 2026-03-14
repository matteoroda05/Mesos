package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class MarketRow {
    private final List<TribeCard> tribe_market;
    private final List<BuildingCard> building_market;

    public MarketRow(boolean is_top_row, TribeDeck tribe_deck, BuildingDeck building_deck) {
        this.tribe_market = is_top_row ? tribe_deck.drawTopRow() : tribe_deck.drawInitialBottomRow();
        this.building_market = is_top_row ? building_deck.getBuildingCards(Era.FIRST) : new ArrayList<>();
    }

    public List<TribeCard> getTribeCards() {
        return tribe_market; //se usassimo una collection sarebbe read-only
    }

    public List<BuildingCard> getBuildingCards() {
        return building_market;
    }

    public List<Card> getCards() {
        List<Card> result = new ArrayList<>(tribe_market.size() + building_market.size());
        result.addAll(tribe_market);
        result.addAll(building_market);
        return result;
    }

    public List<Card> getTakeableCards(int playerFood, int buildersDiscount) {
        if (playerFood < 0) {
            throw new IllegalArgumentException("playerFood cannot be negative");
        }
        if (buildersDiscount < 0) {
            throw new IllegalArgumentException("buildersDiscount cannot be negative");
        }

        List<Card> takeable = new ArrayList<>();

        for (TribeCard card : tribe_market) {
            if (!(card instanceof EventCard)) {
                takeable.add(card);
            }
        }

        for (BuildingCard card : building_market) {
            int effectiveCost = Math.max(0, card.getFoodCost() - buildersDiscount);
            if (effectiveCost <= playerFood) {
                takeable.add(card);
            }
        }

        return takeable;
    }

    public Card takeCard(int index, boolean isBuilding) {
        if (isBuilding) {
            checkIndex(index, building_market.size(), "building_market");
            return building_market.remove(index);
        } else {
            checkIndex(index, tribe_market.size(), "tribe_market");
            return tribe_market.remove(index);
        }
    }

    public void placeCard(Card c) {
        switch (c) {
            case null -> throw new IllegalArgumentException("card cannot be null");
            case BuildingCard buildingCard -> building_market.add(buildingCard);
            case TribeCard tribeCard -> tribe_market.add(tribeCard);
            default -> throw new IllegalArgumentException("Unsupported card type: " + c.getClass().getSimpleName());
        }

    }


    public List<TribeCard> removeTribeCards() {
        List<TribeCard> removed = new ArrayList<>(tribe_market);
        tribe_market.clear();
        return removed;
    }


    public List<BuildingCard> removeBuildingCards() {
        List<BuildingCard> removed = new ArrayList<>(building_market);
        building_market.clear();
        return removed;
    }

    public boolean isEmpty() {
        return tribe_market.isEmpty() && building_market.isEmpty();
    }

    private static void checkIndex(int index, int size, String listName) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for " + listName + " with size " + size
            );
        }
    }


}
