package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;


public class BuildingCard extends Card{
    private final int food_cost;
    private final int prestige_points;
    private Player owner;
    private final BuildingTrigger trigger;

    public BuildingCard(Era era, int min_players, int food_cost, int prestige_points, BuildingTrigger trigger) {
        super(era,min_players);
        this.food_cost = food_cost;
        this.prestige_points = prestige_points;
        this.trigger = trigger;
    }

    public int getFoodCost() {
        return food_cost;
    }

    public int getPrestigePoints() {
        return prestige_points;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public BuildingTrigger getTrigger() {
        return trigger;
    }
}