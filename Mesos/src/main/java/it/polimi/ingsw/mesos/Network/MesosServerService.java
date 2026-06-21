package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

/**
 * Defines the server-side operations used by network transports to manage
 * games, players, requests, and live updates.
 */
public interface MesosServerService {
    /**
     * Returns the games visible in the lobby.
     *
     * @return result containing the current lobby snapshot
     */
    GameOperationResult getLobby();

    /**
     * Creates a game from the supplied configuration.
     *
     * @param request game configuration and creator identity
     * @return result of the creation attempt
     */
    GameOperationResult createGame(CreateGameRequest request);

    /**
     * Adds a player to an existing game.
     *
     * @param request game and player to join
     * @return result of the join attempt
     */
    GameOperationResult joinGame(JoinGameRequest request);

    /**
     * Produces the current game snapshot requested by a player.
     *
     * @param request game and player whose view is requested
     * @return result containing the snapshot when available
     */
    GameOperationResult getSnapshot(SnapshotRequest request);

    /**
     * Applies an offer-tile choice to a game.
     *
     * @param request offer selection to apply
     * @return result of the selection attempt
     */
    GameOperationResult chooseOffer(ChooseOfferRequest request);

    /**
     * Applies a market-card choice to a game.
     *
     * @param request card selection to apply
     * @return result of the selection attempt
     */
    GameOperationResult chooseCard(ChooseCardRequest request);

    /**
     * Reserves a player color before the game begins.
     *
     * @param request color choice to apply
     * @return result of the reservation attempt
     */
    GameOperationResult chooseColor(ChooseColorRequest request);

    /**
     * Skips an optional action for a player.
     *
     * @param request player and game requesting the pass
     * @return result of the pass attempt
     */
    GameOperationResult pass(PassRequest request);

    /**
     * Registers a sink for game updates.
     *
     * @param request subscription identifying the game and player
     * @param updateSink recipient of subsequent server messages
     * @return result of the subscription attempt
     */
    GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink);

    /**
     * Removes a registered game-update sink.
     *
     * @param request subscription identifying the game and player
     * @param updateSink sink to unregister
     * @return result of the unsubscription attempt
     */
    GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink);

    /**
     * Records a heartbeat from a connected player.
     *
     * @param request heartbeat to process
     * @return result of the heartbeat operation
     */
    GameOperationResult heartbeat(HeartbeatRequest request);

    /**
     * Registers or updates a client's nickname.
     *
     * @param request nickname request to process
     * @return result of the nickname operation
     */
    GameOperationResult nickname(NicknameRequest request);

    /**
     * Returns the ranking for the requested game.
     *
     * @param request ranking request to process
     * @return result containing rankings when available
     */
    GameOperationResult getRankings(RankingsRequest request);
}
