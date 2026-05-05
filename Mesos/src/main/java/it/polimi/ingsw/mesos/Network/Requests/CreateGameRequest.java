package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

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

    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.createGame(this);
    }
}
