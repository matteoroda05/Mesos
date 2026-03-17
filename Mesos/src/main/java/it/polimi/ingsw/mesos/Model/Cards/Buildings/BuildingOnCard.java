package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import it.polimi.ingsw.mesos.Controller.PlayerController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

import java.util.Set;

public class BuildingOnCard extends BuildingCard {
    private final int food_bonus;
    private final boolean set_completion;
    private final boolean inventors_pair;
    private Set<InventionIcon> inventor_icons;

    public BuildingOnCard (
            Era era,
            int min_players,
            int food_cost,
            int prestige_points,
            int food_bonus,
            boolean set_completion,
            boolean inventors_pair
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_ACQUIRE_CHARACTER);

        if (food_bonus <= 0 || food_bonus > 5) throw new IllegalArgumentException("Invalid food bonus: " + food_bonus);
        if (set_completion == inventors_pair) throw new IllegalArgumentException("Building must have one and only one effect!");

        this.food_bonus = food_bonus;
        this.set_completion = set_completion;
        this.inventors_pair = inventors_pair;
    }

    @Override
    protected void onAssignment() {
        if (inventors_pair) {
            inventor_icons = owner.getTribe().inventionIcons();
        }
    }

    public void effect(CharacterCard card, PlayerController visitor) {
        if (inventors_pair) {
            if (card.getClass() != InventorCard.class) return;

            final InventionIcon icon = ((InventorCard) card).getInventionIcon();
            final boolean not_in_set = inventor_icons.add(icon);
            if (!not_in_set) visitor.addFood(food_bonus, owner.getColor());
            return;
        }

        // ensure there's only one type with the least number of cards and that it is the one the player just received
        int min = Integer.MAX_VALUE;
        int counter = 0;
        CharacterType to_get_bonus = null;
        for (CharacterType type : CharacterType.values()) {
            final int amount = owner.getTribe().getNumCharacterOfType(type);
            if (amount < min) {
                min = amount;
                counter = 1;
                to_get_bonus = type;
            } else if (amount == min) {
                counter++;
            }
        }

        if (counter == 1 && to_get_bonus == card.getCharacterType()) {
            visitor.addFood(food_bonus, owner.getColor());
        }
    }
}
