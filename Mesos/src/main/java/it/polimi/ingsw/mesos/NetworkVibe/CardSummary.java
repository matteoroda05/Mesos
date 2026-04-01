package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public record CardSummary(
        int selectionIndex,
        String type,
        Era era,
        boolean event,
        String details,
        Integer foodCost,
        Integer prestigePoints
) implements Serializable {

    public CardSummary {
        if (selectionIndex < 0) {
            throw new IllegalArgumentException("selectionIndex cannot be negative");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type cannot be null or blank");
        }
        if (details == null) {
            details = "";
        }
    }
}
