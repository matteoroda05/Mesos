package it.polimi.ingsw.mesos.Model.Events;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
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
        if (food_per_hunter != 1) throw new IllegalArgumentException("Food per hunter must be positive.");
        
        this.prestige_per_hunter = prestige_per_hunter;
        this.food_per_hunter = food_per_hunter;
    }

    // Hunt event always gives 1 food for hunter, this method exists to support possible future extensions of the game
    public float getMultiplier() {
        return food_per_hunter / prestige_per_hunter;
    }

    @Override
    public List<Integer> calculateOutcome(List<Tribe> players_tribes) {
        List<Integer> player_outcomes = new ArrayList<>();

        for (Tribe tribe : players_tribes) {
            int hunters = tribe.getNumCharacterOfType(CharacterType.HUNTER);
            player_outcomes.add(hunters * prestige_per_hunter);
        }

        return player_outcomes;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_CAVE_PAINTINGS;
    }
}