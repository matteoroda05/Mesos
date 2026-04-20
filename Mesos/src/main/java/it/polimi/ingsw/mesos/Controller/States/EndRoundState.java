package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Enums.Color;


public class EndRoundState extends GameState {
    public EndRoundState(ModelController controller) {
        super(controller);
    }

    @Override
    public GameState chooseCard(Color player_color, int card, boolean is_top) {
        // TODO: set error in model to warn clients
        return this;
    }

    @Override
    public GameState chooseOffer(Color player_color, int offer_tile) {
        // TODO: set error in model to warn clients
        return this;
    }

    /**
     * Advances the model to the next round and starts a new offer-selection
     * phase.
     *
     * @return the offer-selection state for the new round
     */
    @Override
    public GameState process() {
        controller.endRound();

        return new ChooseOfferState(controller);
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.ROUND_END;
    }
}
