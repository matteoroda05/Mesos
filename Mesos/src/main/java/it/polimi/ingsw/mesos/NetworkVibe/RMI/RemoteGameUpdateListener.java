package it.polimi.ingsw.mesos.Network.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.Network.GameViewUpdate;

public interface RemoteGameUpdateListener extends Remote {
    void onUpdate(GameViewUpdate update) throws RemoteException;
}
