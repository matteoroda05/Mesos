package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

import java.io.IOException;
import java.io.Serializable;

public interface GameServerMessage extends Serializable {
    void accept(GameServerMessageVisitor visitor);

    default void messageExecute(SocketGameClient.SocketClientContext context) throws IOException {
        throw new IOException("Unsupported server message: " + this.getClass().getSimpleName());
    }
}
