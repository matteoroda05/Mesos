package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndTurn;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

import java.util.List;
import java.util.Optional;

public class ActionState extends GameState {
    private int pick_top_row;
    private int pick_bottom_row;
    private Color p;
    private int food_gain;

    public ActionState(ModelController controller) {
        super(controller);
        OfferTile tile = controller.getNextPlayerOfferTile();
        int max_top_picks = controller.getAvailableCards(tile.getOccupant(), true);
        int max_bottom_picks = controller.getAvailableCards(tile.getOccupant(), false);
        loadTile(tile,  max_top_picks, max_bottom_picks);
    }

    @Override
    public GameState chooseCard(Color player_color, int card, boolean is_top) {
        if (p != player_color) {
            return this;
        }

        Optional<CharacterCard> chosen;
        if (is_top && pick_top_row > 0) {
            chosen = controller.playerChooseCard(player_color, card, true);
            pick_top_row--;
        } else if (!is_top && pick_bottom_row > 0) {
            chosen = controller.playerChooseCard(player_color, card, false);
            pick_bottom_row--;
        } else {
            // TODO: set error in model to warn clients
            return this;
        }

        if (chosen.isPresent()) {
            List<BuildingOnCard> buildings = controller.getBuildingsWithTrigger(BuildingTrigger.ON_ACQUIRE_CHARACTER)
                    .stream().filter(building -> building.getOwner().getColor() == this.p).map(building -> ((BuildingOnCard) building)).toList();
            buildings.forEach(building -> building.effect(chosen.get(), controller));
        }

        if (hasFinished()) {
            return endTurn();
        }

        return this;
    }

    @Override
    public GameState chooseOffer(Color player_color, int offer_tile) {
        // TODO: set error in model to warn clients
        return this;
    }

    @Override
    public GameState process() {
        if (food_gain != 0) {
            controller.playerChangeFood(food_gain, p);
            food_gain = 0;
            return endTurn();
        }
        return this;
    }

    private boolean hasFinished() {
        return pick_bottom_row <= 0 && pick_top_row <= 0 && food_gain == 0;
    }

    private GameState endTurn() {
        controller.playerEndTurn();

        List<BuildingEndTurn> buildings = controller.getBuildingsWithTrigger(BuildingTrigger.ON_END_TURN)
                .stream().map(building -> ((BuildingEndTurn) building)).toList();
        TurnOrderTile tile = controller.getTurnOrderTile();
        buildings.forEach(building -> building.effect(tile, controller));

        OfferTile next = controller.getNextPlayerOfferTile();
        if (next == null) {
            return new EventState(controller);
        }

        int max_top_picks = controller.getAvailableCards(next.getOccupant(), true);
        int max_bottom_picks = controller.getAvailableCards(next.getOccupant(), false);
        loadTile(next, max_top_picks, max_bottom_picks);
        return this;
    }

    private void loadTile(OfferTile tile, int max_top_picks, int max_bottom_picks) {
        p = tile.getOccupant();
        pick_top_row = Math.min(tile.getTopPicks(), max_top_picks);
        pick_bottom_row = Math.min(tile.getBottomPicks(), max_bottom_picks);
        food_gain = tile.getFoodGain();
    }
}
