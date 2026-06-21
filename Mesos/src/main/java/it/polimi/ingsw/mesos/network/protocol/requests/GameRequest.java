package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer.ClientConnection;

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
