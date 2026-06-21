package it.polimi.ingsw.mesos.model.tiles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.polimi.ingsw.mesos.common.Globals;
import it.polimi.ingsw.mesos.model.enums.Color;

/**
 * Models an offer-track space with its card-pick allowances, food reward, and
 * optional occupying player totem.
 */
public class OfferTile {

    private final char id;
    private final int pick_top_row;
    private final int pick_bottom_row;
    private Color occupant;
    private final int food_gain;
    private final int min_num_players;

    /**
     * Creates an offer tile from JSON data and leaves it unoccupied.
     *
     * @param offer_tile_id tile identifier within the configured id range
     * @param pick_top_row number of cards selectable from the top row
     * @param pick_bottom_row number of cards selectable from the bottom row
     * @param food_gain food granted by the tile
     * @param min_num_players minimum player count required for this tile
     * @throws IllegalArgumentException if any numeric value is outside its
     *                                  allowed range
     */
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
        if (min_num_players < Globals.MIN_NUM_PLAYERS || min_num_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException("minNumPlayers must be between 2 and 5");
        }

        this.id = offer_tile_id;
        this.pick_top_row = pick_top_row;
        this.pick_bottom_row = pick_bottom_row;
        this.occupant = null; // sempre runtime
        this.food_gain = food_gain;
        this.min_num_players = min_num_players;
    }

    /**
     * Returns how many cards this tile allows taking from the top row.
     *
     * @return top-row picks
     */
    public int getTopPicks() {
        return pick_top_row;
    }

    /**
     * Returns how many cards this tile allows taking from the bottom row.
     *
     * @return bottom-row picks
     */
    public int getBottomPicks() {
        return pick_bottom_row;
    }

    /**
     * Returns the food gained from choosing this tile.
     *
     * @return food gain
     */
    public int getFoodGain() {
        return food_gain;
    }

    /**
     * Returns this tile's identifier.
     *
     * @return offer tile id
     */
    public char getOfferTileId() {
        return id;
    }

    /**
     * Returns the occupying player color, if any.
     *
     * @return occupant color, or null when the tile is free
     */
    public Color getOccupant() {
        return occupant;
    }

    /**
     * Returns the minimum player count required for this tile to be used.
     *
     * @return minimum number of players
     */
    public int getMinNumPlayers() {
        return min_num_players;
    }

    /**
     * Checks whether this tile has no occupying totem.
     *
     * @return true when no player occupies the tile
     */
    public boolean isAvailable() {
        return occupant == null;
    }

    /**
     * Marks this offer tile as occupied by a player's totem.
     *
     * @param player_color color of the occupying player
     * @throws IllegalArgumentException if the player color is null
     * @throws IllegalStateException if the tile is already occupied
     */
    public void occupy(Color player_color) {
        if (player_color == null) {
            throw new IllegalArgumentException("Player Color cannot be null");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("Offer tile is already occupied");
        }
        this.occupant = player_color;
    }

    /**
     * Removes the occupying totem and returns the player color that occupied
     * the tile.
     *
     * @return previous occupant, or null if the tile was already empty
     */
    public Color clear() {
        Color output = occupant;
        occupant = null;
        return output;
    }
}
