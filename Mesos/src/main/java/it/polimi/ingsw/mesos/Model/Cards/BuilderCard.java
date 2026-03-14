package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class BuilderCard extends CharacterCard {
    private final int prestige_points;
    private final int food_discount;

    public BuilderCard(Era era, int min_players, int prestige_points, int food_discount) {
        super(era, min_players);

        if (prestige_points < 0) throw new IllegalArgumentException("Prestige points cannot be negative");
        if (food_discount < 0) throw new IllegalArgumentException("Food discount cannot be negative");

        this.prestige_points = prestige_points;
        this.food_discount = food_discount;
    }

    public int getPrestigePoints() {
        return prestige_points;
    }

    public int getBuildingDiscount() {
        return food_discount;
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.ARTIST;
    }
}