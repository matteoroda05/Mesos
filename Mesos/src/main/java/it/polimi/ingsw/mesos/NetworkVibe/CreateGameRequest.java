package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Globals;

public record CreateGameRequest(
        String gameName,
        int playerCount
) implements GameRequest {

    public CreateGameRequest {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (playerCount < Globals.MIN_NUM_PLAYERS || playerCount > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "playerCount must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
    }
}
