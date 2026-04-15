package it.polimi.ingsw.mesos.Network.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;

public interface RemoteMesosServer extends Remote {
    GameOperationResult createGame(CreateGameRequest request) throws RemoteException;

    GameOperationResult joinGame(JoinGameRequest request) throws RemoteException;

    GameOperationResult getSnapshot(SnapshotRequest request) throws RemoteException;

    GameOperationResult chooseOffer(ChooseOfferRequest request) throws RemoteException;

    GameOperationResult chooseCard(ChooseCardRequest request) throws RemoteException;

    GameOperationResult subscribe(SubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;

    GameOperationResult unsubscribe(UnsubscribeGameRequest request, RemoteGameUpdateListener listener) throws RemoteException;
}
