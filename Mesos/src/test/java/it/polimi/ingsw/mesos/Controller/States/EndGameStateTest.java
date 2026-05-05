package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EndGameStateTest extends StateTestSupport {

    private EndGameState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        state = new EndGameState(model_controller);
    }

    @Test
    void getStatusReturnsEndGame() {
        assertEquals(GameStatus.END_GAME, state.getStatus());
    }

    @Test
    void processReturnsNull() {
        GameState nextState = state.process();
        assertNull(nextState);
    }
}
