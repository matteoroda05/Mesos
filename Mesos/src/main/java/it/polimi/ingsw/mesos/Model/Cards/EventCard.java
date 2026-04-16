package it.polimi.ingsw.mesos.Model.Cards;

import java.util.Map;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Result;


abstract public class EventCard extends TribeCard {
    public EventCard(Era era) {
        super(era, 0);
    }

    /**
     * Calculates the resource and prestige outcome of this event for every
     * participating tribe without directly mutating player state.
     *
     * @param player_tribes tribes keyed by player color
     * @return event outcome keyed by player color
     */
    abstract public Map<Color, Result> calculateOutcome(Map<Color, Tribe> player_tribes);

    /**
     * Converts this event type to the building trigger used by buildings that
     * react to the event.
     *
     * @return matching building trigger
     */
    abstract public BuildingTrigger asTrigger();

    @Override
    public boolean isEvent() {
        return true;
    }
}
