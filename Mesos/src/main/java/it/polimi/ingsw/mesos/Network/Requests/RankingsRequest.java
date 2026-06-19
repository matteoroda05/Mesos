package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Request to get the global game rankings.
 */
public record RankingsRequest(String gameName) implements GameRequest {
    /**
     * Dispatches this request to the server service.
     *
     * @param service server service handling the request
     * @param connection client connection making the request
     * @return operation result from the server
     */
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.getRankings(this);
    }
}
