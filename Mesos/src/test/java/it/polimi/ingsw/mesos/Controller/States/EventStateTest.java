package it.polimi.ingsw.mesos.controller.states;

import it.polimi.ingsw.mesos.controller.enums.GameStatus;
import it.polimi.ingsw.mesos.controller.GameState;
import it.polimi.ingsw.mesos.controller.ModelController;
import it.polimi.ingsw.mesos.model.cards.BuildingCard;
import it.polimi.ingsw.mesos.model.cards.buildings.BuildingBonusEvent;
import it.polimi.ingsw.mesos.model.cards.buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.model.cards.characters.HunterCard;
import it.polimi.ingsw.mesos.model.cards.EventCard;
import it.polimi.ingsw.mesos.model.cards.events.HuntEvent;
import it.polimi.ingsw.mesos.model.cards.events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.model.cards.events.SustenanceEvent;
import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.GameContext;
import it.polimi.ingsw.mesos.model.Player;
import it.polimi.ingsw.mesos.model.Tribe;
import it.polimi.ingsw.mesos.dto.event.AppliedDelta;
import it.polimi.ingsw.mesos.dto.event.EventLogEntry;
import it.polimi.ingsw.mesos.dto.event.EventMessageFormatter;
import it.polimi.ingsw.mesos.model.Result;
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

    @Test
    void processInFinalRoundLogsFinalEventsWithAppliedLinesAndOwnedBuildingNote() {
        for (int i = 0; i < 10; i++) {
            game_context.newRound();
        }
        CountingBonusBuilding huntBuilding = new CountingBonusBuilding(BuildingTrigger.ON_EVENT_HUNT);
        huntBuilding.setOwner(game_context.getPlayer(player_colors.getFirst()));
        EventSpyController spy = new EventSpyController(
                game_context,
                List.of(),
                List.of(new HuntEvent(Era.FIRST, 1, 1)));
        spy.appliedDelta = new AppliedDelta(2, 1);
        spy.addBuilding(BuildingTrigger.ON_EVENT_HUNT, huntBuilding);

        GameState nextState = new EventState(spy).process();

        assertInstanceOf(EndGameState.class, nextState);
        EventLogEntry entry = game_context.drainEventLog().getLast();
        assertEquals("Final events", entry.title());
        Player owner = game_context.getPlayer(player_colors.getFirst());
        assertTrue(entry.body().contains("from player "
                + EventMessageFormatter.playerLabel(owner.getNickname(), owner.getColor())));
    }

    private static final class EventSpyController extends ModelController {
        private final List<EventCard> bottomEvents;
        private final List<EventCard> allEvents;
        private final Map<BuildingTrigger, List<BuildingCard>> buildings = new EnumMap<>(BuildingTrigger.class);
        private final Map<Color, Tribe> tribes;
        private final List<Result> appliedResults = new ArrayList<>();
        private final List<String> effectOrder = new ArrayList<>();
        private AppliedDelta appliedDelta = new AppliedDelta(0, 0);

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
        public AppliedDelta playerApplyOutcome(Result outcome, Color player_color) {
            appliedResults.add(outcome);
            return appliedDelta;
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
