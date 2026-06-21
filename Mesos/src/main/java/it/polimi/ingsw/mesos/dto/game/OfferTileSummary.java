package it.polimi.ingsw.mesos.dto.game;

import java.io.Serializable;

import it.polimi.ingsw.mesos.model.enums.Color;

/**
 * Serializable summary of an offer tile exposed to clients.
 *
 * @param index zero-based tile index
 * @param id tile identifier
 * @param topPicks number of top-row card picks granted by the tile
 * @param bottomPicks number of bottom-row card picks granted by the tile
 * @param foodGain food gained by choosing the tile
 * @param available true when the tile can currently be chosen
 * @param occupantColor color occupying the tile, or null when empty
 * @param occupantNickname nickname occupying the tile, or null when empty
 */
public record OfferTileSummary(
        int index,
        char id,
        int topPicks,
        int bottomPicks,
        int foodGain,
        boolean available,
        Color occupantColor,
        String occupantNickname
) implements Serializable {

    /**
     * Validates tile summary index.
     *
     * @throws IllegalArgumentException if index is negative
     */
    public OfferTileSummary {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
    }
}
