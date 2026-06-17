package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Result;

public final class SustenanceEvent extends EventCard {
    private final int food_per_character;
    private final int points_penalty;

    /**
     * Creates a sustenance event from JSON data.
     *
     * @param era era this event belongs to
     * @param points_penalty prestige penalty per unpaid food
     * @param food_per_character food required for each character
     * @throws IllegalArgumentException if the penalty is outside -3 to -1, or
     *                                  food per character is not 1
     */
    @JsonCreator
    public SustenanceEvent(
            @JsonProperty("era")              Era era,
            @JsonProperty("pointsPenalty")    int points_penalty,
            @JsonProperty("foodPerCharacter") int food_per_character
    ) {
        super(era);

        if (points_penalty < -3 || points_penalty > -1) throw new IllegalArgumentException("Points penalty must be negative.");
        if (food_per_character != 1) throw new IllegalArgumentException("Food per character must be positive.");

        this.points_penalty = points_penalty;
        this.food_per_character = food_per_character;
    }

    /**
     * Calculates each player's food requirement from tribe size, reduced by any
     * gatherer discount, and records the penalty conversion for unpaid food.
     * Food in the result is never positive; a sufficient gatherer discount
     * produces zero required food.
     *
     * @param players_tribes tribes keyed by player color
     * @return sustenance costs keyed by player color, or an empty map if no
     *         tribes are provided
     * @throws NullPointerException if the input map or one of its tribe values
     *                              is null
     */
    @Override
    public Map<Color, Result> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, Result> food_requirements = new HashMap<>();

        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            int food_needed = tribe.getValue().getNumCharacters() * food_per_character;
            int discount = tribe.getValue().gatherersDiscount();
            // Negative because prestige "conversion" has to be positive
            Result val = new Result(Math.min(0, - food_needed + discount), 0, -points_penalty);
            food_requirements.put(tribe.getKey(), val);
        }

        return food_requirements;
    }

    /**
     * Returns the building trigger used by sustenance-event buildings.
     *
     * @return sustenance event trigger
     */
    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_SUSTENANCE;
    }

    // Utils for view

    /**
     * Returns the food required for each character.
     *
     * @return food per character
     */
    public int getFoodPerCharacter() {
        return food_per_character;
    }

    /**
     * Returns the prestige penalty applied per unpaid food.
     *
     * @return negative point penalty value
     */
    public int getPointPenalty() {
        return points_penalty;
    }

    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeSustenanceEvent(this);
    }

    @Override
    public String getReadableName() {
        return "Sustenance Event";
    }
}
