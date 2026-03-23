package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public class BuildingDeck {
    private Map<Era, List<BuildingCard>> deck;

    public BuildingDeck(List<BuildingCard> buildingCards) {
        if (buildingCards == null) {
            throw new IllegalArgumentException("buildingCards cannot be null");
        }

        this.deck = new EnumMap<>(Era.class);

        for (BuildingCard card : buildingCards) {
            if (card == null) {
                throw new IllegalArgumentException("buildingCards cannot contain null cards");
            }

            deck.computeIfAbsent(card.getEra(), era -> new ArrayList<>()).add(card);
        }
    }

    public List<BuildingCard> drawBuildingCards() {
        if (deck == null) {
            throw new IllegalStateException("Building deck is not initialized");
        }

        List<BuildingCard> output = null;

        for (Era era : Era.values()) {
            List<BuildingCard> era_cards = deck.get(era);
            if (era_cards != null && !era_cards.isEmpty()) {
                output = era_cards;
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
