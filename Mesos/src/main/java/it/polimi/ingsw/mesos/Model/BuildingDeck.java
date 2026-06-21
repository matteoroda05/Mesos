package it.polimi.ingsw.mesos.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.model.cards.BuildingCard;
import it.polimi.ingsw.mesos.model.enums.Era;

/**
 * Stores building cards by era and supplies the next era's full building set
 * when the market advances.
 */
public class BuildingDeck {
    private final Map<Era, List<BuildingCard>> deck;

    /**
     * Builds a deck grouped by building era.
     *
     * @param buildingCards building cards to include
     * @throws IllegalArgumentException if the list is null or contains a null
     *                                  card
     */
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

    /**
     * Draws all building cards for the earliest era that still has buildings
     * in the deck, then removes that era from future draws.
     *
     * @return the building cards for the next available era
     * @throws IllegalStateException if the deck storage is unexpectedly null
     * @throws IllegalAccessError if no era still has building cards
     */
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

    /**
     * Checks whether every era group has already been drawn.
     *
     * @return true when no building cards remain in the deck
     * @throws IllegalStateException if the deck storage is unexpectedly null
     */
    public boolean isEmpty() {
        if (deck == null) {
            throw new IllegalStateException("Building deck is not initialized");
        }
        return deck.isEmpty();
    }
}
