package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Controller.States.ActionState;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameTable;

public class BuildingAfterEveryone extends BuildingCard {
    private boolean to_apply = true;

    @JsonCreator
    public BuildingAfterEveryone(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_AFTER_EVERYONE);
    }

    // TODO: add visitor that can allow extra card pick
    public ActionState effect(GameTable table, ModelController controller) {
        to_apply = false;  // effect applied this round, waits reset for new round

        final boolean cards_available = !table.getTopRow()
                .getTakeableCards(owner.getFood(), owner.getTribe().buildersDiscount()).isEmpty();
        if (cards_available) {
            return new ActionState(controller, owner.getColor(), 1, true);
        }
        return null;
    }

    public boolean isActive() {
        return to_apply;
    }

    public void reset() {
        to_apply = true;
    }
}
