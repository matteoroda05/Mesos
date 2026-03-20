package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class Card {
    private final Era era;
    // private final Image image;
    private final int min_players;

    public Card(Era era, int min_players) {
        if (min_players < 0 || min_players > 5) throw new IllegalArgumentException("min_players must be between 0 and 5");

        this.era = era;
        this.min_players = min_players;
    }

    public Era getEra(){
        return this.era;
    }

    public int getMinPlayers() {
        return this.min_players;
    }

    public boolean isEvent() {
        return false;
    }
}