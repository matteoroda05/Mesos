package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

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
