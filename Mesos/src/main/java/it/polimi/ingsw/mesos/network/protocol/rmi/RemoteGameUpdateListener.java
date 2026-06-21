package it.polimi.ingsw.mesos.network.protocol.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessage;

/**
 * Defines the remote callback contract used to push game messages to RMI
 * clients.
 */
public interface RemoteGameUpdateListener extends Remote {
    /**
     * Receives a server-pushed update over RMI.
     *
     * @param update message delivered by the server
     * @throws RemoteException if the remote delivery fails
     */
    void onUpdate(GameServerMessage update) throws RemoteException;
}
