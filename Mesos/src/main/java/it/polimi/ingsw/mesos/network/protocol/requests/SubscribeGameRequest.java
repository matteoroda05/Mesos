package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

/**
 * Request to subscribe a socket client to updates for a game.
 *
 * @param gameName game name
 * @param nickname player nickname
 */
public record SubscribeGameRequest(
        String gameName,
        String nickname
) implements GameRequest {

    /**
     * Validates required subscription fields.
     *
     * @throws IllegalArgumentException if game name or nickname is null/blank
     */
    public SubscribeGameRequest {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }

    /**
     * Dispatches this request to the server service and registers the socket
     * connection for updates when the service accepts the subscription.
     *
     * @param service server service handling the request
     * @param connection socket client connection to subscribe
     * @return service operation result
     * @throws NullPointerException if connection is null and the service
     *                              succeeds, because a successful socket
     *                              subscription must be stored locally
     */
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        GameOperationResult output = service.subscribe(this, connection);
        if (output.success()){
            connection.subscribe(this);
        }
        return output;
    }
}
