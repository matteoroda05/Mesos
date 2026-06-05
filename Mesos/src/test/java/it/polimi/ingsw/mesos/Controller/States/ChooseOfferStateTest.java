package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChooseOfferStateTest extends StateTestSupport {

    private ChooseOfferState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        state = new ChooseOfferState(model_controller);
    }

    @Test
    void getStatusReturnsRoundOffer() {
        assertEquals(GameStatus.ROUND_OFFER, state.getStatus());
    }

    @Test
    void getActivePlayerReturnsFirstInTurnOrder() {
        Color firstPlayer = game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().getFirst();
        assertEquals(firstPlayer, state.getActivePlayer());
    }

    @Test
    void chooseOfferValidPlayerMovesToNextPlayer() {
        Color firstPlayer = state.getActivePlayer();
        GameState nextState = state.chooseOffer(firstPlayer, 0);
        
        assertSame(state, nextState);
        assertNotEquals(firstPlayer, state.getActivePlayer());
    }

    @Test
    void chooseOfferInvalidPlayerThrows() {
        Color firstPlayer = state.getActivePlayer();
        Color otherPlayer = player_colors.stream().filter(c -> c != firstPlayer).findFirst().orElseThrow();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> state.chooseOffer(otherPlayer, 0)
        );

        assertTrue(exception.getMessage().contains("Wrong turn"));
        assertEquals(firstPlayer, state.getActivePlayer());
    }

    @Test
    void chooseOfferLastPlayerMovesToActionState() {
        int numPlayers = player_colors.size();
        GameState currentState = state;
        
        for (int i = 0; i < numPlayers - 1; i++) {
            Color currentPlayer = currentState.getActivePlayer();
            currentState = currentState.chooseOffer(currentPlayer, i);
        }
        
        Color lastPlayer = currentState.getActivePlayer();
        GameState finalState = currentState.chooseOffer(lastPlayer, numPlayers - 1);

        assertInstanceOf(ActionState.class, finalState);
    }

    @Test
    void skipTurnMovesToNextPlayerAndEventuallyActionState() {
        Color firstPlayer = state.getActivePlayer();
        GameState nextState = state.skipTurn(firstPlayer);

        assertSame(state, nextState);
        assertNotEquals(firstPlayer, state.getActivePlayer());

        while (nextState instanceof ChooseOfferState) {
            nextState = nextState.skipTurn(nextState.getActivePlayer());
        }

        assertInstanceOf(ActionState.class, nextState);
    }

    @Test
    void skipTurnInvalidPlayerThrows() {
        Color firstPlayer = state.getActivePlayer();
        Color otherPlayer = player_colors.stream().filter(c -> c != firstPlayer).findFirst().orElseThrow();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> state.skipTurn(otherPlayer)
        );

        assertTrue(exception.getMessage().contains("Wrong turn"));
        assertEquals(firstPlayer, state.getActivePlayer());
    }

    @Test
    void chooseCardThrowsBecauseWrongGameState() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> state.chooseCard(state.getActivePlayer(), 0, true)
        );

        assertTrue(exception.getMessage().contains("wrong game state"));
    }
}
