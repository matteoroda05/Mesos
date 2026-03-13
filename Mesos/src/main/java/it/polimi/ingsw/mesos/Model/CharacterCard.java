package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class CharacterCard extends TribeCard {
    public CharacterCard(Era era, int min_players) {
        super(era, min_players);
    }

    abstract public CharacterType getCharacterType();
}