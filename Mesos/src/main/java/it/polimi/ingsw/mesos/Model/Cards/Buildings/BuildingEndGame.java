package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;

import java.util.Collections;
import java.util.Map;

public class BuildingEndGame extends BuildingCard {
    private final boolean double_builders_pp;
    private final int character_set_bonus_prestige;
    private final int character_multiplier;
    private final Class<? extends CharacterCard> character_type;
    private final int prestige_bonus;

    private static final Map<String, Class<? extends CharacterCard>> CHARACTER_MAP = Map.of(
            "HUNTER",   HunterCard.class,
            "SHAMAN",   ShamanCard.class,
            "ARTIST",   ArtistCard.class,
            "BUILDER",  BuilderCard.class,
            "INVENTOR", InventorCard.class,
            "GATHERER", GathererCard.class
    );

    /**
     * Creates an end-game building from JSON data, converting the optional
     * character type string into the matching character class.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param double_builders_pp whether builder prestige is counted again
     * @param character_set_bonus_prestige prestige for each full character set
     * @param character_multiplier prestige per character of the configured type
     * @param character_type_str character type name from JSON
     * @param prestige_bonus fixed prestige bonus
     */
    @JsonCreator
    private BuildingEndGame(
            @JsonProperty("era")                      Era era,
            @JsonProperty("minPlayers")               int min_players,
            @JsonProperty("foodCost")                 int food_cost,
            @JsonProperty("prestigePoints")           int prestige_points,
            @JsonProperty("doubleBuildersPp")         boolean double_builders_pp,
            @JsonProperty("characterSetBonusPrestige")int character_set_bonus_prestige,
            @JsonProperty("characterMultiplier")      int character_multiplier,
            @JsonProperty("characterType")            String character_type_str,
            @JsonProperty("prestigeBonus")            int prestige_bonus
    ) {
        this(era, min_players, food_cost, prestige_points,
                double_builders_pp,
                character_set_bonus_prestige,
                character_multiplier,
                character_type_str != null ? CHARACTER_MAP.get(character_type_str) : null,
                prestige_bonus);
    }

    /**
     * Creates an end-game building with exactly one scoring effect.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param double_builders_pp whether builder prestige is counted again
     * @param character_set_bonus_prestige prestige for each full character set
     * @param character_multiplier prestige per character of the configured type
     * @param character_type character class counted by multiplier effects
     * @param prestige_bonus fixed prestige bonus
     * @throws IllegalArgumentException if any bonus value is outside its
     *                                  allowed range or the configuration does
     *                                  not enable exactly one effect
     */
    public BuildingEndGame(
            Era era,
            int min_players,
            int food_cost,
            int prestige_points,
            boolean double_builders_pp,
            int character_set_bonus_prestige,
            int character_multiplier,
            Class<? extends CharacterCard> character_type,
            int prestige_bonus
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_END_GAME);

        if (character_set_bonus_prestige < 0 || character_set_bonus_prestige > 8)
            throw new IllegalArgumentException("Invalid prestige bonus per character set: " + character_set_bonus_prestige);
        if (character_multiplier < 0 || character_multiplier > 5)
            throw new IllegalArgumentException("Invalid character multiplier: " + character_multiplier);
        if (prestige_bonus != 0 && prestige_bonus != 25)
            throw new IllegalArgumentException("Invalid prestige points bonus: " + prestige_bonus);

        // ensure only one effect is active
        int count = double_builders_pp ? 1 : 0;
        count += character_set_bonus_prestige != 0 ? 1 : 0;
        count += character_multiplier != 0 ? 1 : 0;
        count += prestige_bonus != 0 ? 1 : 0;
        if (count != 1) throw new IllegalArgumentException("Building must have one and only one effect!");

        this.double_builders_pp = double_builders_pp;
        this.character_multiplier = character_multiplier;
        this.character_set_bonus_prestige = character_set_bonus_prestige;
        this.character_type = character_type;
        this.prestige_bonus = prestige_bonus;
    }

    /**
     * Applies this building's single configured end-game scoring effect to its
     * owner.
     *
     * @param visitor controller used to update player prestige
     * @throws NullPointerException if the owner or visitor is null
     * @throws IllegalArgumentException if a multiplier effect has no character
     *                                  type
     */
    public void effect(ModelController visitor) {
        if (prestige_bonus != 0) {
            visitor.playerChangePrestige(prestige_bonus, owner.getColor());
            return;
        }

        // add one more time the builders prestige: one time is done by default
        if (double_builders_pp) {
            visitor.playerChangePrestige(owner.getTribe().buildersEndGamePrestige(), owner.getColor());
            return;
        }

        if (character_multiplier != 0) {
            final int bonus_prestige = owner.getTribe().getNumCharacterOfType(character_type) * character_multiplier;
            visitor.playerChangePrestige(bonus_prestige, owner.getColor());
            return;
        }

        // number of sets completed = min number of character cards of one type
        final int min = Collections.min(owner.getTribe().getNumCharactersOfEachType().values());
        final int prestige_bonus = min * character_set_bonus_prestige;
        visitor.playerChangePrestige(prestige_bonus, owner.getColor());
    }

    /**
     * Checks whether this building doubles builder end-game prestige.
     *
     * @return true when the double-builder effect is active
     */
    public boolean isDoubleBuildersPrestige() {
        return double_builders_pp;
    }

    /**
     * Returns the prestige granted for each complete character set.
     *
     * @return set bonus prestige, or zero when inactive
     */
    public int getCharacterSetBonusPrestige() {
        return character_set_bonus_prestige;
    }

    /**
     * Returns the prestige multiplier for the configured character type.
     *
     * @return character multiplier, or zero when inactive
     */
    public int getCharacterMultiplier() {
        return character_multiplier;
    }

    /**
     * Returns the character type used by multiplier effects.
     *
     * @return character type, or null when this building does not use one
     */
    public Class<? extends CharacterCard> getCharacterType() {
        return character_type;
    }

    /**
     * Returns the fixed prestige bonus.
     *
     * @return fixed prestige bonus, or zero when inactive
     */
    public int getPrestigeBonus() {
        return prestige_bonus;
    }

    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeBuildingEndGame(this);
    }

    @Override
    public String getReadableName() {
        return "Building End Game";
    }
}
