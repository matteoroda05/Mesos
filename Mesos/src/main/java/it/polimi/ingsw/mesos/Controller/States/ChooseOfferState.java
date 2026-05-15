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
            throw new RuntimeException("Wrong turn: it's player " + p + "'s turn, not "  + player_color + "'s turn (dumbass)" );
        }

        controller.playerChoseOffer(player_color, offer_tile);

        p = controller.getNextPlayerTurn();
        if (p == null) {
            return new ActionState(controller);
        }
        return this;
    }

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

    @Override
    public GameStatus getStatus() {
        return GameStatus.ROUND_OFFER;
    }

    @Override
    public Color getActivePlayer() {
        return p;
    }
}
