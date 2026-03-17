package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Controller.PlayerController;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public class BuildingEndGame extends BuildingCard {
    private final boolean double_builders_pp;
    private final int character_set_bonus_prestige;
    private final int character_multiplier;
    private final CharacterType character_type;
    private final int prestige_bonus;

    public BuildingEndGame(
            Era era,
            int min_players,
            int food_cost,
            int prestige_points,
            boolean double_builders_pp,
            int character_set_bonus_prestige,
            int character_multiplier,
            CharacterType character_type,
            int prestige_bonus
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_END_GAME);

        if (character_set_bonus_prestige < 0 || character_set_bonus_prestige > 6)
            throw new IllegalArgumentException("Invalid prestige bonus per character set: " + character_set_bonus_prestige);
        if (character_multiplier < 0 || character_multiplier > 5)
            throw new IllegalArgumentException("Invalid character multiplier: " + character_multiplier);
        if (prestige_bonus != 0 && prestige_bonus != 25)
            throw new IllegalArgumentException("Invalid prestige points bonus: " + prestige_bonus);

        // ensure only one effect is active
        int count = !double_builders_pp ? 1 : 0;
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

    public void effect(PlayerController visitor) {
        if (prestige_bonus != 0) {
            visitor.addPrestige(prestige_bonus, owner.getColor());
            return;
        }

        // add one more time the builders prestige: one time is done by default
        if (double_builders_pp) {
            visitor.addPrestige(owner.getTribe().buildersEndGamePrestige(), owner.getColor());
            return;
        }

        if (character_multiplier != 0) {
            final int bonus_prestige = owner.getTribe().getNumCharacterOfType(character_type) * character_multiplier;
            visitor.addPrestige(bonus_prestige, owner.getColor());
            return;
        }

        // number of sets completed = min number of character cards of one type
        int min = Integer.MAX_VALUE;
        for (CharacterType type : CharacterType.values()) {
            min = Math.min(min, owner.getTribe().getNumCharacterOfType(type));
        }
        final int prestige_bonus = min * character_set_bonus_prestige;
        visitor.addPrestige(prestige_bonus, owner.getColor());
    }
}
