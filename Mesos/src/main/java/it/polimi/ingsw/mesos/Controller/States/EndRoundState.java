package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;


public class EndRoundState extends GameState {
    /**
     * Creates the end-round automatic state.
     *
     * @param controller model controller used to advance the round
     */
    public EndRoundState(ModelController controller) {
        super(controller);
    }

    /**
     * Advances the model to the next round and starts a new offer-selection
     * phase.
     *
     * @return the offer-selection state for the new round
     * @throws IllegalStateException if advancing requires moving past the last
     *                               era or restoring a rejoined player with no
     *                               free turn-order space
     */
    @Override
    public GameState process() {
        controller.endRound();
        controller.restoreRejoinedPlayers();

        return new ChooseOfferState(controller);
    }

    /**
     * Reports that the state machine is resolving end-of-round work.
     *
     * @return round-end status
     */
    @Override
    public GameStatus getStatus() {
        return GameStatus.ROUND_END;
    }
}
