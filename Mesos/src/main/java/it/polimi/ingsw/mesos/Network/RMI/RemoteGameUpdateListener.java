package it.polimi.ingsw.mesos.Network.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.Network.GameServerMessage;

public interface RemoteGameUpdateListener extends Remote {
    void onUpdate(GameServerMessage update) throws RemoteException;
}
