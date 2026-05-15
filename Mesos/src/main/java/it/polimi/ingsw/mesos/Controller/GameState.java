package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;

abstract public class GameState {
    protected final ModelController controller;

    /**
     * Creates a state backed by the shared model controller.
     *
     * @param controller model controller used by this state
     */
    public GameState(ModelController controller) {
        this.controller = controller;
    }

    /**
     * Handles a card-choice command for this state.
     *
     * @param player_color color of the player issuing the command
     * @param card selected card index
     * @param is_top true when selecting from the top market row
     * @return the next state after handling the command
     * @throws RuntimeException by default because this base state does not
     *                          accept card choices
     */
    public GameState chooseCard(Color player_color, int card, boolean is_top) throws RuntimeException {
        throw new RuntimeException("Unable to choose card: wrong game state");
    }

    /**
     * Handles an offer-choice command for this state.
     *
     * @param player_color color of the player issuing the command
     * @param offer_tile selected offer-tile index
     * @return the next state after handling the command
     * @throws RuntimeException by default because this base state does not
     *                          accept offer choices
     */
    public GameState chooseOffer(Color player_color, int offer_tile) throws RuntimeException {
        throw new RuntimeException("Unable to choose offer tile: wrong game state");
    }

    /**
     * Handles a skip-turn command for this state.
     *
     * @param player_color color of the player whose turn is skipped
     * @return the next state after handling the command
     * @throws RuntimeException by default because this base state does not
     *                          accept skipping
     */
    public GameState skipTurn(Color player_color) throws RuntimeException {
        throw new RuntimeException("Unable to skip turn: wrong game state");
    }

    /**
     * Performs automatic work for this state when no immediate player input is
     * required.
     *
     * @return this state if input is needed, another state if the flow advances
     *         automatically, or null when the game finishes
     */
    public GameState process() { return this; }

    public abstract GameStatus getStatus();

    /**
     * Returns the player expected to act in this state.
     *
     * @return active player color, or null when this state has no active player
     */
    public Color getActivePlayer() {
        return null;
    }

    /**
     * Returns how many top-row choices remain for the active action state.
     *
     * @return remaining top-row choices, or zero for states without such
     *         choices
     */
    public int getRemainingTopChoices() {
        return 0;
    }

    /**
     * Returns how many bottom-row choices remain for the active action state.
     *
     * @return remaining bottom-row choices, or zero for states without such
     *         choices
     */
    public int getRemainingBottomChoices() {
        return 0;
    }
}
