package it.polimi.ingsw.mesos.dto.game;

import java.io.Serializable;

import it.polimi.ingsw.mesos.model.enums.Color;

/**
 * Serializable summary of a turn-order space exposed to clients.
 *
 * @param index zero-based space index
 * @param foodBonus food bonus granted by the space
 * @param occupantColor color occupying the space, or null when empty
 * @param occupantNickname nickname occupying the space, or null when empty
 */
public record TurnOrderSpaceSummary(
        int index,
        int foodBonus,
        Color occupantColor,
        String occupantNickname
) implements Serializable {

    /**
     * Validates turn-order space index.
     *
     * @throws IllegalArgumentException if index is negative
     */
    public TurnOrderSpaceSummary {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
    }
}
