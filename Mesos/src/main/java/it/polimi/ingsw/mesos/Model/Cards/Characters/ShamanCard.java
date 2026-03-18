package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class ShamanCard extends CharacterCard {
    private final int num_stars;

    @JsonCreator
    public ShamanCard(
            @JsonProperty("era")        Era era,
            @JsonProperty("minPlayers") int min_players,
            @JsonProperty("numStars")   int num_stars
    ) {
        super(era, min_players);

        if (num_stars < 1 || num_stars > 3) throw new IllegalArgumentException("Number of stars must be between 1 and 3");

        this.num_stars = num_stars;
    }

    public int getStars() {
        return num_stars;
    }
}