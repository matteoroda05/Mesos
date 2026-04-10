package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

import it.polimi.ingsw.mesos.Model.Enums.Color;

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

    public OfferTileSummary {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
    }
}
