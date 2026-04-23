package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Enums.Color;


public class EndRoundState extends GameState {
    public EndRoundState(ModelController controller) {
        super(controller);
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
