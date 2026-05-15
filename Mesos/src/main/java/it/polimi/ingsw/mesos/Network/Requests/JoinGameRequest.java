package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Request to join a game, or to rejoin a previously disconnected session.
 *
 * @param gameName game to join; may be null only when {@code rejoin} is true
 * @param nickname player nickname
 * @param rejoin true when the server should look for a rejoinable session
 *               instead of joining the named game normally
 */
public record JoinGameRequest(
        String gameName,
        String nickname,
        boolean rejoin
) implements GameRequest {

    /**
     * Creates a regular join request.
     *
     * @param gameName game to join
     * @param nickname player nickname
     * @throws IllegalArgumentException if game name or nickname is null/blank
     */
    public JoinGameRequest(String gameName, String nickname) {
        this(gameName, nickname, false);
    }

    /**
     * Validates required join fields.
     * Rejoin requests allow a null game name because the server resolves the
     * previous game from the nickname; regular joins require a game name.
     *
     * @throws IllegalArgumentException if nickname is null/blank, or if a
     *                                  regular join has a null/blank game name
     */
    public JoinGameRequest {
        validateNickname(nickname);

        if (!rejoin) {
            validateGameName(gameName);
        }
    }

    /**
     * Validates that a game name can identify a concrete game.
     *
     * @param gameName game name to validate
     * @throws IllegalArgumentException if gameName is null or blank
     */
    static void validateGameName(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
    }

    /**
     * Validates that a nickname can identify a player.
     *
     * @param nickname nickname to validate
     * @throws IllegalArgumentException if nickname is null or blank
     */
    static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }

    /**
     * Dispatches this request to the server service.
     *
     * @param service server service handling the request
     * @param connection socket client connection, unused by this request
     * @return service operation result
     */
    @Override
    public GameOperationResult requestToService (MesosServerService service, SocketGameServer.ClientConnection connection){
        return service.joinGame(this);
    }
}
