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
 * @param shamanStars total ritual stars from shaman cards
 * @param buildersDiscount total building-cost food discount from builder cards
 * @param gatherersDiscount total sustenance-event food discount from gatherer cards
 * @param buildersPrestige total end-game prestige printed on builder cards
 * @param buildingsPrestige total static prestige printed on owned building cards
 * @param inventorTypeCount number of distinct invention icon types owned
 */
public record PlayerSummary(
        String nickname,
        Color color,
        int food,
        int prestigePoints,
        int characterCount,
        int buildingCount,
        List<CardSummary> characterCards,
        List<CardSummary> buildingCards,
        int shamanStars,
        int buildersDiscount,
        int gatherersDiscount,
        int buildersPrestige,
        int buildingsPrestige,
        int inventorTypeCount
) implements Serializable {

    public PlayerSummary(
            String nickname,
            Color color,
            int food,
            int prestigePoints,
            int characterCount,
            int buildingCount
    ) {
        this(nickname, color, food, prestigePoints, characterCount, buildingCount,
                List.of(), List.of(), 0, 0, 0, 0, 0, 0);
    }

    public PlayerSummary(
            String nickname,
            Color color,
            int food,
            int prestigePoints,
            int characterCount,
            int buildingCount,
            List<CardSummary> characterCards,
            List<CardSummary> buildingCards
    ) {
        this(nickname, color, food, prestigePoints, characterCount, buildingCount,
                characterCards, buildingCards, 0, 0, 0, 0, 0, 0);
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
        if (food < 0) {
            throw new IllegalArgumentException("food cannot be negative");
        }
        if (shamanStars < 0) {
            throw new IllegalArgumentException("shamanStars cannot be negative");
        }
        if (buildersPrestige < 0) {
            throw new IllegalArgumentException("buildersPrestige cannot be negative");
        }
         if (buildingsPrestige < 0) {
            throw new IllegalArgumentException("buildingsPrestige cannot be negative");
        }
        if (inventorTypeCount < 0) {
            throw new IllegalArgumentException("inventorTypeCount cannot be negative");
        }
        if (buildersDiscount < 0) {
            throw new IllegalArgumentException("buildersDiscount cannot be negative");
        }
        if (gatherersDiscount < 0) {
            throw new IllegalArgumentException("gatherersDiscount cannot be negative");
        }
        if (characterCount < 0) {
            throw new IllegalArgumentException("characterCount cannot be negative");
        }
        if (buildingCount < 0) {
            throw new IllegalArgumentException("buildingCount cannot be negative");
        }
        characterCards = characterCards == null ? List.of() : List.copyOf(characterCards);
        buildingCards = buildingCards == null ? List.of() : List.copyOf(buildingCards);
    }
}
