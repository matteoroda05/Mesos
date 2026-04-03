package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

public class BuildingEndTurn extends BuildingCard {
    private final int food_bonus;

    @JsonCreator
    public BuildingEndTurn(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points,
            @JsonProperty("foodBonus")      int food_bonus
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_END_TURN);

        if (food_bonus != 1) throw new IllegalArgumentException("Invalid food bonus: " + food_bonus);

        this.food_bonus = food_bonus;
    }

    public void effect(TurnOrderTile order, ModelController visitor) {
        List<TurnOrderSpace> spaces = order.spaces();

        boolean found = false;
        for (TurnOrderSpace space : spaces) {
            if (space.getOccupant() == owner.getColor()) {
                found = true;
                if (space.getBonus() > 0) {
                    visitor.playerAddFood(food_bonus, owner.getColor());
                }
                break;
            }
        }

        if (!found) throw new IllegalStateException("Called effect before player returns to order tile!");
    }
}
