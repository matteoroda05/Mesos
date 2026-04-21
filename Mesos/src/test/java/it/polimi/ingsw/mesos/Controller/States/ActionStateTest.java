package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionStateTest extends StateTestSupport {

    private ActionState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // Occupy an offer tile so ActionState constructor doesn't fail
        Color firstPlayer = player_colors.getFirst();
        // game_context.getGameTable().getOfferTrack().getTile(0).occupy(firstPlayer);
        state = new ActionState(model_controller);
    }

    @Test
    void getStatusReturnsRoundActions() {
        assertEquals(GameStatus.ROUND_ACTIONS, state.getStatus());
    }

    @Test
    void chooseCardInvalidPlayerReturnsSameState() {
        Color otherPlayer = player_colors.get(1);
        GameState nextState = state.chooseCard(otherPlayer, 0, true);
        assertSame(state, nextState);
    }

    @Test
    void chooseOfferReturnsSameState() {
        GameState nextState = state.chooseOffer(state.getActivePlayer(), 0);
        assertSame(state, nextState);
    }
    
    @Test
    void chooseCardDecrementsPicks() {
        // Offer tile 0 has some picks. Let's check.
        int initialTop = state.getRemainingTopChoices();
        int initialBottom = state.getRemainingBottomChoices();
        
        Color p = state.getActivePlayer();
        
        if (initialTop > 0) {
            state.chooseCard(p, 0, true);
            assertEquals(initialTop - 1, state.getRemainingTopChoices());
        } else if (initialBottom > 0) {
            state.chooseCard(p, 0, false);
            assertEquals(initialBottom - 1, state.getRemainingBottomChoices());
        }
    }
}
