package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class ArtistCard extends CharacterCard {

    /**
     * Creates an artist card from JSON data.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range accepted by the base card
     */
    @JsonCreator
    public ArtistCard(
            @JsonProperty("era")        Era era,
            @JsonProperty("minPlayers") int min_players
    ) {
        super(era, min_players);
    }
}
