package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
