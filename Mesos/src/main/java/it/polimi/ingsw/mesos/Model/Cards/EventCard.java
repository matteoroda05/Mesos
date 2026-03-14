package it.polimi.ingsw.mesos.Model.Cards;

import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;

abstract public class EventCard extends TribeCard {
    public EventCard(Era era) {
        super(era, 0);
    }

    abstract public List<Integer> calculateOutcome(List<Tribe> player_tribes);

    abstract public BuildingTrigger asTrigger();
}