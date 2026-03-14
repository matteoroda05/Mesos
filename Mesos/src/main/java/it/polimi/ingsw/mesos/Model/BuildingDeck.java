package it.polimi.ingsw.mesos.Model;

import java.util.List;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class BuildingDeck {
    private List<BuildingCard> deck;

    // TODO: initialise deck with cards from JSON file
    public BuildingDeck(int n_players) {
        if (n_players < 2 || n_players > 5) throw new IllegalArgumentException("Number of players must be between 2 and 5");
    }

    public List<BuildingCard> getBuildingCards(Era era) {
        List<BuildingCard> output = this.deck.stream()
            .filter(card -> card.getEra() == era)
            .collect(Collectors.toList());
        
        return output;
    }

    public boolean isEmpty() {
        return deck.isEmpty();
    }
}