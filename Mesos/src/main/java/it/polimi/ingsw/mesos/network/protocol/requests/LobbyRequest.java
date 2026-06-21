package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

/**
 * Request to retrieve the current lobby state.
 */
public record LobbyRequest() implements GameRequest {
    /**
     * Dispatches this request to the server service.
     *
     * @param service server service handling the request
     * @param connection socket client connection, unused by this request
     * @return service operation result containing lobby data
     */
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.getLobby();
    }
}
