package it.polimi.ingsw.mesos.model.cards.events;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.model.cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.model.cards.characters.HunterCard;
import it.polimi.ingsw.mesos.model.cards.EventCard;
import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.Tribe;
import it.polimi.ingsw.mesos.model.Result;

/**
 * Event card that awards food and prestige according to each tribe's hunters.
 */
public class HuntEvent extends EventCard {
    private final int prestige_per_hunter;
    private final int food_per_hunter;

    /**
     * Creates a hunt event from JSON data.
     *
     * @param era era this event belongs to
     * @param prestige_per_hunter prestige gained for each hunter
     * @param food_per_hunter food gained for each hunter
     * @throws IllegalArgumentException if prestige per hunter is outside 1 to
     *                                  3, or food per hunter is not 1
     */
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
    /**
     * Returns the food-to-prestige ratio for this hunt event.
     *
     * @return food gained per prestige point awarded
     */
    public float getMultiplier() {
        return (float) food_per_hunter / prestige_per_hunter;
    }

    /**
     * Returns the prestige gained for each hunter.
     *
     * @return prestige per hunter
     */
    public int getPrestigePerHunter() {
        return prestige_per_hunter;
    }

    /**
     * Returns the food gained for each hunter.
     *
     * @return food per hunter
     */
    public int getFoodPerHunter() {
        return food_per_hunter;
    }

    /**
     * Awards each player food and prestige according to the number of hunters
     * in their tribe.
     *
     * @param players_tribes tribes keyed by player color
     * @return hunt rewards keyed by player color, or an empty map if no tribes
     *         are provided
     * @throws NullPointerException if the input map or one of its tribe values
     *                              is null
     */
    @Override
    public Map<Color, Result> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, Result> player_outcomes = new HashMap<>();

        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            int hunters = tribe.getValue().getNumCharacterOfType(HunterCard.class);
            Result vals = new Result(hunters * food_per_hunter, hunters * prestige_per_hunter, 0);
            player_outcomes.put(tribe.getKey(), vals);
        }

        return player_outcomes;
    }

    /**
     * Returns the building trigger used by hunt-event buildings.
     *
     * @return hunt event trigger
     */
    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_HUNT;
    }

    /**
     * Delegates this hunt event's description to the supplied visitor.
     *
     * @param visitor visitor that describes hunt events
     * @return visitor-provided event description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeHuntEvent(this);
    }

    /**
     * Returns the display name of this event type.
     *
     * @return {@code "Hunt Event"}
     */
    @Override
    public String getReadableName() {
        return "Hunt Event";
    }
}
