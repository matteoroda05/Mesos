package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class GathererCard extends CharacterCard {
    private final int food_discount;

    @JsonCreator
    public GathererCard(
            @JsonProperty("era") Era era,
            @JsonProperty("minPlayers") int min_players,
            @JsonProperty("foodDiscount") int food_discount
    ) {
        super(era, min_players);

        this.food_discount = 3;
    }

    public int getDiscount() {
        return food_discount;
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.GATHERER;
    }
}