package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.*;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.*;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Network.CardSummary;

/**
 * Abstract base for acquirable building cards, retaining their cost, prestige,
 * owner, and activation trigger.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BuildingOnCard.class,         name = "BUILDING_ON_CARD"),
        @JsonSubTypes.Type(value = BuildingEndTurn.class,        name = "BUILDING_END_TURN"),
        @JsonSubTypes.Type(value = BuildingAfterEveryone.class,  name = "BUILDING_END_ROUND"),
        @JsonSubTypes.Type(value = BuildingEndGame.class,        name = "BUILDING_END_GAME"),
        @JsonSubTypes.Type(value = BuildingBonusEvent.class,     name = "BUILDING_BONUS_EVENT"),
        @JsonSubTypes.Type(value = BuildingShamanicRitual.class, name = "BUILDING_SHAMANIC_RITUAL")
})
public abstract class BuildingCard extends Card {
    private final int food_cost;
    private final int prestige_points;
    protected Player owner;
    private final BuildingTrigger trigger;

    /**
     * Creates a building card with its printed cost, prestige, and trigger.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param trigger effect trigger for this building
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range accepted by {@link Card}
     */
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

    /**
     * Returns the printed food cost.
     *
     * @return food cost before discounts
     */
    public int getFoodCost() {
        return food_cost;
    }

    /**
     * Returns the printed prestige value.
     *
     * @return prestige points printed on the building
     */
    public int getPrestigePoints() {
        return prestige_points;
    }

    /**
     * Returns the player who owns this building.
     *
     * @return owner, or null until the building is assigned
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Assigns this building to a player and runs any ownership-dependent setup
     * required by subclasses.
     *
     * @param owner player who acquired the building
     * @throws NullPointerException if a subclass requires a non-null owner in
     *                              {@link #onAssignment()} and null is passed
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

    /**
     * Returns the trigger that activates this building.
     *
     * @return building trigger
     */
    public BuildingTrigger getTrigger() {
        return trigger;
    }

    /**
     * Creates the client-facing summary of this building card.
     *
     * @param selectionIndex index used to select this card in its source row
     * @return summary with the building's details, cost, prestige, and
     *         presentation ID
     */
    @Override
    public CardSummary getSummary(int selectionIndex) {
        return new CardSummary(
                selectionIndex,
                this.getReadableName(),
                this.getEra(),
                false,
                this.accept(CardDescriptionVisitorImpl.INSTANCE),
                this.getFoodCost(),
                this.getPrestigePoints(),
                this.getPresentationId()
        );
    }
}
