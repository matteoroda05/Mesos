package it.polimi.ingsw.mesos.NetworkVibe.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.NetworkVibe.ChooseCardRequest;
import it.polimi.ingsw.mesos.NetworkVibe.ChooseOfferRequest;
import it.polimi.ingsw.mesos.NetworkVibe.CreateGameRequest;
import it.polimi.ingsw.mesos.NetworkVibe.GameOperationResult;
import it.polimi.ingsw.mesos.NetworkVibe.JoinGameRequest;
import it.polimi.ingsw.mesos.NetworkVibe.SnapshotRequest;
import it.polimi.ingsw.mesos.NetworkVibe.SubscribeGameRequest;
import it.polimi.ingsw.mesos.NetworkVibe.UnsubscribeGameRequest;

public interface RemoteMesosServer extends Remote {
    GameOperationResult createGame(CreateGameRequest request) throws RemoteException;

    GameOperationResult joinGame(JoinGameRequest request) throws RemoteException;

    GameOperationResult getSnapshot(SnapshotRequest request) throws RemoteException;

    GameOperationResult chooseOffer(ChooseOfferRequest request) throws RemoteException;

    GameOperationResult chooseCard(ChooseCardRequest request) throws RemoteException;

    GameOperationResult subscribe(SubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;

    GameOperationResult unsubscribe(UnsubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;
}
