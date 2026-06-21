package it.polimi.ingsw.mesos.network.protocol.messages;

import it.polimi.ingsw.mesos.network.client.socket.SocketGameClient;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents a serializable message sent from the game server to a client.
 * Implementations dispatch to a visitor and may handle socket delivery.
 */
public interface GameServerMessage extends Serializable {
    /**
     * Dispatches this server message to the matching visitor method.
     *
     * @param visitor visitor that handles this message type
     */
    void accept(GameServerMessageVisitor visitor);

    /**
     * Handles this message in a socket client's update context.
     * The default implementation rejects unsupported message types.
     *
     * @param context socket client context that received the message
     * @throws IOException always, unless an implementation overrides this method
     */
    default void messageExecute(SocketGameClient.SocketClientContext context) throws IOException {
        throw new IOException("Unsupported server message: " + this.getClass().getSimpleName());
    }
}
