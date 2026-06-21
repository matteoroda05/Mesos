package it.polimi.ingsw.mesos.controller;

import java.util.List;

import it.polimi.ingsw.mesos.model.cards.BuildingCard;
import it.polimi.ingsw.mesos.model.cards.CharacterCard;
import it.polimi.ingsw.mesos.model.enums.Color;

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
