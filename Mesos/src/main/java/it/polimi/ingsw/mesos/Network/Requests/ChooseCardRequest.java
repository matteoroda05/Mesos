package it.polimi.ingsw.mesos.Network.Requests;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Request to choose a visible card from a market row.
 *
 * @param gameName game name
 * @param nickname player nickname
 * @param cardIndex zero-based card index in the selected row
 * @param topRow true for the top row, false for the bottom row
 */
public record ChooseCardRequest(
        String gameName,
        String nickname,
        int cardIndex,
        boolean topRow
) implements GameRequest {

    /**
     * Validates required names and card index.
     *
     * @throws IllegalArgumentException if game name or nickname is null/blank,
     *                                  or card index is negative
     */
    public ChooseCardRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
        if (cardIndex < 0) {
            throw new IllegalArgumentException("cardIndex cannot be negative");
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
    public GameOperationResult requestToService(MesosServerService service, SocketGameServer.ClientConnection connection) {
        return service.chooseCard(this);
    }
}
