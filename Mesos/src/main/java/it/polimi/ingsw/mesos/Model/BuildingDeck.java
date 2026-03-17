package it.polimi.ingsw.mesos.Model;

import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public class BuildingDeck {
    private Map<Era, List<BuildingCard>> deck;

    // TODO: initialise deck with cards from JSON file

    public BuildingDeck(int num_players) {
        if (num_players < Globals.MIN_NUM_PLAYERS || num_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "numPlayers must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
    }

    public List<BuildingCard> drawBuildingCards() {
        if (deck == null) {
            throw new IllegalStateException("Building deck is not initialized");
        }

        List<BuildingCard> output = null;

        for (Era era : Era.values()) {
            if (!deck.get(era).isEmpty()) {
                output = deck.get(era);
                deck.remove(era);        
                break;
            }
        }

        if (output == null) {
            throw new IllegalAccessError("No buildings left: all eras already done.");
        }

        return output;
    }

    public boolean isEmpty() {
        if (deck == null) {
            throw new IllegalStateException("Building deck is not initialized");
        }
        return deck.isEmpty();
    }
}