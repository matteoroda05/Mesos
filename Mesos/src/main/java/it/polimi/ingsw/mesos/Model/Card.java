package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class Card implements Comparable<Card> {
    private final int card_id;
    private final Era era;
    // private final Image image;
    private final int min_players;

    public Card(Era era, int min_players) {
        if (min_players < 0 || min_players > 5) throw new IllegalArgumentException("min_players must be between 0 and 5");
        
        // TODO: use Integer or long for card_id instead of UUID
        this.card_id = UUID.randomUUID();
        this.era = era;
        this.min_players = min_players;
    }

    public Era getEra(){
        return this.era;
    }

    public int getCardId() {
        return this.card_id;
    }

    public int getMinPlayers() {
        return this.min_players;
    }

    public boolean isEvent() {
        return false;
    }

    @Override
    public int compareTo(Card other) {
        if (other == null) throw new IllegalArgumentException("Cannot compare to null");
        int eraComparison = this.era.compareTo(other.era);
        if (eraComparison != 0) {
            return eraComparison;
        }
        return Integer.compare(this.card_id, other.card_id);
    }

}