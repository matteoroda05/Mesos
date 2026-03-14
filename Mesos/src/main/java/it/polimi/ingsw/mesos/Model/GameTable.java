package it.polimi.ingsw.mesos.Model;

public class GameTable {
    private OfferTrack offer_track;
    private TurnOrderTile turn_order_tile;
    private MarketRow top_row;
    private MarketRow bottom_row;

    public GameTable(int num_players) {
        this.offer_track = new OfferTrack(num_players);
        this.turn_order_tile = new TurnOrderTile(num_players);
        this.top_row = new MarketRow(true);
        this.bottom_row = new MarketRow(false);
    }

    // TODO: getters AND methods
}
