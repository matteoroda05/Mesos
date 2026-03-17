package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
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
    public List<Integer> calculateOutcome(List<Tribe> players_tribes) {
        List<Integer> food_requirements = new ArrayList<>();

        for (Tribe tribe : players_tribes) {
            int food_needed = tribe.getNumCharacters() * food_per_character;
            int discount = tribe.gatherersDiscount();
            food_requirements.add(Math.max(0, food_needed - discount));
        }

        return food_requirements;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_SUSTENANCE;
    }
}