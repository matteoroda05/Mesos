package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class Card {
    private final Era era;
    private final int min_players;
    private String presentationId;

    public Card(Era era, int min_players) {
        if (min_players < 0 || min_players > 5) throw new IllegalArgumentException("min_players must be between 0 and 5");

        this.era = era;
        this.min_players = min_players;
        this.presentationId = null;
    }

    public Era getEra(){
        return this.era;
    }

    public int getMinPlayers() {
        return this.min_players;
    }

    @JsonProperty("presentation_id")
    public String getPresentationId() {
        return presentationId;
    }

    @JsonProperty("presentation_id")
    public void setPresentationId(String presentationId) {
        this.presentationId = (presentationId == null || presentationId.isBlank())
                ? null
                : presentationId;
    }

    public boolean isEvent() {
        return false;
    }
}
