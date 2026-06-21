package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;

import java.io.Closeable;
import java.io.IOException;

/**
 * Defines the transport-independent client operations exposed by the game
 * server, including update subscription lifecycle management.
 */
public interface GameClient extends Closeable {
    /**
     * Retrieves the list of games visible in the lobby.
     *
     * @return result containing the current lobby view
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult getLobby() throws IOException;

    /**
     * Sends a request to create a new game.
     *
     * @param request game configuration and creator identity
     * @return result of the creation attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult createGame(CreateGameRequest request) throws IOException;

    /**
     * Sends a request to join an existing game.
     *
     * @param request game and player to join
     * @return result of the join attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult joinGame(JoinGameRequest request) throws IOException;

    /**
     * Retrieves the latest snapshot for a game.
     *
     * @param request game and player whose view is requested
     * @return result containing the game snapshot when available
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult getSnapshot(SnapshotRequest request) throws IOException;

    /**
     * Sends a player's offer-tile choice.
     *
     * @param request offer selection to apply
     * @return result of the selection attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult chooseOffer(ChooseOfferRequest request) throws IOException;

    /**
     * Sends a player's market-card choice.
     *
     * @param request card selection to apply
     * @return result of the selection attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult chooseCard(ChooseCardRequest request) throws IOException;

    /**
     * Sends a player's totem-color reservation.
     *
     * @param request color choice to apply
     * @return result of the reservation attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult chooseColor(ChooseColorRequest request) throws IOException;

    /**
     * Sends a request to skip an optional action.
     *
     * @param request player and game requesting the pass
     * @return result of the pass attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult pass(PassRequest request) throws IOException;

    /**
     * Registers a listener for game updates.
     *
     * @param request subscription identifying the game and player
     * @param listener recipient of subsequent server messages
     * @return result of the subscription attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateListener listener) throws IOException;

    /**
     * Removes the caller's game-update subscription.
     *
     * @param request subscription identifying the game and player
     * @return result of the unsubscription attempt
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult unsubscribe(UnsubscribeGameRequest request) throws IOException;

    /**
     * Registers or updates a nickname with the server.
     *
     * @param request nickname request to send
     * @return result of the nickname operation
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult nickname(NicknameRequest request) throws IOException;

    /**
     * Retrieves the ranking for the requested game.
     *
     * @param request ranking request to send
     * @return result containing rankings when available
     * @throws IOException if the server cannot be contacted
     */
    GameOperationResult getRankings(RankingsRequest request) throws IOException;

    /**
     * Releases this client's transport resources and active update subscription.
     *
     * @throws IOException if the client cannot be closed cleanly
     */
    @Override
    void close() throws IOException;
}
