package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class ArtistCard extends CharacterCard {

    @JsonCreator
    public ArtistCard(
            @JsonProperty("era")        Era era,
            @JsonProperty("minPlayers") int min_players
    ) {
        super(era, min_players);
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.ARTIST;
    }
}