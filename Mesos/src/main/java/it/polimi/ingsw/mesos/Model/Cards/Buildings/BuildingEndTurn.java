package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

/**
 * Building card that grants food when its owner ends a turn on a positive
 * turn-order space.
 */
public class BuildingEndTurn extends BuildingCard {
    private final int food_bonus;

    /**
     * Creates an end-turn building from JSON data.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param food_bonus food granted by the effect
     * @throws IllegalArgumentException if the base card data is invalid or the
     *                                  food bonus is not 1
     */
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

    /**
     * Grants the owner food at end turn only if their totem returned to a
     * positive-bonus turn-order space.
     *
     * @param order turn order tile to inspect
     * @param visitor controller used to grant food
     * @throws NullPointerException if the owner, order, or visitor is null
     * @throws IllegalStateException if the owner's totem is not on the
     *                               turn-order tile
     */
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

    /**
     * Returns the food granted by this building.
     *
     * @return food bonus
     */
    public int getFoodBonus() {
        return food_bonus;
    }

    /**
     * Delegates this end-turn building's description to the supplied visitor.
     *
     * @param visitor visitor that describes end-turn buildings
     * @return visitor-provided building description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeBuildingEndTurn(this);
    }

    /**
     * Returns the display name of this building type.
     *
     * @return {@code "Building End Turn"}
     */
    @Override
    public String getReadableName() {
        return "Building End Turn";
    }
}
