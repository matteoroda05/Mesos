package it.polimi.ingsw.mesos.Network.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;

public interface RemoteMesosServer extends Remote {
    GameOperationResult getLobby() throws RemoteException;

    GameOperationResult nickname(NicknameRequest request) throws RemoteException;

    GameOperationResult createGame(CreateGameRequest request) throws RemoteException;

    GameOperationResult joinGame(JoinGameRequest request) throws RemoteException;

    GameOperationResult getSnapshot(SnapshotRequest request) throws RemoteException;

    GameOperationResult chooseOffer(ChooseOfferRequest request) throws RemoteException;

    GameOperationResult chooseCard(ChooseCardRequest request) throws RemoteException;

    GameOperationResult pass(PassRequest request) throws RemoteException;

    GameOperationResult subscribe(SubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;

    GameOperationResult unsubscribe(UnsubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;

    GameOperationResult heartbeat(HeartbeatRequest request) throws RemoteException;
}
