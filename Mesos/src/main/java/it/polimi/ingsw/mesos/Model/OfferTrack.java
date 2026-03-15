package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.Globals;

public class OfferTrack {
    private List<OfferTile> tiles;

    public OfferTrack(int num_players) {
        if (num_players < Globals.MIN_NUM_PLAYERS || num_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "numPlayers must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }

        this.tiles = new ArrayList<>();
        
        // TODO: Get offer tiles from loader 
        {}

        if (this.tiles.size() != num_players + 2){
            throw new IllegalStateException("There must be " + num_players +" + 2 tiles");
        }
    }

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

    public OfferTile getTile(int index){
        return tiles.get(index);
    }
}