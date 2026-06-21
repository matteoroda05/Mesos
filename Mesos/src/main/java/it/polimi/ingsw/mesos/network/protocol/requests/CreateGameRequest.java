package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Request to create a new game.
 *
 * @param gameName requested game name
 * @param playerCount requested number of players
 */
public record CreateGameRequest(
        String gameName,
        int playerCount
) implements GameRequest {

    /**
     * Validates game name and player count.
     *
     * @throws IllegalArgumentException if the game name is null/blank or the
     *                                  player count is outside the supported
     *                                  range
     */
    public CreateGameRequest {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (playerCount < Globals.MIN_NUM_PLAYERS || playerCount > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "playerCount must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
    }

    /**
     * Dispatches this request to the server service.
     *
     * @param service server service handling the request
     * @param connection socket client connection, unused by this request
     * @return service operation result
     */
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.createGame(this);
    }
}
