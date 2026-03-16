package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class GathererCard extends CharacterCard {
    private final int food_discount;

    public GathererCard(Era era, int min_players, int food_discount) {
        super(era, min_players);

        if (food_discount <= 0) throw new IllegalArgumentException("Food discount must be positive");

        this.food_discount = food_discount;
    }

    public int getDiscount() {
        return food_discount;
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.GATHERER;
    }
}