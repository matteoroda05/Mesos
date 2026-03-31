package it.polimi.ingsw.mesos.Network.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.Network.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.SnapshotRequest;

public interface RemoteMesosServer extends Remote {
    GameOperationResult createGame(CreateGameRequest request) throws RemoteException;

    GameOperationResult joinGame(JoinGameRequest request) throws RemoteException;

    GameOperationResult getSnapshot(SnapshotRequest request) throws RemoteException;

    GameOperationResult chooseOffer(ChooseOfferRequest request) throws RemoteException;

    GameOperationResult chooseCard(ChooseCardRequest request) throws RemoteException;
}
