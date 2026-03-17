package it.polimi.ingsw.mesos.Model;

import java.util.UUID;

import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class Card {
    private final UUID card_id;
    public final Era era;
    // Image image;
    public final int min_players;

    public Card(Era era, int min_players) {
        if (min_players < 0 || min_players > 5) throw new IllegalArgumentException("min_players must be non-negative");
        // TODO: use Integer or long for card_id instead of UUID
        this.card_id = UUID.randomUUID();
        this.era = era;
        this.min_players = min_players;
    }

    public Era getEra(){
        return this.era;
    }
    public boolean isEvent() {
    return false;
    }
}