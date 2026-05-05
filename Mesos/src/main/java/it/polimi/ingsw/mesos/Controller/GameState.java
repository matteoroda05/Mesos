package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;

abstract public class GameState {
    protected final ModelController controller;

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
     */
    public GameState chooseOffer(Color player_color, int offer_tile) throws RuntimeException {
        throw new RuntimeException("Unable to choose offer tile: wrong game state");
    }

    public GameState skipTurn() throws RuntimeException {
        throw new RuntimeException("Unable to skip turn: wrong game state");
    }

    /**
     * Performs automatic work for this state when no immediate player input is
     * required.
     *
     * @return this state if input is needed, another state if the flow advances
     */
    public GameState process() { return this; }

    /**
     * Reports the public status represented by this state.
     *
     * @return current game status
     */
    public abstract GameStatus getStatus();

    public Color getActivePlayer() {
        return null;
    }

    public int getRemainingTopChoices() {
        return 0;
    }

    public int getRemainingBottomChoices() {
        return 0;
    }
}
