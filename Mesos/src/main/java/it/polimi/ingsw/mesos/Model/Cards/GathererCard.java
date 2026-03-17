package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class GathererCard extends CharacterCard {
    private final int food_discount; 

    public GathererCard(Era era, int min_players) {
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