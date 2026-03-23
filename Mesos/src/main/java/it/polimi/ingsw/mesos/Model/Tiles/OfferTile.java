package it.polimi.ingsw.mesos.Model.Tiles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;

public class OfferTile {

    private final char id;
    private final int pick_top_row;
    private final int pick_bottom_row;
    private Color occupant;
    private final int food_gain;
    private final int min_num_players;

    @JsonCreator
    public OfferTile(
            @JsonProperty("offer_tile_id") char offer_tile_id,
            @JsonProperty("pick_top_row") int pick_top_row,
            @JsonProperty("pick_bottom_row") int pick_bottom_row,
            @JsonProperty("food_gain") int food_gain,
            @JsonProperty("min_num_players") int min_num_players
    ) {
        if (offer_tile_id < Globals.FIRST_OFFER || offer_tile_id > Globals.LAST_OFFER) {
            throw new IllegalArgumentException(
                "offerTileId must be between '" + Globals.FIRST_OFFER + "' and '" + Globals.LAST_OFFER + "'");
        }
        if (pick_top_row < 0 || pick_top_row > 2) {
            throw new IllegalArgumentException("pickTopRow must be between 0 and 2");
        }
        if (pick_bottom_row < 0 || pick_bottom_row > 2) {
            throw new IllegalArgumentException("pickBottomRow must be between 0 and 2");
        }
        if (food_gain < 0 || food_gain > 3) {
            throw new IllegalArgumentException("foodGain cannot be negative or greater than 3");
        }
        if (min_num_players < 2 || min_num_players > 5) {
            throw new IllegalArgumentException("minNumPlayers must be between 2 and 5");
        }

        this.id = offer_tile_id;
        this.pick_top_row = pick_top_row;
        this.pick_bottom_row = pick_bottom_row;
        this.occupant = null; // sempre runtime
        this.food_gain = food_gain;
        this.min_num_players = min_num_players;
    }

    public char getId() {
        return id;
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

    public Color clear() {
        Color output = occupant;
        occupant = null;
        return output;
    }
}