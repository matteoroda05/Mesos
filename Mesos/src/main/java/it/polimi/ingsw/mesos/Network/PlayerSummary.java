package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.Color;

/**
 * Serializable summary of a player exposed to clients.
 *
 * @param nickname player nickname
 * @param color player color, null before the game starts
 * @param food current food amount
 * @param prestigePoints current prestige points
 * @param characterCount number of character cards owned
 * @param buildingCount number of building cards owned
 */
public record PlayerSummary(
        String nickname,
        Color color,
        int food,
        int prestigePoints,
        int characterCount,
        int buildingCount,
        List<CardSummary> characterCards,
        List<CardSummary> buildingCards
) implements Serializable {

    public PlayerSummary(
            String nickname,
            Color color,
            int food,
            int prestigePoints,
            int characterCount,
            int buildingCount
    ) {
        this(nickname, color, food, prestigePoints, characterCount, buildingCount, List.of(), List.of());
    }

    /**
     * Validates player identity.
     *
     * @throws IllegalArgumentException if nickname is null or blank
     */
    public PlayerSummary {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
        characterCards = characterCards == null ? List.of() : List.copyOf(characterCards);
        buildingCards = buildingCards == null ? List.of() : List.copyOf(buildingCards);
    }
}
