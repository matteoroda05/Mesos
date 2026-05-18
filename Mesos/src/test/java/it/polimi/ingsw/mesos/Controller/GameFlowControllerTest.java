package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.GameContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameFlowControllerTest {
    @Test
    void constructorRejectsNullContext() {
        assertThrows(IllegalArgumentException.class, () -> new GameFlowController(null));
    }

    @Test
    void commandsFailAfterGameIsMarkedFinished() throws Exception {
        GameSetup setup = new GameSetup();
        GameContext context = setup.createGame("FlowGuards", 2, CardLoader.load());
        setup.createMapOfPlayers(context, List.of("Alice", "Bob"));
        GameFlowController controller = new GameFlowController(context);
        assertFalse(controller.isGameFinished());

        Field currentState = GameFlowController.class.getDeclaredField("current_state");
        currentState.setAccessible(true);
        currentState.set(controller, null);

        assertTrue(controller.isGameFinished());
        assertThrows(IllegalStateException.class, () -> controller.chooseOffer(Color.BLUE, 0));
        assertThrows(IllegalStateException.class, () -> controller.chooseCard(Color.BLUE, 0, true));
        assertThrows(IllegalStateException.class, () -> controller.pass(Color.BLUE));
        assertThrows(IllegalStateException.class, () -> controller.skipCurrentTurn(Color.BLUE));
    }

    @Test
    void chooseOfferStoresReturnedStateAndAdvancesUntilInputIsNeeded() throws Exception {
        GameFlowController controller = controllerWithPlayers("FlowOffer");
        ModelController modelController = new ModelController(controller.getCurrentState().controller.getGameContext());
        StubState commandState = new StubState(modelController, GameStatus.ROUND_OFFER);
        StubState automaticState = new StubState(modelController, GameStatus.ROUND_EVENTS);
        StubState waitingState = new StubState(modelController, GameStatus.ROUND_ACTIONS);
        commandState.offerNext = automaticState;
        automaticState.processNext = waitingState;
        waitingState.processNext = waitingState;
        setCurrentState(controller, commandState);

        controller.chooseOffer(Color.BLUE, 1);

        assertSame(waitingState, controller.getCurrentState());
        assertTrue(commandState.chooseOfferCalled);
        assertTrue(automaticState.processCalled);
        assertTrue(waitingState.processCalled);
    }

    @Test
    void chooseCardCanAdvanceGameToFinished() throws Exception {
        GameFlowController controller = controllerWithPlayers("FlowCard");
        ModelController modelController = new ModelController(controller.getCurrentState().controller.getGameContext());
        StubState commandState = new StubState(modelController, GameStatus.ROUND_ACTIONS);
        StubState automaticState = new StubState(modelController, GameStatus.FINAL_EVENTS);
        commandState.cardNext = automaticState;
        automaticState.processNext = null;
        setCurrentState(controller, commandState);

        controller.chooseCard(Color.BLUE, 0, true);

        assertTrue(commandState.chooseCardCalled);
        assertTrue(automaticState.processCalled);
        assertTrue(controller.isGameFinished());
    }

    @Test
    void skipCurrentTurnStoresReturnedStateAndAdvancesUntilInputIsNeeded() throws Exception {
        GameFlowController controller = controllerWithPlayers("FlowSkip");
        ModelController modelController = new ModelController(controller.getCurrentState().controller.getGameContext());
        StubState commandState = new StubState(modelController, GameStatus.ROUND_OFFER);
        StubState automaticState = new StubState(modelController, GameStatus.ROUND_END);
        StubState waitingState = new StubState(modelController, GameStatus.ROUND_ACTIONS);
        commandState.skipNext = automaticState;
        automaticState.processNext = waitingState;
        waitingState.processNext = waitingState;
        setCurrentState(controller, commandState);

        controller.skipCurrentTurn(Color.BLUE);

        assertSame(waitingState, controller.getCurrentState());
        assertTrue(commandState.skipTurnCalled);
        assertTrue(automaticState.processCalled);
        assertTrue(waitingState.processCalled);
    }

    @Test
    void passStoresReturnedStateAndAdvancesUntilInputIsNeeded() throws Exception {
        GameFlowController controller = controllerWithPlayers("FlowPass");
        ModelController modelController = new ModelController(controller.getCurrentState().controller.getGameContext());
        StubState commandState = new StubState(modelController, GameStatus.ROUND_ACTIONS);
        StubState automaticState = new StubState(modelController, GameStatus.ROUND_END);
        StubState waitingState = new StubState(modelController, GameStatus.ROUND_ACTIONS);
        commandState.passNext = automaticState;
        automaticState.processNext = waitingState;
        waitingState.processNext = waitingState;
        setCurrentState(controller, commandState);

        controller.pass(Color.BLUE);

        assertSame(waitingState, controller.getCurrentState());
        assertTrue(commandState.passCalled);
        assertTrue(automaticState.processCalled);
        assertTrue(waitingState.processCalled);
    }

    @Test
    void baseGameStateRejectsUnsupportedCommandsAndProvidesDefaultViewData() throws Exception {
        ModelController modelController = new ModelController(controllerWithPlayers("BaseState").getCurrentState().controller.getGameContext());
        BaseState state = new BaseState(modelController);

        assertThrows(RuntimeException.class, () -> state.chooseOffer(Color.BLUE, 0));
        assertThrows(RuntimeException.class, () -> state.chooseCard(Color.BLUE, 0, true));
        assertThrows(RuntimeException.class, () -> state.pass(Color.BLUE));
        assertThrows(RuntimeException.class, () -> state.skipTurn(Color.BLUE));
        assertSame(state, state.process());
        assertTrue(state.getActivePlayer() == null);
        assertTrue(state.getRemainingTopChoices() == 0);
        assertTrue(state.getRemainingBottomChoices() == 0);
    }

    private static GameFlowController controllerWithPlayers(String gameName) throws Exception {
        GameSetup setup = new GameSetup();
        GameContext context = setup.createGame(gameName, 2, CardLoader.load());
        setup.createMapOfPlayers(context, List.of("Alice", "Bob"));
        return new GameFlowController(context);
    }

    private static void setCurrentState(GameFlowController controller, GameState state) throws Exception {
        Field currentState = GameFlowController.class.getDeclaredField("current_state");
        currentState.setAccessible(true);
        currentState.set(controller, state);
    }

    private static final class StubState extends GameState {
        private final GameStatus status;
        private GameState offerNext;
        private GameState cardNext;
        private GameState passNext;
        private GameState skipNext;
        private GameState processNext = this;
        private boolean chooseOfferCalled;
        private boolean chooseCardCalled;
        private boolean passCalled;
        private boolean skipTurnCalled;
        private boolean processCalled;

        private StubState(ModelController controller, GameStatus status) {
            super(controller);
            this.status = status;
        }

        @Override
        public GameState chooseOffer(Color player_color, int offer_tile) {
            chooseOfferCalled = true;
            return offerNext;
        }

        @Override
        public GameState chooseCard(Color player_color, int card, boolean is_top) {
            chooseCardCalled = true;
            return cardNext;
        }

        @Override
        public GameState pass(Color player_color) {
            passCalled = true;
            return passNext;
        }

        @Override
        public GameState skipTurn(Color player_color) {
            skipTurnCalled = true;
            return skipNext;
        }

        @Override
        public GameState process() {
            processCalled = true;
            return processNext;
        }

        @Override
        public GameStatus getStatus() {
            return status;
        }
    }

    private static final class BaseState extends GameState {
        private BaseState(ModelController controller) {
            super(controller);
        }

        @Override
        public GameStatus getStatus() {
            return GameStatus.LOADING;
        }
    }
}
