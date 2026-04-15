package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public record ChooseOfferRequest(
        String gameName,
        String nickname,
        int offerTileIndex
) implements GameRequest {

    public ChooseOfferRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
        if (offerTileIndex < 0) {
            throw new IllegalArgumentException("offerTileIndex cannot be negative");
        }
    }

    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.chooseOffer(this);
    }
}
