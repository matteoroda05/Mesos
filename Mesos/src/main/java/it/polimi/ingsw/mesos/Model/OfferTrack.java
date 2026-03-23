package it.polimi.ingsw.mesos.Model;

import java.util.List;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;

public class OfferTrack {
    private final List<OfferTile> tiles;

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

        if (offer_tiles.stream().anyMatch(tile -> tile == null)) {
            throw new IllegalArgumentException("Offer track contains null tile entries");
        }

        if (offer_tiles.stream().map(tile -> tile.getId()).distinct().count() != offer_tiles.size()) {
            throw new IllegalArgumentException("Offer track contains duplicate tiles");
        }

        if (offer_tiles.stream().anyMatch(tile -> tile.getMinNumPlayers() > num_players)) {
            throw new IllegalArgumentException("Offer track contains tiles that cannot be used with the current number of players");
        }

        this.tiles = offer_tiles;
    }

    public List<OfferTile> getAvailableTiles() {
        ensureTilesInitialized();

        List<OfferTile> output = this.tiles.stream()
                .filter(tile -> tile != null)
                .filter(tile -> tile.isAvailable())
                .collect(Collectors.toList());

        if (output.isEmpty()) {
            throw new IllegalStateException("No available offer tiles: impossible state");
        }

        return output;
    }

    public List<OfferTile> getOccupiedTiles() {
        ensureTilesInitialized();

        List<OfferTile> output = this.tiles.stream()
                .filter(tile -> tile != null)
                .filter(tile -> !tile.isAvailable())
                .collect(Collectors.toList());

        return output;
    }

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
    
    private void ensureTilesInitialized() {
        if (tiles == null) {
            throw new IllegalStateException("Offer track tiles are not initialized");
        }
    }
}