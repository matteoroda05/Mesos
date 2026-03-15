package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;

public class OfferTile {

    private final char id;
    private final int pick_top_row;
    private final int pick_bottom_row;
    private Color occupant;
    private final int food_gain;
    private final int min_num_players;

    public OfferTile(char offer_tile_id, int pick_top_row, int pick_bottom_row, int food_gain, int min_num_players) {
        if (offer_tile_id < Globals.FIRST_OFFER || offer_tile_id > Globals.LAST_OFFER) {
            throw new IllegalArgumentException(
                "offerTileId must be between '" + Globals.FIRST_OFFER + "' and '" + Globals.LAST_OFFER);
        }
        if (pick_top_row < 0) {
            throw new IllegalArgumentException("pickTopRow cannot be negative");
        }
        if (pick_bottom_row < 0) {
            throw new IllegalArgumentException("pickBottomRow cannot be negative");
        }
        if (food_gain < 0) {
            throw new IllegalArgumentException("foodGain cannot be negative");
        }
        if (min_num_players < 0) {
            throw new IllegalArgumentException("minNumPlayers must be positive");
        }

        this.id = offer_tile_id;
        this.pick_top_row = pick_top_row;
        this.pick_bottom_row = pick_bottom_row;
        this.occupant = null;
        this.food_gain = food_gain;
        this.min_num_players = min_num_players;
    }

    public int getTopPicks() {
        return pick_top_row;
    }

    public int getBottomPicks() {
        return pick_bottom_row;
    }

    public int getFoodGain() {
        return food_gain;
    }

    public char getOfferTileId() {
        return id;
    }

    public Color getOccupant() {
        return occupant;
    }

    public int getMinNumPlayers() {
        return min_num_players;
    }

    public boolean isAvailable() {
        return occupant == null;
    }

    public void occupy(Color player_color) {
        if (player_color == null) {
            throw new IllegalArgumentException("Player Color cannot be null");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("Offer tile is already occupied");
        }

        this.occupant = player_color;
    }

    public void clear() {
        occupant = null;
    }

}
