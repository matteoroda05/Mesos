package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

/**
 * Request to validate or register a nickname before joining a game.
 *
 * @param nickname player nickname
 */
public record NicknameRequest(String nickname) implements GameRequest {
    /**
     * Validates nickname format.
     *
     * @throws IllegalArgumentException if nickname is null or blank
     */
    public NicknameRequest {
        JoinGameRequest.validateNickname(nickname);
    }

    /**
     * Dispatches this request to the server service.
     *
     * @param service server service handling the request
     * @param connection socket client connection, unused by this request
     * @return service operation result, including rejoin availability when
     *         applicable
     */
    @Override
    public GameOperationResult requestToService(
            MesosServerService service,
            SocketGameServer.ClientConnection connection
    ) {
        return service.nickname(this);
    }
}
