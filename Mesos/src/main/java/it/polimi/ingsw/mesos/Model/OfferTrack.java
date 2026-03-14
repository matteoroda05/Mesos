package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Enums.Color;

import java.util.List;
import java.util.stream.Collectors;

public class OfferTrack {
    private List<OfferTile> tiles;

    // TODO: populate the track with the correct number of tiles
    public OfferTrack(int num_players) {
    }

    // TODO: decide how to implement offertiles (private class inside offertrack or
    // separate class) AND finish functions

    public List<OfferTile> getAvailableTiles() {
        List<OfferTile> output = this.tiles.stream()
                .filter(tile -> tile.isAvailable())
                .collect(Collectors.toList());

        if (output.isEmpty()) {
            throw new IllegalStateException("No available offer tiles: impossible state");
        }

        return output;
    }

    public List<OfferTile> getOccupiedTiles() {
        List<OfferTile> output = this.tiles.stream()
                .filter(tile -> !tile.isAvailable())
                .collect(Collectors.toList());

        return output;
    }
}
