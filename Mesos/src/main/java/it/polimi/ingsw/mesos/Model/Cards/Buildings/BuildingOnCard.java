package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class BuildingOnCard extends BuildingCard {
    private final int food_bonus;
    private final boolean inventors_pair;
    private Set<InventionIcon> inventor_icons;

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

    @Override
    protected void onAssignment() {
        if (inventors_pair) {
            inventor_icons = owner.getTribe().inventionIcons();
        }
    }

    public void effect(CharacterCard card, ModelController visitor) {
        if (inventors_pair) {
            if (card.getClass() != InventorCard.class) return;

            final InventionIcon icon = ((InventorCard) card).getInventionIcon();
            final boolean not_in_set = inventor_icons.add(icon);
            if (!not_in_set) visitor.playerChangeFood(food_bonus, owner.getColor());
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
            visitor.playerChangeFood(food_bonus, owner.getColor());
        }
    }
}
