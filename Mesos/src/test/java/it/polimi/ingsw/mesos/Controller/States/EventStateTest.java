package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingBonusEvent;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EventStateTest extends StateTestSupport {

    private EventState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        state = new EventState(model_controller);
    }

    @Test
    void getStatusReturnsRoundEventsWhenNotLastRound() {
        assertEquals(GameStatus.ROUND_EVENTS, state.getStatus());
    }

    @Test
    void processMovesToEndRoundStateWhenNotLastRound() {
        GameState nextState = state.process();
        assertInstanceOf(EndRoundState.class, nextState);
    }

    @Test
    void processMovesToEndGameStateWhenLastRound() {
        // Force last round
        for (int i = 0; i < 10; i++) {
            game_context.newRound();
        }
        assertEquals(10, game_context.getRoundNumber());
        
        EventState lastRoundState = new EventState(model_controller);
        assertEquals(GameStatus.FINAL_EVENTS, lastRoundState.getStatus());
        
        GameState nextState = lastRoundState.process();
        assertInstanceOf(EndGameState.class, nextState);
    }

    @Test
    void processAppliesEventBuildingsAndDelaysSustenance() {
        CountingBonusBuilding huntBuilding = new CountingBonusBuilding(BuildingTrigger.ON_EVENT_HUNT);
        CountingBonusBuilding sustenanceBuilding = new CountingBonusBuilding(BuildingTrigger.ON_EVENT_SUSTENANCE);
        CountingShamanicBuilding ritualBuilding = new CountingShamanicBuilding();

        List<EventCard> bottomEvents = List.of(
                new SustenanceEvent(Era.FIRST, -1, 1),
                new HuntEvent(Era.SECOND, 1, 1),
                new ShamanicRitualEvent(Era.THIRD, 5, -3)
        );
        EventSpyController spy = new EventSpyController(game_context, bottomEvents, List.of());
        spy.addBuilding(BuildingTrigger.ON_EVENT_HUNT, huntBuilding);
        spy.addBuilding(BuildingTrigger.ON_EVENT_SUSTENANCE, sustenanceBuilding);
        spy.addBuilding(BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL, ritualBuilding);

        GameState nextState = new EventState(spy).process();

        assertInstanceOf(EndRoundState.class, nextState);
        assertEquals(1, huntBuilding.calls);
        assertEquals(1, sustenanceBuilding.calls);
        assertEquals(1, ritualBuilding.calls);
        assertTrue(spy.appliedResults.size() >= player_colors.size() * 3);
        assertEquals(List.of("hunt", "sustenance"), spy.effectOrder);
    }

    @Test
    void finalRoundUsesAllVisibleEvents() {
        for (int i = 0; i < 10; i++) {
            game_context.newRound();
        }
        EventSpyController spy = new EventSpyController(
                game_context,
                List.of(),
                List.of(new HuntEvent(Era.FIRST, 1, 1))
        );

        GameState nextState = new EventState(spy).process();

        assertInstanceOf(EndGameState.class, nextState);
        assertEquals(player_colors.size(), spy.appliedResults.size());
    }

    private static final class EventSpyController extends ModelController {
        private final List<EventCard> bottomEvents;
        private final List<EventCard> allEvents;
        private final Map<BuildingTrigger, List<BuildingCard>> buildings = new EnumMap<>(BuildingTrigger.class);
        private final Map<Color, Tribe> tribes;
        private final List<Result> appliedResults = new ArrayList<>();
        private final List<String> effectOrder = new ArrayList<>();

        private EventSpyController(GameContext context, List<EventCard> bottomEvents, List<EventCard> allEvents) {
            super(context);
            this.bottomEvents = new ArrayList<>(bottomEvents);
            this.allEvents = new ArrayList<>(allEvents);
            this.tribes = context.getPlayers().stream()
                    .collect(Collectors.toMap(player -> player.getColor(), player -> player.getTribe()));
        }

        private void addBuilding(BuildingTrigger trigger, BuildingCard building) {
            buildings.computeIfAbsent(trigger, ignored -> new ArrayList<>()).add(building);
        }

        @Override
        public List<EventCard> getEventsBottom() {
            return new ArrayList<>(bottomEvents);
        }

        @Override
        public List<EventCard> getAllEvents() {
            return new ArrayList<>(allEvents);
        }

        @Override
        public Map<Color, Tribe> getTribes() {
            return tribes;
        }

        @Override
        public List<BuildingCard> getBuildingsWithTrigger(BuildingTrigger trigger) {
            return buildings.getOrDefault(trigger, List.of());
        }

        @Override
        public void playerApplyOutcome(Result outcome, Color player_color) {
            appliedResults.add(outcome);
        }
    }

    private static final class CountingBonusBuilding extends BuildingBonusEvent {
        private int calls;

        private CountingBonusBuilding(BuildingTrigger trigger) {
            super(Era.FIRST, 2, 0, 0, 1, 0, HunterCard.class, trigger);
        }

        @Override
        public void effect(Map<Color, Result> output, ModelController visitor) {
            calls++;
            ((EventSpyController) visitor).effectOrder.add(
                    getTrigger() == BuildingTrigger.ON_EVENT_SUSTENANCE ? "sustenance" : "hunt"
            );
        }
    }

    private static final class CountingShamanicBuilding extends BuildingShamanicRitual {
        private int calls;

        private CountingShamanicBuilding() {
            super(Era.FIRST, 2, 0, 0, 0, 2);
        }

        @Override
        public void effect(Map<Color, Result> points) {
            calls++;
        }
    }
}
