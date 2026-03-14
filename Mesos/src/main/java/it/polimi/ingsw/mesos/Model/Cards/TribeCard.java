package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Card;
import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class TribeCard extends Card {
    public TribeCard(Era era, int min_players) {
        super(era, min_players);
    }
}