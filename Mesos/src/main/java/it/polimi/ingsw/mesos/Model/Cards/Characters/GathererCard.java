package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class GathererCard extends CharacterCard {
    @JsonCreator
    public GathererCard(
            @JsonProperty("era") Era era,
            @JsonProperty("minPlayers") int min_players
    ) {
        super(era, min_players);
    }

    public int getDiscount() {
        return 3;
    }
}