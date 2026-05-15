package it.polimi.ingsw.mesos.Model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;

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
        ensureTilesInitialized();

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
        ensureTilesInitialized();

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
     * @throws IllegalStateException if the tile list is unexpectedly null or
     *                               the requested entry is null
     */
    public OfferTile getTile(int index){
        ensureTilesInitialized();

        if (index < 0 || index >= this.tiles.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for offer track with size " + tiles.size());
        }

        OfferTile tile = this.tiles.get(index);
        if (tile == null) {
            throw new IllegalStateException("Offer tile at index " + index + " is null");
        }

        return tile;
    }
    
    // TODO: remove since constructor ensures tiles is never null since its never modified
    /**
     * Verifies that the track storage has been initialized.
     *
     * @throws IllegalStateException if the tile list is unexpectedly null
     */
    private void ensureTilesInitialized() {
        if (tiles == null) {
            throw new IllegalStateException("Offer track tiles are not initialized");
        }
    }
}
