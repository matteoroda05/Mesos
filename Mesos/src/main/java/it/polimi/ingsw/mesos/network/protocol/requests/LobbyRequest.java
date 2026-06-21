package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

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
