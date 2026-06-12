package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

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
