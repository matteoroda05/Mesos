package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndRoundStateTest extends StateTestSupport {

    private EndRoundState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        state = new EndRoundState(model_controller);
    }

    @Test
    void getStatusReturnsRoundEnd() {
        assertEquals(GameStatus.ROUND_END, state.getStatus());
    }

    @Test
    void processMovesToChooseOfferState() {
        int initialRound = game_context.getRoundNumber();
        GameState nextState = state.process();

        assertInstanceOf(ChooseOfferState.class, nextState);
        assertEquals(initialRound + 1, game_context.getRoundNumber());
    }
}
