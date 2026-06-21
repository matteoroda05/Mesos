package it.polimi.ingsw.mesos.Network.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;

/**
 * Defines the RMI-exposed counterpart of the game server's client-facing
 * operations.
 */
public interface RemoteMesosServer extends Remote {
    /**
     * Retrieves the current lobby through RMI.
     *
     * @return result containing the lobby snapshot
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult getLobby() throws RemoteException;

    /**
     * Registers or updates a client nickname through RMI.
     *
     * @param request nickname request to process
     * @return result of the nickname operation
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult nickname(NicknameRequest request) throws RemoteException;

    /**
     * Creates a game through RMI.
     *
     * @param request game configuration and creator identity
     * @return result of the creation attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult createGame(CreateGameRequest request) throws RemoteException;

    /**
     * Joins a player to a game through RMI.
     *
     * @param request game and player to join
     * @return result of the join attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult joinGame(JoinGameRequest request) throws RemoteException;

    /**
     * Retrieves a game snapshot through RMI.
     *
     * @param request game and player whose view is requested
     * @return result containing the snapshot when available
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult getSnapshot(SnapshotRequest request) throws RemoteException;

    /**
     * Applies an offer-tile choice through RMI.
     *
     * @param request offer selection to apply
     * @return result of the selection attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult chooseOffer(ChooseOfferRequest request) throws RemoteException;

    /**
     * Applies a market-card choice through RMI.
     *
     * @param request card selection to apply
     * @return result of the selection attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult chooseCard(ChooseCardRequest request) throws RemoteException;

    /**
     * Reserves a player color through RMI.
     *
     * @param request color choice to apply
     * @return result of the reservation attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult chooseColor(ChooseColorRequest request) throws RemoteException;

    /**
     * Skips an optional action through RMI.
     *
     * @param request player and game requesting the pass
     * @return result of the pass attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult pass(PassRequest request) throws RemoteException;

    /**
     * Registers a remote listener for game updates.
     *
     * @param request subscription identifying the game and player
     * @param listener remote recipient of server messages
     * @return result of the subscription attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult subscribe(SubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;

    /**
     * Removes a remote game-update listener.
     *
     * @param request subscription identifying the game and player
     * @param listener remote listener to unregister
     * @return result of the unsubscription attempt
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult unsubscribe(UnsubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;

    /**
     * Processes a client heartbeat through RMI.
     *
     * @param request heartbeat to process
     * @return result of the heartbeat operation
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult heartbeat(HeartbeatRequest request) throws RemoteException;

    /**
     * Retrieves game rankings through RMI.
     *
     * @param request ranking request to process
     * @return result containing rankings when available
     * @throws RemoteException if the remote invocation fails
     */
    GameOperationResult getRankings(RankingsRequest request) throws RemoteException;
}
