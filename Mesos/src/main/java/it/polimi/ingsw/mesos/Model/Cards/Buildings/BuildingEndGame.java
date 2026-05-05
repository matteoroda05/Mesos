package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
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

    public boolean isDoubleBuildersPrestige() {
        return double_builders_pp;
    }

    public int getCharacterSetBonusPrestige() {
        return character_set_bonus_prestige;
    }

    public int getCharacterMultiplier() {
        return character_multiplier;
    }

    public Class<? extends CharacterCard> getCharacterType() {
        return character_type;
    }

    public int getPrestigeBonus() {
        return prestige_bonus;
    }
}
