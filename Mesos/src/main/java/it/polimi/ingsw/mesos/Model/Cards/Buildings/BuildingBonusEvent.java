package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Enums.*;
import it.polimi.ingsw.mesos.Result;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BuildingBonusEvent extends BuildingCard {
    private final int food_bonus;
    private final int pp_bonus;
    private final Class<? extends CharacterCard> character_type;

    //Map to import from character_type from JSON file
    private static final Map<String, Class<? extends CharacterCard>> CHARACTER_MAP = Map.of(
            "HUNTER",   HunterCard.class,
            "SHAMAN",   ShamanCard.class,
            "ARTIST",   ArtistCard.class,
            "BUILDER",  BuilderCard.class,
            "INVENTOR", InventorCard.class,
            "GATHERER", GathererCard.class
    );

    /**
     * Creates an event-bonus building from JSON data, converting the character
     * type string into the matching character class.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param food_bonus food change per matching character
     * @param pp_bonus prestige bonus per matching character
     * @param character_type_str character type name from JSON
     * @param trigger event trigger this building reacts to
     */
    @JsonCreator
    private BuildingBonusEvent(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points,
            @JsonProperty("foodBonus")      int food_bonus,
            @JsonProperty("ppBonus")        int pp_bonus,
            @JsonProperty("characterType")  String character_type_str,
            @JsonProperty("trigger")        BuildingTrigger trigger
    ) {
        this(era, min_players, food_cost, prestige_points,
                food_bonus, pp_bonus,
                CHARACTER_MAP.get(character_type_str),
                trigger);
    }

    /**
     * Creates an event-bonus building.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param food_bonus food change per matching character
     * @param pp_bonus prestige bonus per matching character
     * @param character_type character class counted for the bonus
     * @param trigger event trigger this building reacts to
     * @throws IllegalArgumentException if the trigger is not one of the
     *                                  supported event triggers, both bonuses
     *                                  are zero, the food bonus is outside 0
     *                                  to 1, or the prestige bonus is outside
     *                                  0 to 1
     */
    public BuildingBonusEvent(
            Era era,
            int min_players,
            int food_cost,
            int prestige_points,
            int food_bonus,
            int pp_bonus,
            Class<? extends CharacterCard> character_type,
            BuildingTrigger trigger
    ) {
        super(era, min_players, food_cost, prestige_points, trigger);

        List<BuildingTrigger> valid = Arrays.asList(
                BuildingTrigger.ON_EVENT_SUSTENANCE,
                BuildingTrigger.ON_EVENT_HUNT,
                BuildingTrigger.ON_EVENT_CAVE_PAINTINGS);
        if (!valid.contains(trigger)) throw new IllegalArgumentException("Invalid trigger: " + trigger);
        if (food_bonus == 0 && pp_bonus == 0) throw new IllegalArgumentException("Building must have one effect.");
        // A food bonus represents food gained during Hunt/Cave Paintings or food discounted
        // during Sustenance; it is never a food loss, so negative values are rejected.
        if (food_bonus < 0 || food_bonus > 1) throw new IllegalArgumentException("Invalid food bonus: " + food_bonus);
        if (pp_bonus < 0 || pp_bonus > 1) throw new IllegalArgumentException("Invalid prestige points bonus: " + pp_bonus);

        this.food_bonus = food_bonus;
        this.pp_bonus = pp_bonus;
        this.character_type = character_type;
    }

    /**
     * Applies the event bonus produced by this building for each matching
     * character owned by the building owner.
     *
     * @param output base event results keyed by player color
     * @param visitor controller used to mutate the owner's resources and score
     * @throws NullPointerException if the owner, output entry for the owner,
     *                              or visitor is null
     * @throws IllegalArgumentException if the computed food change is negative
     *                                  and the controller rejects it, or if the
     *                                  character type is null
     */
    public void effect(Map<Color, Result> output, ModelController visitor) {
        final int multiplier = owner.getTribe().getNumCharacterOfType(character_type);
        final int prev_food = output.get(owner.getColor()).food();
        final int prev_prestige = output.get(owner.getColor()).prestige();
        int food_change = prev_food + multiplier * food_bonus;
        final int prestige_change = prev_prestige + multiplier * pp_bonus;
        final int prev_penalty = output.get(owner.getColor()).penalty();

        // During Sustenance the food value is the (non-positive) food owed and it carries a
        // non-zero penalty. A discount can at most cancel the food owed: it must never turn
        // into a food gain, which would also make the Result invalid (positive food with a
        // penalty). Clamp the food at zero for the Sustenance event.
        if (getTrigger() == BuildingTrigger.ON_EVENT_SUSTENANCE) {
            food_change = Math.min(0, food_change);
        }

        Result outcome = new Result(food_change, prestige_change, prev_penalty);
        output.replace(owner.getColor(), outcome);
    }

    /**
     * Returns the food change applied per matching character.
     *
     * @return food bonus, a non-negative value (0 or 1)
     */
    public int getFoodBonus() {
        return food_bonus;
    }

    /**
     * Returns the prestige bonus applied per matching character.
     *
     * @return prestige bonus
     */
    public int getPrestigeBonus() {
        return pp_bonus;
    }

    /**
     * Returns the character class counted by this building.
     *
     * @return character type
     */
    public Class<? extends CharacterCard> getCharacterType() {
        return character_type;
    }

    /**
     * Delegates this event-bonus building's description to the supplied visitor.
     *
     * @param visitor visitor that describes bonus-event buildings
     * @return visitor-provided building description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeBuildingBonusEvent(this);
    }

    /**
     * Returns the display name of this building type.
     *
     * @return {@code "Building Bonus Event"}
     */
    @Override
    public String getReadableName() {
        return "Building Bonus Event";
    }
}
