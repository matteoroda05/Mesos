package it.polimi.ingsw.mesos.NetworkVibe;

import java.io.Serializable;

import it.polimi.ingsw.mesos.Model.Enums.Color;

public record TurnOrderSpaceSummary(
        int index,
        int foodBonus,
        Color occupantColor,
        String occupantNickname
) implements Serializable {

    public TurnOrderSpaceSummary {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
    }
}
