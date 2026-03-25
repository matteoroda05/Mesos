package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.Enums.Color;

public class ChooseOfferState extends GameState {
    private Color p;

    public ChooseOfferState(ModelController controller) {
        super(controller);
        p = controller.getNextPlayerTurn();
    }

    @Override
    public GameState chooseCard(Color player_color, int card, boolean is_top) {
        // TODO: set error in model to warn clients
        return this;
    }

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
}
