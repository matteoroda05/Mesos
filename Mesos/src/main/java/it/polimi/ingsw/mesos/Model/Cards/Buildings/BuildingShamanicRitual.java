package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.EventPhase;
import it.polimi.ingsw.mesos.Result;

import java.util.Map;

/**
 * Building card that modifies a shamanic ritual before or after its base
 * outcome, according to its configured effect.
 */
public class BuildingShamanicRitual extends BuildingCard {
    private final int star_bonus;
    private final int win_multiplier;
    private final EventPhase phase;

    /**
     * Creates a shamanic-ritual building from JSON data.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param star_bonus star bonus used before the ritual
     * @param win_multiplier multiplier used after a sole win
     * @throws IllegalArgumentException if both effects are configured, the star
     *                                  bonus is not 0 or 3, or the win
     *                                  multiplier is not 0 or 2
     */
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

    /**
     * Adjusts shamanic ritual results after the base event outcome: it either
     * doubles a sole winner's prestige or cancels the owner's prestige loss.
     * Pre-event (star-bonus) buildings are a no-op here, since their effect is
     * already accounted for while computing the ritual outcome.
     *
     * @param points mutable ritual outcomes keyed by player color
     * @throws NullPointerException if the owner, points map, or owner result is
     *                              null
     */
    public void effect(Map<Color, Result> points) {
        if (win_multiplier != 0) {
            // multiply points if it's the only player that won
            int player_points = points.get(owner.getColor()).prestige();

            if (player_points <= 0) return; // player didn't win

            boolean single_winner = false;
            for (Result vals : points.values()) {
                int point =  vals.prestige();
                if (point > 0) {
                    if (single_winner) {
                        // more than one winner -> no bonus
                        return;
                    }

                    single_winner = true;
                }
            }

            // there's only one winner and it's the player
            Result prev = points.get(owner.getColor());
            Result new_points = new Result(prev.food(), player_points * win_multiplier, prev.penalty());
            points.replace(owner.getColor(), new_points);
            return;
        }

        // Pre-event star-bonus buildings contribute through their star bonus (read directly
        // while computing the ritual outcome) and must not be treated as "no prestige loss"
        // buildings here.
        if (star_bonus != 0) {
            return;
        }

        // last effect: no pp lost if last
        Result prev = points.get(owner.getColor());
        if (prev.prestige() >= 0) return; // player didn't loose
        Result new_points = new Result(prev.food(), 0, prev.penalty());
        points.replace(owner.getColor(), new_points);
    }

    /**
     * Returns when this building effect applies relative to the event.
     *
     * @return pre-event for star bonuses, post-event for result modifiers
     */
    public EventPhase getPhase() {
        return phase;
    }

    /**
     * Returns the star bonus configured for this building.
     *
     * @return star bonus, or zero for post-event multiplier buildings
     */
    public int getStarBonus() { return star_bonus; }

    /**
     * Returns the winning-prestige multiplier configured for this building.
     *
     * @return win multiplier, or zero for pre-event star-bonus buildings
     */
    public int getWinMultiplier() {
        return win_multiplier;
    }

    /**
     * Delegates this ritual building's description to the supplied visitor.
     *
     * @param visitor visitor that describes ritual buildings
     * @return visitor-provided building description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeBuildingShamanicRitual(this);
    }

    /**
     * Returns the display name of this building type.
     *
     * @return {@code "Building Shamanic Ritual"}
     */
    @Override
    public String getReadableName() {
        return "Building Shamanic Ritual";
    }
}
