package it.polimi.ingsw.mesos.Model;

import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class BuildingDeck {
    private final Era era;
    private List<BuildingCard> building_cards;

    public BuildingDeck(Era era) {
        this.era = era;
    }

    public List<BuildingCard> getBuildingCards() {
        return building_cards;
    }

    public boolean isEmpty() {
        return building_cards.isEmpty();
    }
}