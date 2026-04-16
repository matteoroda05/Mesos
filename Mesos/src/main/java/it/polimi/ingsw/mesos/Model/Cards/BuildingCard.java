package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.*;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Player;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BuildingOnCard.class,         name = "BUILDING_ON_CARD"),
        @JsonSubTypes.Type(value = BuildingEndTurn.class,        name = "BUILDING_END_TURN"),
        @JsonSubTypes.Type(value = BuildingAfterEveryone.class,       name = "BUILDING_END_ROUND"),
        @JsonSubTypes.Type(value = BuildingEndGame.class,        name = "BUILDING_END_GAME"),
        @JsonSubTypes.Type(value = BuildingBonusEvent.class,     name = "BUILDING_BONUS_EVENT"),
        @JsonSubTypes.Type(value = BuildingShamanicRitual.class, name = "BUILDING_SHAMANIC_RITUAL")
})
public abstract class BuildingCard extends Card {
    private final int food_cost;
    private final int prestige_points;
    protected Player owner;
    private final BuildingTrigger trigger;

    @JsonCreator
    public BuildingCard(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points,
            @JsonProperty("trigger")        BuildingTrigger trigger
    ) {
        super(era, min_players);
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

    /**
     * Assigns this building to a player and runs any ownership-dependent setup
     * required by subclasses.
     *
     * @param owner player who acquired the building
     */
    public void setOwner(Player owner) {
        this.owner = owner;
        onAssignment();
    }

    /**
     * Hook for subclasses that must initialize effect state after the owner is
     * known.
     */
    protected void onAssignment() {}

    public BuildingTrigger getTrigger() {
        return trigger;
    }
}
