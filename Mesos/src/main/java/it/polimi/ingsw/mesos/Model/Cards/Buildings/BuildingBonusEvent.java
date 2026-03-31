package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Enums.*;

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
        if (food_bonus < -1 || food_bonus > 1) throw new IllegalArgumentException("Invalid food bonus: " + food_bonus);
        if (pp_bonus < 0 || pp_bonus > 1) throw new IllegalArgumentException("Invalid prestige points bonus: " + pp_bonus);

        this.food_bonus = food_bonus;
        this.pp_bonus = pp_bonus;
        this.character_type = character_type;
    }

    public void effect(Map<Color, int[]> output, ModelController visitor) {
        final int multiplier = owner.getTribe().getNumCharacterOfType(character_type);
        final int prev_food = output.get(owner.getColor())[1];
        final int food_change = prev_food + multiplier * food_bonus;
        final int prestige_bonus = multiplier * pp_bonus;

        visitor.playerChangeFood(food_change, owner.getColor());
        visitor.playerChangePrestige(prestige_bonus, owner.getColor());
    }

    public int getFoodBonus() {
        return food_bonus;
    }

    public int getPrestigeBonus() {
        return pp_bonus;
    }

    public Class<? extends CharacterCard> getCharacterType() {
        return character_type;
    }
}
