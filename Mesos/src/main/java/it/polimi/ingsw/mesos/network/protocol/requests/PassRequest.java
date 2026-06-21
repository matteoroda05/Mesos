package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Request to pass an optional action.
 *
 * @param gameName game name
 * @param nickname player nickname
 */
public record PassRequest(
        String gameName,
        String nickname
) implements GameRequest {

    /**
     * Validates required names.
     *
     * @throws IllegalArgumentException if game name or nickname is null/blank
     */
    public PassRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
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
        return service.pass(this);
    }
}
