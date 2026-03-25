package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Tribe;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EndGameState extends GameState {
    public EndGameState(ModelController controller) {
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
}
