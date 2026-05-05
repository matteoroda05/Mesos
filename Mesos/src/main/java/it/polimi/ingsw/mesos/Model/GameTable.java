package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

public class GameTable {
    private final OfferTrack offer_track;
    private final TurnOrderTile turn_order_tile;
    private final MarketRow top_row;
    private final MarketRow bottom_row;
    private final BuildingDeck building_deck;
    private final TribeDeck tribe_deck;

    public GameTable(OfferTrack offer_track,
            TurnOrderTile turn_order_tile,
            BuildingDeck building_deck,
            TribeDeck tribe_deck) {

        if (offer_track == null) {
            throw new IllegalArgumentException("offerTrack cannot be null");
        }
        if (turn_order_tile == null) {
            throw new IllegalArgumentException("turnOrderTile cannot be null");
        }
        if (building_deck == null) {
            throw new IllegalArgumentException("buildingDeck cannot be null");
        }
        if (tribe_deck == null) {
            throw new IllegalArgumentException("tribeDeck cannot be null");
        }

        this.offer_track = offer_track;
        this.turn_order_tile = turn_order_tile;
        this.building_deck = building_deck;
        this.tribe_deck = tribe_deck;
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

    /**
     * Places a player's totem on an available offer tile after validating both
     * the player and the requested tile.
     *
     * @param player the player whose totem is being placed
     * @param offer_tile_id index of the offer tile on the offer track
     */
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

    /**
     * Refills the top market row with tribe cards and reports whether any
     * drawn card belongs to a new era.
     *
     * @param curr_era the era of the cards moved down during this refill
     * @return true when at least one newly drawn card belongs to a different
     *         era
     */
    private boolean restoreTopRow(Era curr_era) {
        if (tribe_deck == null) {
            throw new IllegalArgumentException("Tribe deck null at start of new era.");
        }

        List<TribeCard> drawn_cards = tribe_deck.drawTopRow();
        boolean new_era = false;

        for (TribeCard card : drawn_cards) {
            top_row.placeCard(card);
            if (card.getEra() != curr_era) {
                new_era = true;
            }
        }

        return new_era;
    }

    /**
     * Performs the building-card transition that happens when a new era starts:
     * bottom-row buildings are discarded, top-row buildings move down, and new
     * buildings are drawn into the top row.
     */
    public void beginNewEra() {
        bottom_row.removeBuildingCards();

        List<BuildingCard> buildings_from_top = top_row.removeBuildingCards();
        for (BuildingCard card : buildings_from_top) {
            bottom_row.placeCard(card);
        }

        List<BuildingCard> new_buildings = building_deck.drawBuildingCards();
        for (BuildingCard card : new_buildings) {
            top_row.placeCard(card);
        }
    }

    /**
     * Collects all visible event cards from the market rows.
     *
     * @param include_top_row when true, events in the top row are included in
     *                        addition to bottom-row events
     * @return event cards currently visible in the requested rows
     */
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

    /**
     * Advances market rows for a new round by removing old bottom-row tribe
     * cards, moving top-row tribe cards down, and refilling the top row.
     *
     * @return true if refilling the top row revealed cards from a new era
     */
    public boolean newRound() {
        bottom_row.removeTribeCards();
        List<TribeCard> to_move = top_row.removeTribeCards();
        Era curr_era = to_move.getFirst().getEra();

        for (TribeCard card : to_move) {
            bottom_row.placeCard(card);
        }

        return restoreTopRow(curr_era);
    }
}
