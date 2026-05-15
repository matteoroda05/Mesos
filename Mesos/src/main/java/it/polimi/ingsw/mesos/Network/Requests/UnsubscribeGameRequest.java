package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Request to unsubscribe a socket client from updates for a game.
 *
 * @param gameName game name
 * @param nickname player nickname
 */
public record UnsubscribeGameRequest(
        String gameName,
        String nickname
) implements GameRequest {

    /**
     * Validates required unsubscription fields.
     *
     * @throws IllegalArgumentException if game name or nickname is null/blank
     */
    public UnsubscribeGameRequest {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }

    /**
     * Dispatches this request to the server service and removes the socket
     * connection from local update subscriptions.
     * The local unsubscribe is attempted even if the service result is a
     * failure, keeping the socket-side subscription state clear.
     *
     * @param service server service handling the request
     * @param connection socket client connection to unsubscribe
     * @return service operation result
     * @throws NullPointerException if connection is null, because local socket
     *                              subscription state must be updated
     */
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        GameOperationResult output = service.unsubscribe(this, connection);
        connection.unsubscribe(this);
        return output;
    }
}
