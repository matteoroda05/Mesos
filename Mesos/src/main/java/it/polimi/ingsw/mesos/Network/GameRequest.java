package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer.ClientConnection;

import java.io.Serializable;

/**
 * Sealed contract for serializable client commands that delegate their
 * execution to a {@link MesosServerService}.
 */
public sealed interface GameRequest extends Serializable permits CreateGameRequest, JoinGameRequest, SnapshotRequest,
        ChooseOfferRequest, ChooseCardRequest, SubscribeGameRequest, UnsubscribeGameRequest, LobbyRequest,
        HeartbeatRequest, NicknameRequest, PassRequest, ChooseColorRequest, RankingsRequest {

    /**
     * Dispatches this request to the server service for its client connection.
     *
     * @param service service that owns the game operations
     * @param connection connection that submitted the request
     * @return result produced by the service operation
     */
    GameOperationResult requestToService (MesosServerService service, ClientConnection connection);
}
