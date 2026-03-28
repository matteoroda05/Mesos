package it.polimi.ingsw.mesos.Controller.States;

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

import java.util.*;

public class EventState extends GameState {
    private final List<EventCard> events;
    private final boolean last_round;

    public EventState(ModelController controller) {
        super(controller);
        last_round = round >= 10;
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
                Map<Color, Integer> outcome = curr.calculateOutcome(tribes);
                List<BuildingCard> buildings = controller.getBuildingsWithTrigger(trigger);
                if (trigger == BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL) {
                    buildings.forEach(building -> ((BuildingShamanicRitual) building).effect(outcome));
                } else {
                    buildings.forEach(building -> ((BuildingBonusEvent) building).effect(outcome, controller));
                }

                applyOutcome(outcome);
            }
        }

        // sustenance events must be done after the other events
        while (!sustenance.isEmpty()) {
            SustenanceEvent event = sustenance.removeFirst();
            BuildingTrigger trigger = event.asTrigger();

            Map<Color, Integer> outcome = event.calculateOutcome(tribes);
            List<BuildingBonusEvent> buildings = controller.getBuildingsWithTrigger(trigger)
                    .stream().map(building -> ((BuildingBonusEvent) building)).toList();

            buildings.forEach(building -> building.effect(outcome, controller));
            applyOutcome(outcome, event.getPointPenalty());
        }

        if (last_round) {
            return new EndGameState(controller);
        } else {
            return new EndRoundState(controller);
        }
    }

    // TODO: fix for hunter's food reward
    private void applyOutcome(Map<Color, Integer> outcome) {
        outcome.forEach((player, delta) -> {
            if (delta != 0) {
                controller.playerChangePrestige(delta, player);
            }
        });
    }

    private void applyOutcome(Map<Color, Integer> outcome, int prestige_convertion) {
        outcome.forEach((player, delta) -> {
            if (delta != 0) {
                controller.playerChangeFood(delta, player, prestige_convertion);
            }
        });
    }
}
