package it.polimi.ingsw.mesos.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.common.Globals;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.tiles.OfferTile;

/**
 * Maintains the offer tiles available in a round and their current player
 * occupants.
 */
public class OfferTrack {
    private final List<OfferTile> tiles;

    /**
     * Creates an offer track with the tiles usable for the current player
     * count.
     *
     * @param offer_tiles tiles to place on the track
     * @param num_players number of players in the game
     * @throws IllegalArgumentException if the tile list is null, the player
     *                                  count is invalid, the number of tiles
     *                                  is not {@code num_players + 2}, a tile
     *                                  is null, tile ids are duplicated, or a
     *                                  tile requires more players than the game
     *                                  has
     */
    public OfferTrack(List<OfferTile> offer_tiles, int num_players) {
        if (offer_tiles == null) {
            throw new IllegalArgumentException("Offer track tiles cannot be null");
        }

        if (num_players < Globals.MIN_NUM_PLAYERS || num_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "num_players must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }

        if (offer_tiles.size() != num_players + 2){
            throw new IllegalArgumentException("There must be " + num_players +" + 2 tiles");
        }

        if (offer_tiles.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Offer track contains null tile entries");
        }

        if (offer_tiles.stream().map(OfferTile::getOfferTileId).distinct().count() != offer_tiles.size()) {
            throw new IllegalArgumentException("Offer track contains duplicate tiles");
        }

        if (offer_tiles.stream().anyMatch(tile -> tile.getMinNumPlayers() > num_players)) {
            throw new IllegalArgumentException("Offer track contains tiles that cannot be used with the current number of players");
        }

        this.tiles = offer_tiles;
    }

    /**
     * Returns the offer tiles that can still be occupied this round.
     *
     * @return available offer tiles in track order
     * @throws IllegalStateException if no tile is available, which should not
     *                               happen during a valid offer phase
     */
    public List<OfferTile> getAvailableTiles() {
        List<OfferTile> output = this.tiles.stream()
                .filter(Objects::nonNull)
                .filter(OfferTile::isAvailable)
                .collect(Collectors.toList());

        if (output.isEmpty()) {
            throw new IllegalStateException("No available offer tiles: impossible state");
        }

        return output;
    }

    /**
     * Returns the offer tiles that already contain a player totem.
     *
     * @return occupied offer tiles in track order, or an empty list if none are
     *         occupied
     */
    public List<OfferTile> getOccupiedTiles() {
        return this.tiles.stream()
                .filter(Objects::nonNull)
                .filter(tile -> !tile.isAvailable())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves an offer tile by position and validates that the tile is not null.
     *
     * @param index tile position in the offer track
     * @return the offer tile at the requested index
     * @throws IndexOutOfBoundsException if the index is outside the track
     * @throws IllegalStateException if the requested entry is null
     */
    public OfferTile getTile(int index){
        if (index < 0 || index >= this.tiles.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for offer track with size " + tiles.size());
        }

        OfferTile tile = this.tiles.get(index);
        if (tile == null) {
            throw new IllegalStateException("Offer tile at index " + index + " is null");
        }

        return tile;
    }

    /**
     * Clears the first occupied offer tile that contains the given player
     * color, if present.
     *
     * @param playerColor color to remove from the offer track
     * @throws IllegalArgumentException if the player color is null
     */
    public void clearOccupantByColor(Color playerColor) {
        if (playerColor == null) {
            throw new IllegalArgumentException("playerColor cannot be null");
        }

        for (OfferTile tile : tiles) {
            if (playerColor.equals(tile.getOccupant())) {
                tile.clear();
                return;
            }
        }
    }
}
