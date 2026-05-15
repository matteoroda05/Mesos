package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Tribe;

import java.util.Map;
import java.util.stream.Collectors;

public class EndGameState extends GameState {
    /**
     * Creates the end-game automatic state.
     *
     * @param controller model controller used to apply final scoring
     */
    public EndGameState(ModelController controller) {
        super(controller);
    }

    /**
     * Computes final scoring, applies end-game building effects, and terminates
     * the state machine.
     *
     * @return null to mark the game as finished
     * @throws NullPointerException if an end-game building is missing an owner
     */
    @Override
    public GameState process() {
        Map<Color, Tribe> tribes = controller.getTribes();
        Map<Color, Integer> points = tribes.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().tribeEndGamePrestige()
        ));
        points.forEach((key, point) -> controller.playerChangePrestige(point, key));

        controller.getBuildingsWithTrigger(BuildingTrigger.ON_END_GAME)
                .forEach(building -> ((BuildingEndGame) building).effect(controller));

        return null; // game finished, declare winner
    }

    /**
     * Reports that final scoring is being resolved.
     *
     * @return end-game status
     */
    @Override
    public GameStatus getStatus() {
        return GameStatus.END_GAME;
    }
}
