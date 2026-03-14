package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class ArtistCard extends CharacterCard {
    public ArtistCard(Era era, int min_players) {
        super(era, min_players);
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.ARTIST;
    }
}