package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;

public final class SustenanceEvent extends EventCard {
    private final int food_per_character;
    private final int points_penalty;

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

    public int getPointPenalty() {
        return points_penalty;
    }

    @Override
    public Map<Color, int[]> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, int[]> food_requirements = new HashMap<>();

        int[] vals = new int[2];
        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            int food_needed = tribe.getValue().getNumCharacters() * food_per_character;
            int discount = tribe.getValue().gatherersDiscount();
            vals[1] = Math.max(0, food_needed - discount);
            food_requirements.put(tribe.getKey(), vals);
        }

        return food_requirements;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_SUSTENANCE;
    }
}