package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

/**
 * Serializable summary of a game visible in the lobby.
 *
 * @param gameName game name
 * @param playerCount configured number of players
 * @param joinedPlayers number of players already joined
 * @param availableSeats number of seats still available
 */
public record LobbyGameSummary(
        String gameName,
        int playerCount,
        int joinedPlayers,
        int availableSeats
) implements Serializable {

    /**
     * Validates lobby summary consistency.
     *
     * @throws IllegalArgumentException if game name is null/blank, player count
     *                                  is not positive, joined players is out
     *                                  of range, or available seats do not
     *                                  complete the player count
     */
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
