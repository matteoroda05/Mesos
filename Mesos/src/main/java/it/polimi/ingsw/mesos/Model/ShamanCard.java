package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class ShamanCard extends CharacterCard {
    private final int num_stars;

    public ShamanCard(Era era, int min_players, int num_stars) {
        super(era, min_players);

        if (num_stars < 1) throw new IllegalArgumentException("Number of stars must be at least 1");

        this.num_stars = num_stars;
    }

    public int getStars() {
        return num_stars;
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.ARTIST;
    }
}