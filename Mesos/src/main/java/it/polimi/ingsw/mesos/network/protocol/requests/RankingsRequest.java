package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

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
