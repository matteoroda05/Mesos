package it.polimi.ingsw.mesos.Model;

//TODO: INTERFACCIA SI O NO? COME?

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;

public interface BuildingEffect {
    public boolean effect(BuildingCard building_card);
}