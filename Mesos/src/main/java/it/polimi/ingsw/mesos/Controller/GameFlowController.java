package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Controller.States.ChooseOfferState;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.GameContext;

/**
 * Coordinates the game state machine, applying player commands and advancing
 * automatic phases until new input is required.
 */
public class GameFlowController {
    private GameState current_state;

    /**
     * Creates the game flow controller, starts the first round, and advances to
     * the first state that requires player input.
     *
     * @param context initialized game context
     * @throws IllegalArgumentException if the context is null
     */
    public GameFlowController(GameContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        context.newRound();
        ModelController model_controller = new ModelController(context);
        this.current_state = new ChooseOfferState(model_controller);
        advance();
    }

    /**
     * Processes automatic state transitions until the game reaches a state that
     * requires player input or reaches the finished state.
     */
    private void advance() {
        while (current_state != null) {
            GameState next_state = current_state.process();
            if (next_state == current_state) {
                return;
            }
            current_state = next_state;
        }
    }

    /**
     * Returns the state currently waiting for input or representing the current
     * automatic phase.
     *
     * @return current state, or null after the game has finished
     */
    public GameState getCurrentState() {
        return current_state;
    }

    /**
     * Checks whether the state machine has reached its terminal state.
     *
     * @return true when no current state remains
     */
    public boolean isGameFinished() {
        return current_state == null;
    }

    /**
     * Applies a card-choice command to the current state and resolves any
     * automatic transitions that follow it.
     *
     * @param player_color color of the player issuing the command
     * @param card selected card index
     * @param is_top true when the card is selected from the top row
     * @throws IllegalStateException if the game has already finished
     * @throws RuntimeException if the current state does not accept card
     *                          choices or rejects the command
     */
    public void chooseCard(Color player_color, int card, boolean is_top) {
        ensureGameIsRunning();
        current_state = current_state.chooseCard(player_color, card, is_top);
        advance();
    }

    /**
     * Applies an offer-choice command to the current state and resolves any
     * automatic transitions that follow it.
     *
     * @param player_color color of the player issuing the command
     * @param offer_tile selected offer-tile index
     * @throws IllegalStateException if the game has already finished
     * @throws RuntimeException if the current state does not accept offer
     *                          choices or rejects the command
     */
    public void chooseOffer(Color player_color, int offer_tile) {
        ensureGameIsRunning();
        current_state = current_state.chooseOffer(player_color, offer_tile);
        advance();
    }

    /**
     * Applies a pass command to the current state and resolves any automatic
     * transitions that follow it.
     *
     * @param player_color color of the player issuing the command
     * @throws IllegalStateException if the game has already finished
     * @throws RuntimeException if the current state does not accept passing or
     *                          rejects the command
     */
    public void pass(Color player_color) {
        ensureGameIsRunning();
        current_state = current_state.pass(player_color);
        advance();
    }

    /**
     * Applies a skip-turn command to the current state and resolves any
     * automatic transitions that follow it.
     *
     * @param player_color color of the player whose turn is skipped
     * @throws IllegalStateException if the game has already finished
     * @throws RuntimeException if the current state does not accept skipping or
     *                          rejects the command
     */
    public void skipCurrentTurn(Color player_color) {
        ensureGameIsRunning();
        current_state = current_state.skipTurn(player_color);
        advance();
    }

    /**
     * Guards public commands from being executed after the game has ended.
     *
     * @throws IllegalStateException if the game has already finished
     */
    private void ensureGameIsRunning() {
        if (current_state == null) {
            throw new IllegalStateException("game has already finished");
        }
    }
}
