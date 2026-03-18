package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class BuilderCard extends CharacterCard {
    private final int prestige_points;
    private final int food_discount;

    @JsonCreator
    public BuilderCard(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("prestigePoints") int prestige_points,
            @JsonProperty("foodDiscount")   int food_discount
    ) {
        super(era, min_players);

        if (prestige_points < 0 || prestige_points > 5) throw new IllegalArgumentException("Prestige points cannot be negative or greater than 5");
        if (food_discount < 1 || food_discount > 2) throw new IllegalArgumentException("Food discount has to be between 1 and 2");

        this.prestige_points = prestige_points;
        this.food_discount = food_discount;
    }

    public int getPrestigePoints() {
        return prestige_points;
    }

    public int getBuildingDiscount() {
        return food_discount;
    }
}