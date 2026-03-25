package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndRound;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;

import java.util.List;

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

    @Override
    public GameState process() {
        List<BuildingEndRound> buildings = controller.getBuildingsWithTrigger(BuildingTrigger.ON_END_ROUND)
                        .stream().map(building -> ((BuildingEndRound) building)).toList();
        buildings.forEach(building -> building.effect(controller.getGameTable()));

        controller.endRound();

        round += 1;

        return new ChooseOfferState(controller);
    }
}