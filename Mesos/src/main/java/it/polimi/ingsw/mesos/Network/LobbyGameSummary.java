package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

public record LobbyGameSummary(
        String gameName,
        int playerCount,
        int joinedPlayers,
        int availableSeats
) implements Serializable {

    public LobbyGameSummary {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (playerCount <= 0) {
            throw new IllegalArgumentException("playerCount must be positive");
        }
        if (joinedPlayers < 0 || joinedPlayers > playerCount) {
            throw new IllegalArgumentException("joinedPlayers must be between 0 and playerCount");
        }
        if (availableSeats < 0 || joinedPlayers + availableSeats != playerCount) {
            throw new IllegalArgumentException("availableSeats must complete the player count");
        }
    }
}
