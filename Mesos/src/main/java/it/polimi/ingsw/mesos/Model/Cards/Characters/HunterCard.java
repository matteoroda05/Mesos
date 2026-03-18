package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class HunterCard extends CharacterCard {
    private final boolean active_hunter;

    @JsonCreator
    public HunterCard(
            @JsonProperty("era")          Era era,
            @JsonProperty("minPlayers")   int min_players,
            @JsonProperty("activeHunter") boolean active_hunter
    ) {
        super(era, min_players);
        this.active_hunter = active_hunter;
    }

    public boolean isactive_hunter() {
        return active_hunter;
    }
}