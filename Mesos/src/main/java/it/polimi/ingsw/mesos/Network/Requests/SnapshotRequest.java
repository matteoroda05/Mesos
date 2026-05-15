package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Request to retrieve the latest snapshot for a game.
 *
 * @param gameName game name
 */
public record SnapshotRequest(String gameName) implements GameRequest {
    /**
     * Validates game name.
     *
     * @throws IllegalArgumentException if game name is null or blank
     */
    public SnapshotRequest {
        JoinGameRequest.validateGameName(gameName);
    }

    /**
     * Dispatches this request to the server service.
     *
     * @param service server service handling the request
     * @param connection socket client connection, unused by this request
     * @return service operation result containing the requested snapshot when
     *         available
     */
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.getSnapshot(this);
    }
}
