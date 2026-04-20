package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingBonusEvent;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Result;

import java.util.*;

public class EventState extends GameState {
    private final List<EventCard> events;
    private final boolean last_round;

    /**
     * Builds the event-resolution state, selecting bottom-row events during a
     * normal round or all visible events during the final round.
     *
     * @param controller model controller used to resolve events
     */
    public EventState(ModelController controller) {
        super(controller);
        last_round = controller.getGameContext().getRoundNumber() >= 10;
        events = last_round ? controller.getAllEvents() : controller.getEventsBottom();
        events.sort(Comparator.comparing(EventCard::getEra)); // sort by era to maintain order
    }

    @Override
    public GameState chooseCard(Color player_color, int card, boolean is_top) {
        // TODO: set error in model to warn clients
        return this;
    }

    @Override
    public GameState chooseOffer(Color player_color, int offer_tile) {
        // TODO: set error in model to warn clients
        return this;
    }

    /**
     * Resolves all pending events in event order. Sustenance is delayed until
     * after other events, and event-triggered buildings are applied before
     * outcomes are committed to players.
     *
     * @return the end-game state after final events, otherwise the end-round
     *         state
     */
    @Override
    public GameState process() {
        List<SustenanceEvent> sustenance = new ArrayList<>();
        Map<Color, Tribe> tribes = controller.getTribes();

        while (!events.isEmpty()) {
            EventCard curr = events.removeFirst();
            BuildingTrigger trigger = curr.asTrigger();

            if (trigger == BuildingTrigger.ON_EVENT_SUSTENANCE) {
                sustenance.add((SustenanceEvent) curr);
            } else {
                Map<Color, Result> outcome = curr.calculateOutcome(tribes);
                List<BuildingCard> buildings = controller.getBuildingsWithTrigger(trigger);
                if (trigger == BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL) {
                    buildings.forEach(building -> ((BuildingShamanicRitual) building).effect(outcome));
                } else {
                    buildings.forEach(building -> ((BuildingBonusEvent) building).effect(outcome, controller));
                }
                outcome.forEach((player, result) -> {controller.playerApplyOutcome(result, player);});
            }
        }

        // sustenance events must be done after the other events
        while (!sustenance.isEmpty()) {
            SustenanceEvent event = sustenance.removeFirst();
            BuildingTrigger trigger = event.asTrigger();

            Map<Color, Result> outcome = event.calculateOutcome(tribes);
            List<BuildingBonusEvent> buildings = controller.getBuildingsWithTrigger(trigger)
                    .stream().map(building -> ((BuildingBonusEvent) building)).toList();

            buildings.forEach(building -> building.effect(outcome, controller));
            outcome.forEach((player, result) -> {controller.playerApplyOutcome(result, player);});
        }

        if (last_round) {
            return new EndGameState(controller);
        } else {
            return new EndRoundState(controller);
        }
    }

    @Override
    public GameStatus getStatus() {
        return last_round ? GameStatus.FINAL_EVENTS : GameStatus.ROUND_EVENTS;
    }
}
