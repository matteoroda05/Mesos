package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
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
     * @throws NullPointerException if the controller is null
     */
    public EventState(ModelController controller) {
        super(controller);
        // TODO: Set back to 10 (debug purposes only)
        last_round = controller.getGameContext().getRoundNumber() >= 2;
        events = last_round ? controller.getAllEvents() : controller.getEventsBottom();
        events.sort(Comparator.comparing(EventCard::getEra)); // sort by era to maintain order
    }

    /**
     * Resolves all pending events in event order. Sustenance is delayed until
     * after other events, and event-triggered buildings are applied before
     * outcomes are committed to players.
     *
     * @return the end-game state after final events, otherwise the end-round
     *         state
     * @throws NullPointerException if a visible event, tribe, event outcome, or
     *                              event-triggered building is unexpectedly
     *                              null
     */
    @Override
    public GameState process() {
        List<SustenanceEvent> sustenance = new ArrayList<>();
        Map<Color, Tribe> tribes = controller.getTribes();

        System.out.println("EVENT STATE");
        printTribeCards(tribes);

        while (!events.isEmpty()) {
            EventCard curr = events.removeFirst();
            BuildingTrigger trigger = curr.asTrigger();

            if (trigger == BuildingTrigger.ON_EVENT_SUSTENANCE) {
                sustenance.add((SustenanceEvent) curr);
            } else {
                System.out.println("Current event: " + formatCard(curr));
                Map<Color, Result> outcome = curr.calculateOutcome(tribes);
                List<BuildingCard> buildings = controller.getBuildingsWithTrigger(trigger);
                if (trigger == BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL) {
                    buildings.forEach(building -> ((BuildingShamanicRitual) building).effect(outcome));
                } else {
                    buildings.forEach(building -> ((BuildingBonusEvent) building).effect(outcome, controller));
                }
                outcome.forEach((player, result) -> controller.playerApplyOutcome(result, player));
            }
        }

        // sustenance events must be done after the other events
        while (!sustenance.isEmpty()) {
            SustenanceEvent event = sustenance.removeFirst();
            BuildingTrigger trigger = event.asTrigger();

            System.out.println("Current event: " + formatCard(event));
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

    /**
     * Reports whether this state is resolving normal round events or final
     * events.
     *
     * @return final-events status in the last round, otherwise round-events
     *         status
     */
    @Override
    public GameStatus getStatus() {
        return last_round ? GameStatus.FINAL_EVENTS : GameStatus.ROUND_EVENTS;
    }

    // TODO: REMOVE THIS FUNCTION USEFUL: FOR DEBUG ONLY
    private static void printTribeCards(Map<Color, Tribe> tribes) {
        tribes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.println(entry.getKey());
                    List<Card> cards = entry.getValue().getCards();
                    if (cards.isEmpty()) {
                        System.out.println("  (no cards)");
                    } else {
                        cards.forEach(card -> System.out.println("  - " + formatCard(card)));
                    }
                });
    }

    private static String formatCard(Card card) {
        return card.getClass().getSimpleName() + " (" + card.getEra() + ")";
    }
}
