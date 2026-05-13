package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public record NicknameRequest(String nickname) implements GameRequest {
    public NicknameRequest {
        JoinGameRequest.validateNickname(nickname);
    }

    @Override
    public GameOperationResult requestToService(
            MesosServerService service,
            SocketGameServer.ClientConnection connection
    ) {
        return service.nickname(this);
    }
}

