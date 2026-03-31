package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.EventPhase;

import java.util.Map;

public class BuildingShamanicRitual extends BuildingCard {
    private final int star_bonus;
    private final int win_multiplier;
    private final EventPhase phase;

    @JsonCreator
    public BuildingShamanicRitual(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points,
            @JsonProperty("starBonus")      int star_bonus,
            @JsonProperty("winMultiplier")  int win_multiplier
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL);

        if (star_bonus * win_multiplier != 0) throw new IllegalArgumentException("Building cannot have more than one effect.");
        if (star_bonus != 0 && star_bonus != 3) throw new IllegalArgumentException("Invalid star bonus: " + star_bonus);
        if (win_multiplier != 0 && win_multiplier != 2) throw new IllegalArgumentException("Invalid win multiplier: " + win_multiplier);

        this.star_bonus = star_bonus;
        this.win_multiplier = win_multiplier;
        phase = this.star_bonus != 0 ? EventPhase.PRE_EVENT : EventPhase.POST_EVENT;
    }

    public void effect(Map<Color, int[]> points) {
        if (win_multiplier != 0) {
            // multiply points if it's the only player that won
            int player_points = points.get(owner.getColor())[0];

            if (player_points <= 0) return; // player didn't win

            boolean single_winner = false;
            for (int[] vals : points.values()) {
                int point =  vals[0];
                if (point > 0) {
                    if (single_winner) {
                        // more than one winner -> no bonus
                        return;
                    }

                    single_winner = true;
                }
            }

            // there's only one winner and it's the player
            int[] vals = points.get(owner.getColor());
            vals[0] = player_points * win_multiplier;
            points.replace(owner.getColor(), vals);
            return;
        }

        // last effect: no pp lost if last
        int[] vals = points.get(owner.getColor());
        if (vals[0] >= 0) return; // player didn't loose
        vals[0] = 0;

        points.replace(owner.getColor(), vals);
    }

    public EventPhase getPhase() {
        return phase;
    }

    public int getStarBonus() { return star_bonus; }

    public int getWinMultiplier() {
        return win_multiplier;
    }
}
