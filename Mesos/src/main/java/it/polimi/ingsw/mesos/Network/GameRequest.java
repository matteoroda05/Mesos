package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer.ClientConnection;

import java.io.Serializable;

public sealed interface GameRequest extends Serializable permits CreateGameRequest, JoinGameRequest, SnapshotRequest,
        ChooseOfferRequest, ChooseCardRequest, SubscribeGameRequest, UnsubscribeGameRequest, LobbyRequest,
        HeartbeatRequest, NicknameRequest, PassRequest, ChooseColorRequest {

    GameOperationResult requestToService (MesosServerService service, ClientConnection connection);
}
