package it.polimi.ingsw.mesos.NetworkVibe.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.NetworkVibe.GameViewUpdate;

public interface RemoteGameUpdateListener extends Remote {
    void onUpdate(GameViewUpdate update) throws RemoteException;
}
