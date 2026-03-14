package it.polimi.ingsw.mesos.Model;

public class GameTable {
    private OfferTrack offer_track;
    private TurnOrderTile turn_order_tile;
    private MarketRow top_row;
    private MarketRow bottom_row;

    public GameTable(int num_players, TribeDeck tribe_deck, BuildingDeck building_deck) {
        this.offer_track = new OfferTrack(num_players);
        this.turn_order_tile = new TurnOrderTile(num_players);
        this.top_row = new MarketRow(true, tribe_deck, building_deck );
        this.bottom_row = new MarketRow(false, tribe_deck, building_deck);
    }

    // TODO: getters AND methods
}
