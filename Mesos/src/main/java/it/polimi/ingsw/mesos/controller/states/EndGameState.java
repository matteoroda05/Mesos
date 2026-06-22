package it.polimi.ingsw.mesos.controller.states;

import it.polimi.ingsw.mesos.controller.enums.GameStatus;
import it.polimi.ingsw.mesos.controller.GameState;
import it.polimi.ingsw.mesos.controller.ModelController;
import it.polimi.ingsw.mesos.dto.event.AppliedDelta;
import it.polimi.ingsw.mesos.dto.event.EventMessageFormatter;
import it.polimi.ingsw.mesos.model.cards.buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.Player;
import it.polimi.ingsw.mesos.model.Tribe;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Performs automatic final scoring, including end-game building effects, and
 * terminates the game state machine.
 */
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

        StringBuilder body = new StringBuilder();
        points.keySet().stream().sorted().forEach(color ->
                body.append("Player ")
                        .append(EventMessageFormatter.playerLabel(
                                controller.getGameContext().getPlayer(color).getNickname(), color))
                        .append(" gained ").append(points.get(color)).append(" prestige points\n"));
        controller.logNotice("FINAL SCORING", body.toString().stripTrailing());

        controller.getBuildingsWithTrigger(BuildingTrigger.ON_END_GAME)
                .forEach(building -> {
                    BuildingEndGame endGameBuilding = (BuildingEndGame) building;
                    Player owner = endGameBuilding.getOwner();
                    if (owner == null) {
                        endGameBuilding.effect(controller);
                        return;
                    }
                    AppliedDelta delta = controller.captureDelta(
                            owner.getColor(),
                            () -> endGameBuilding.effect(controller));
                    controller.logBuildingActivation(endGameBuilding, delta, null);
                });

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
