package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class MarketRow {
    private List<TribeCard> tribe_market;
    private List<BuildingCard> building_market;

    public MarketRow(boolean is_top_row, TribeDeck tribe_deck, BuildingDeck building_deck) {
        this.tribe_market = is_top_row ? tribe_deck.drawTopRow() : tribe_deck.drawInitialBottomRow();
        this.building_market = is_top_row ? building_deck.getBuildingCards(Era.FIRST) : new ArrayList<>();
    }

    // TODO: getters AND methods
}
