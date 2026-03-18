package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class CharacterCard extends TribeCard {
    public CharacterCard(Era era, int min_players) {
        super(era, min_players);
    }
}