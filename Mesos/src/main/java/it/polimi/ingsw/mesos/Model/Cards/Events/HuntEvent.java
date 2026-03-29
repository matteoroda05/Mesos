package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;

public class HuntEvent extends EventCard {
    private final int prestige_per_hunter;
    private final int food_per_hunter;

    @JsonCreator
    public HuntEvent(
            @JsonProperty("era")               Era era,
            @JsonProperty("prestigePerHunter") int prestige_per_hunter,
            @JsonProperty("foodPerHunter")     int food_per_hunter
    ) {
        super(era);

        if (prestige_per_hunter < 1 || prestige_per_hunter > 3) throw new IllegalArgumentException("Prestige per hunter must be positive.");
        if (food_per_hunter != 1) throw new IllegalArgumentException("Food per hunter must equal to 1.");
        
        this.prestige_per_hunter = prestige_per_hunter;
        this.food_per_hunter = food_per_hunter;
    }

    // Hunt event always gives 1 food por hunter, this method exists to support possible future extensions of the game
    public float getMultiplier() {
        return (float) food_per_hunter / prestige_per_hunter;
    }

    @Override
    public Map<Color, int[]> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, int[]> player_outcomes = new HashMap<>();

        int[] vals =  new int[2]; //new int[2] alr initializes to 0
        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            int hunters = tribe.getValue().getNumCharacterOfType(HunterCard.class);
            vals[0] = hunters * prestige_per_hunter;
            vals[1] = hunters * food_per_hunter;
            player_outcomes.put(tribe.getKey(), vals);
        }

        return player_outcomes;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_HUNT;
    }
}