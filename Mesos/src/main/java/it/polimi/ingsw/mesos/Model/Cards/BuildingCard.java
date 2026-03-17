package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Card;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Player;


@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildingCard extends Card {
    private final int food_cost;
    private final int prestige_points;
    private Player owner;
    private final BuildingTrigger trigger;

    @JsonCreator
    public BuildingCard(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points,
            @JsonProperty("trigger")        BuildingTrigger trigger
    ) {
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