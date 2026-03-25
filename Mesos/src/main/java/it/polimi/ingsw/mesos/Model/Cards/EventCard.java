package it.polimi.ingsw.mesos.Model.Cards;

import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;

abstract public class EventCard extends TribeCard {
    public EventCard(Era era) {
        super(era, 0);
    }

    abstract public Map<Color, Integer> calculateOutcome(Map<Color, Tribe> player_tribes);

    abstract public BuildingTrigger asTrigger();

    @Override
    public boolean isEvent() {
        return true;
    }
}