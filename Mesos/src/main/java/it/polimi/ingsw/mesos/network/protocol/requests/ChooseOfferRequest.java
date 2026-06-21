package it.polimi.ingsw.mesos.network.protocol.requests;

import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.protocol.requests.GameRequest;
import it.polimi.ingsw.mesos.network.server.MesosServerService;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;

/**
 * Request to choose an offer tile.
 *
 * @param gameName game name
 * @param nickname player nickname
 * @param offerTileIndex zero-based offer tile index
 */
public record ChooseOfferRequest(
        String gameName,
        String nickname,
        int offerTileIndex
) implements GameRequest {

    /**
     * Validates required names and offer tile index.
     *
     * @throws IllegalArgumentException if game name or nickname is null/blank,
     *                                  or offer tile index is negative
     */
    public ChooseOfferRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
        if (offerTileIndex < 0) {
            throw new IllegalArgumentException("offerTileIndex cannot be negative");
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
        return service.chooseOffer(this);
    }
}
