package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Enums.Color;

/**
 * Handles the phase in which players choose offer tiles in turn-order
 * sequence before card actions begin.
 */
public class ChooseOfferState extends GameState {
    private Color p;

    /**
     * Creates the offer-choice state and selects the first player in turn
     * order as active.
     *
     * @param controller model controller used to apply choices
     */
    public ChooseOfferState(ModelController controller) {
        super(controller);
        p = controller.getNextPlayerTurn();
    }

    /**
     * Handles the active player's offer selection and advances to the action
     * phase once every player has chosen an offer tile.
     *
     * @param player_color color of the player choosing an offer
     * @param offer_tile selected offer-tile index
     * @return this state if more offers must be chosen, otherwise the first
     *         action state
     * @throws RuntimeException if the command is issued by a non-active player
     * @throws IllegalArgumentException if the selected offer tile is invalid
     * @throws IllegalStateException if the selected tile is unavailable or turn
     *                               order is inconsistent
     */
    @Override
    public GameState chooseOffer(Color player_color, int offer_tile) {
        if (p != player_color) {
            throw new RuntimeException("Wrong turn: it's player " + p + "'s turn, not "  + player_color + "'s turn");
        }

        controller.playerChoseOffer(player_color, offer_tile);

        p = controller.getNextPlayerTurn();
        if (p == null) {
            return new ActionState(controller);
        }
        return this;
    }

    /**
     * Skips the active player's offer choice and advances to the next player or
     * to the action phase when no offer choices remain.
     *
     * @param player_color color of the player whose offer choice is skipped
     * @return this state if more offers must be chosen, otherwise the first
     *         action state
     * @throws RuntimeException if the command is issued by a non-active player
     * @throws IllegalStateException if turn order is inconsistent
     */
    @Override
    public GameState skipTurn(Color player_color) {
        if (p != player_color) {
            throw new RuntimeException("Wrong turn: it's player " + p + "'s turn, not "  + player_color + "'s turn");
        }

        controller.playerSkipOffer(player_color);
        p = controller.getNextPlayerTurn();
        if (p == null) {
            return new ActionState(controller);
        }
        return this;
    }

    /**
     * Reports that the game is waiting for offer choices.
     *
     * @return offer-round status
     */
    @Override
    public GameStatus getStatus() {
        return GameStatus.ROUND_OFFER;
    }

    /**
     * Returns the player currently expected to choose an offer.
     *
     * @return active player color, or null if no offer choices remain
     */
    @Override
    public Color getActivePlayer() {
        return p;
    }
}
