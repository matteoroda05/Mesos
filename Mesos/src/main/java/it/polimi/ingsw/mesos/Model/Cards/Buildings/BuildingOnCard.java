package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class BuildingOnCard extends BuildingCard {
    private final int food_bonus;
    private final boolean inventors_pair;
    private Set<InventionIcon> inventor_icons;

    /**
     * Creates an on-character-acquired building from JSON data.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @param food_bonus food granted when the configured condition is met
     * @param set_completion true for set-completion scoring
     * @param inventors_pair true for repeated-invention scoring
     * @throws IllegalArgumentException if the food bonus is outside 1 to 5 or
     *                                  the two effect flags are both true or
     *                                  both false
     */
    @JsonCreator
    public BuildingOnCard(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points,
            @JsonProperty("foodBonus")      int food_bonus,
            @JsonProperty("setCompletion")  boolean set_completion,
            @JsonProperty("inventorsPair")  boolean inventors_pair
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_ACQUIRE_CHARACTER);

        if (food_bonus <= 0 || food_bonus > 5) throw new IllegalArgumentException("Invalid food bonus: " + food_bonus);
        if (set_completion == inventors_pair) throw new IllegalArgumentException("Building must have one and only one effect!");

        this.food_bonus = food_bonus;
        this.inventors_pair = inventors_pair;
    }

    /**
     * Captures the owner's current invention icons when this building tracks
     * repeated inventor icons.
     *
     * @throws NullPointerException if this building is an inventor-pair
     *                              building and the owner has not been set
     */
    @Override
    protected void onAssignment() {
        if (inventors_pair) {
            inventor_icons = owner.getTribe().inventionIcons();
        }
    }

    /**
     * Applies the on-character-acquired effect: repeated inventor icons grant
     * food for inventor-pair buildings, while set-completion buildings reward
     * when a new set is completed at acquisition.
     *
     * @param card character card just acquired by the owner
     * @param visitor controller used to grant food
     * @throws NullPointerException if the owner, card, visitor, or inventor
     *                              icon cache is null when required
     */
    public void effect(CharacterCard card, ModelController visitor) {
        if (inventors_pair) {
            if (card.getClass() != InventorCard.class) return;

            final InventionIcon icon = ((InventorCard) card).getInventionIcon();
            final boolean not_in_set = inventor_icons.add(icon);
            if (!not_in_set) visitor.playerAddFood(food_bonus, owner.getColor());
            return;
        }

        // ensure there's only one type with the least number of cards and that it is the one the player just received
        Map<Class<? extends CharacterCard>, Integer> counts = owner.getTribe().getNumCharactersOfEachType();
        counts.replace(card.getClass(), counts.get(card.getClass()) - 1);
        int min = Integer.MAX_VALUE;
        List<Class<? extends CharacterCard>> keys = new ArrayList<>();

        for (Class<? extends CharacterCard> type : counts.keySet()) {
            final int amount = counts.get(type);
            if (amount < min) {
                min = amount;
                keys.clear();
                keys.add(type);
            } else if (amount == min) {
                keys.add(type);
            }
        }

        if (keys.size() == 1 && keys.getFirst() == card.getClass()) {
            visitor.playerAddFood(food_bonus, owner.getColor());
        }
    }

    /**
     * Returns the food granted by this building effect.
     *
     * @return food bonus
     */
    public int getFoodBonus() {
        return food_bonus;
    }

    /**
     * Checks whether this building rewards completing character sets.
     *
     * @return true for set-completion buildings
     */
    public boolean isSetCompletion() {
        return !inventors_pair;
    }

    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeBuildingOnCard(this);
    }

    @Override
    public String getReadableName() {
        return "Building On Card";
    }
}
