package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public record JoinGameRequest(
        String gameName,
        String nickname
) implements GameRequest {

    public JoinGameRequest {
        validateGameName(gameName);
        validateNickname(nickname);
    }

    static void validateGameName(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
    }

    static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }

    @Override
    public GameOperationResult requestToService (MesosServerService service, SocketGameServer.ClientConnection connection){
        return service.joinGame(this);
    }
}
