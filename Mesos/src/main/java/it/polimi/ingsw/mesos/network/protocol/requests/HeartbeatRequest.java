package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

/**
 * Request used by clients to report liveness.
 *
 * @param gameName game name when the player has joined a game, otherwise null
 * @param nickname player nickname
 * @param joined true when the heartbeat belongs to a joined game session,
 *               false when the player is still in the lobby
 */
public record HeartbeatRequest(
        String gameName,
        String nickname,
        boolean joined
) implements GameRequest {

    /**
     * Validates heartbeat state.
     * Joined heartbeats require a game name; lobby heartbeats require the game
     * name to be null.
     *
     * @throws IllegalArgumentException if nickname is null/blank, joined is
     *                                  true and game name is invalid, or joined
     *                                  is false and game name is not null
     */
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

    /**
     * Creates a lobby heartbeat for a nickname.
     *
     * @param nickname player nickname
     * @throws IllegalArgumentException if nickname is null or blank
     */
    public HeartbeatRequest (String nickname){
        this(null,  nickname, false);
    }

    /**
     * Dispatches this request to the server service.
     *
     * @param service server service handling the request
     * @param connection socket client connection, unused by this request
     * @return service operation result
     */
    @Override
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.heartbeat(this);
    }
}
