package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public record LobbyRequest() implements GameRequest {
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.getLobby();
    }
}
