package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public record SnapshotRequest(String gameName) implements GameRequest {
    public SnapshotRequest {
        JoinGameRequest.validateGameName(gameName);
    }

    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.getSnapshot(this);
    }
}
