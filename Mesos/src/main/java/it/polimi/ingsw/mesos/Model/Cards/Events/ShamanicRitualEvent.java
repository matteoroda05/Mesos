package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.EventPhase;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Result;

/**
 * Event card that compares tribe shaman stars to award winners and penalize
 * players tied for fewest stars.
 */
public class ShamanicRitualEvent extends EventCard {
    private final int winner_bonus;
    private final int loser_penalty;

    /**
     * Creates a shamanic ritual event from JSON data.
     *
     * @param era era this event belongs to
     * @param winner_bonus prestige gained by players tied for most stars
     * @param loser_penalty prestige lost by players tied for fewest stars
     * @throws IllegalArgumentException if the winner bonus is outside 5 to 15
     *                                  or the loser penalty is outside -7 to -3
     */
    @JsonCreator
    public ShamanicRitualEvent(
            @JsonProperty("era")          Era era,
            @JsonProperty("winnerBonus")  int winner_bonus,
            @JsonProperty("loserPenalty") int loser_penalty
    ) {
        super(era);

        if (winner_bonus < 5 || winner_bonus > 15) throw new IllegalArgumentException("Winner bonus must be positive");
        if (loser_penalty < -7 || loser_penalty > -3) throw new IllegalArgumentException("Loser penalty must be negative");

        this.winner_bonus = winner_bonus;
        this.loser_penalty = loser_penalty;
    }

    /**
     * Resolves the ritual by comparing shaman stars and assigning winner
     * bonuses and loser penalties.
     * Ties are inclusive: every player tied for most stars gains the winner
     * bonus, every player tied for fewest stars suffers the loser penalty, and
     * players receive both effects when all star totals are equal.
     *
     * @param players_tribes tribes keyed by player color
     * @return ritual outcome keyed by player color, or an empty map if no
     *         tribes are provided
     * @throws NullPointerException if the input map or one of its tribe values
     *                              is null
     */
    @Override
    public Map<Color, Result> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, Integer> player_stars = new HashMap<>();
        Map<Color, Result> player_outcomes = new HashMap<>();

        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            player_stars.put(tribe.getKey(), tribe.getValue().countShamanStars());
        }

        Map<Color, List<BuildingCard>> buildings = players_tribes.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getBuildingsWithTrigger(asTrigger()).stream()
                        .filter(building ->
                                ((BuildingShamanicRitual) building).getPhase() == EventPhase.PRE_EVENT).toList()
        ));

        buildings.forEach((key, list) -> list.forEach(building ->
                player_stars.replace(key, player_stars.get(key) + ((BuildingShamanicRitual) building).getStarBonus())));

        // Thresholds must reflect the pre-event star bonuses applied above.
        int lowest_stars = Integer.MAX_VALUE;
        int highest_stars = Integer.MIN_VALUE;
        for (int stars : player_stars.values()) {
            lowest_stars = Math.min(lowest_stars, stars);
            highest_stars = Math.max(highest_stars, stars);
        }

        for (Map.Entry<Color, Integer> stars : player_stars.entrySet()) {
            int prestige_diff = 0;
            if (stars.getValue() == highest_stars) {
                prestige_diff += winner_bonus;
            }
            // if all have same they all win and loose at the same time
            if (stars.getValue() == lowest_stars) {
                prestige_diff += loser_penalty;
            }
            player_outcomes.put(stars.getKey(), new Result(0, prestige_diff, 0));
        }

        return player_outcomes;
    }

    /**
     * Returns the prestige bonus for players tied for most stars.
     *
     * @return winner bonus
     */
    public int getWinnerBonus() {
        return winner_bonus;
    }

    /**
     * Returns the prestige penalty for players tied for fewest stars.
     *
     * @return loser penalty
     */
    public int getLoserPenalty() {
        return loser_penalty;
    }

    /**
     * Returns the building trigger used by shamanic-ritual-event buildings.
     *
     * @return shamanic ritual event trigger
     */
    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL;
    }

    /**
     * Delegates this shamanic ritual's description to the supplied visitor.
     *
     * @param visitor visitor that describes shamanic-ritual events
     * @return visitor-provided event description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeShamanicRitualEvent(this);
    }

    /**
     * Returns the display name of this event type.
     *
     * @return {@code "Shamanic Ritual Event"}
     */
    @Override
    public String getReadableName() {
        return "Shamanic Ritual Event";
    }
}
