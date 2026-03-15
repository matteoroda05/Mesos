package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;

public class GameTable {
    private final OfferTrack offer_track;
    private final TurnOrderTile turn_order_tile;
    private final MarketRow top_row;
    private final MarketRow bottom_row;
    private final BuildingDeck building_deck;
    private final TribeDeck tribe_deck;
 
    public GameTable(int num_players) {
        this.offer_track = new OfferTrack(num_players);
        this.turn_order_tile = new TurnOrderTile(num_players);
        this.building_deck = new BuildingDeck(num_players);
        this.tribe_deck = new TribeDeck(num_players);
        this.top_row = new MarketRow(true, tribe_deck, building_deck);
        this.bottom_row = new MarketRow(false, tribe_deck, building_deck);
    }

    public OfferTrack getOfferTrack() {
        return offer_track;
    }

    public TurnOrderTile getTurnOrderTile() {
        return turn_order_tile;
    }

    public MarketRow getTopRow() {
        return top_row;
    }

    public MarketRow getBottomRow() {
        return bottom_row;
    }

    public void placeTotemOnOfferTrack(Player player, int offer_tile_id) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        OfferTile offer_tile = offer_track.getTile(offer_tile_id);

        if (offer_tile == null) {
            throw new IllegalArgumentException("Offer tile with id " + offer_tile_id + " does not exist");
        }

        if (!offer_tile.isAvailable()) {
            throw new IllegalStateException("Offer tile with id " + offer_tile_id + " is not available");
        }

        offer_tile.occupy(player.getColor());
    }

    private void moveTopToBottomTribeCards() {
        
    }

    private void restoreTopRow(TribeDeck tribe_deck, int players_count) {
        if (tribe_deck == null) {
            throw new IllegalArgumentException("tribeDeck cannot be null");
        }

        List<TribeCard> drawn_cards = tribe_deck.drawTopRow();

        for (int i = 0; i < drawn_cards.size(); i++) {
            top_row.placeCard(drawn_cards.get(i));
        }
    }

    public void beginNewEra(BuildingDeck new_era_building_deck) {
        if (new_era_building_deck == null) {
            throw new IllegalArgumentException("newEraBuildingDeck cannot be null");
        }

        bottom_row.removeBuildingCards();

        List<BuildingCard> buildings_from_top = top_row.removeBuildingCards();
        for (int i = 0; i < buildings_from_top.size(); i++) {
            bottom_row.placeCard(buildings_from_top.get(i));
        }

        List<BuildingCard> new_buildings = new_era_building_deck.drawBuildingCards();
        for (int i = 0; i < new_buildings.size(); i++) {
            top_row.placeCard(new_buildings.get(i));
        }
    }

    public List<EventCard> visibleEvents(boolean include_top_row) {
        List<EventCard> result = new ArrayList<>();

        for (Card card : bottom_row.getCards()) {
            if (card.isEvent()) {
                result.add((EventCard) card);
            }
        }

        if (include_top_row) {
            for (Card card : top_row.getCards()) {
                if (card.isEvent()) {
                    result.add((EventCard) card);
                }
            }
        }

        return result;
    }

    // Returns wether a new era has begun
    // public boolean newRound() {
    //     bottom_row.removeTribeCards();
    //     List<TribeCard> top_row = top_row


    // }
}