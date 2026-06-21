package it.polimi.ingsw.mesos.Controller;

import java.util.List;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Color;

/**
 * Immutable view of a player's resources, owned cards, and derived tribe
 * statistics for use outside the model.
 */
public record PlayerData(
        String nickname,
        Color color,
        int food,
        int prestigePoints,
        List<CharacterCard> characterCards,
        List<BuildingCard> buildingCards,
        int shamanStars,
        int buildersDiscount,
        int gatherersDiscount,
        int buildersPrestige,
        int buildingsPrestige,
        int inventorTypeCount
) {}
