package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

/**
 * Request to reserve a player color before the game starts.
 *
 * @param gameName game name
 * @param nickname player nickname
 * @param color requested color
 */
public record ChooseColorRequest(
        String gameName,
        String nickname,
        Color color
) implements GameRequest {

    /**
     * Validates required fields.
     *
     * @throws IllegalArgumentException if game name or nickname is null/blank,
     *                                  or color is null
     */
    public ChooseColorRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
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
        return service.chooseColor(this);
    }
}
