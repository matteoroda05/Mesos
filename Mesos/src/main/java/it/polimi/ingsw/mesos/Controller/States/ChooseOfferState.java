package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Enums.Color;

public class ChooseOfferState extends GameState {
    private Color p;

    public ChooseOfferState(ModelController controller) {
        super(controller);
        p = controller.getNextPlayerTurn();
    }

    /**
     * Rejects card choices during the offer-selection phase.
     *
     * @param player_color color of the player issuing the command
     * @param card selected card index
     * @param is_top true when selecting from the top market row
     * @return this state, because card choices are not valid here
     */
    @Override
    public GameState chooseCard(Color player_color, int card, boolean is_top) {
        // TODO: set error in model to warn clients
        return this;
    }

    /**
     * Handles the active player's offer selection and advances to the action
     * phase once every player has chosen an offer tile.
     *
     * @param player_color color of the player choosing an offer
     * @param offer_tile selected offer-tile index
     * @return this state if more offers must be chosen, otherwise the first
     *         action state
     */
    @Override
    public GameState chooseOffer(Color player_color, int offer_tile) {
        if (p != player_color) {
            // TODO: set error in model to warn clients
            return this;
        }

        try {
            controller.playerChoseOffer(player_color, offer_tile);
        } catch (Exception _) {
            // TODO: set error in model to warn clients
            return this;
        }

        p = controller.getNextPlayerTurn();
        if (p == null) {
            return new ActionState(controller);
        }

        p = controller.getNextPlayerTurn();
        return this;
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.ROUND_OFFER;
    }

    @Override
    public Color getActivePlayer() {
        return p;
    }
}
