package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public record HeartbeatRequest(
        String gameName,
        String nickname,
        boolean joined
) implements GameRequest {

    public HeartbeatRequest {
        JoinGameRequest.validateNickname(nickname);

        if (joined) {
            JoinGameRequest.validateGameName(gameName);
        } else{
            if (gameName != null) {
                throw new IllegalArgumentException("gameName has to be null in lobby state");
            }
        }
    }

    public HeartbeatRequest (String nickname){
        this(null,  nickname, false);
    }

    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.heartbeat(this);
    }
}
