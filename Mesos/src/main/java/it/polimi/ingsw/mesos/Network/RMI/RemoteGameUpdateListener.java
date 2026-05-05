package it.polimi.ingsw.mesos.Network.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

public interface RemoteGameUpdateListener extends Remote {
    void onUpdate(GameViewUpdate update) throws RemoteException;
}
