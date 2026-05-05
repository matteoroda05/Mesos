package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public record ChooseCardRequest(
        String gameName,
        String nickname,
        int cardIndex,
        boolean topRow
) implements GameRequest {

    public ChooseCardRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
        if (cardIndex < 0) {
            throw new IllegalArgumentException("cardIndex cannot be negative");
        }
    }

    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.chooseCard(this);
    }
}
