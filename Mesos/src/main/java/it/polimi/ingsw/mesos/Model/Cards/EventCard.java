package it.polimi.ingsw.mesos.Model.Cards;

import java.util.Map;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Network.CardSummary;
import it.polimi.ingsw.mesos.Result;


/**
 * Abstract tribe-deck card that calculates a per-player event outcome and
 * identifies the building trigger associated with that event.
 */
abstract public class EventCard extends TribeCard {
    /**
     * Creates an event card, which is available regardless of player count.
     *
     * @param era era this event belongs to
     */
    public EventCard(Era era) {
        super(era, 0);
    }

    /**
     * Calculates this event's outcome for each participating tribe.
     *
     * @param player_tribes tribes keyed by their players' colors
     * @return results keyed by player color
     */
    abstract public Map<Color, Result> calculateOutcome(Map<Color, Tribe> player_tribes);

    /**
     * Returns the building trigger associated with this event type.
     *
     * @return trigger used to activate matching buildings
     */
    abstract public BuildingTrigger asTrigger();

    /**
     * Identifies this card as an event card.
     *
     * @return true for every event card
     */
    @Override
    public boolean isEvent() {
        return true;
    }

    /**
     * Creates the client-facing summary of this event card.
     *
     * @param selectionIndex index used to select this card in its source row
     * @return event summary with no building cost or prestige
     */
    @Override
    public CardSummary getSummary(int selectionIndex) {
        return new CardSummary(
                selectionIndex,
                this.getReadableName(),
                this.getEra(),
                true,
                this.accept(CardDescriptionVisitorImpl.INSTANCE),
                null,
                null,
                this.getPresentationId()
        );
    }
}
