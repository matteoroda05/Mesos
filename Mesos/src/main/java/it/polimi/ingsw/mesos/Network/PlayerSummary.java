package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

import it.polimi.ingsw.mesos.Model.Enums.Color;

public record PlayerSummary(
        String nickname,
        Color color,
        int food,
        int prestigePoints,
        int characterCount,
        int buildingCount
) implements Serializable {

    public PlayerSummary {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }
}
