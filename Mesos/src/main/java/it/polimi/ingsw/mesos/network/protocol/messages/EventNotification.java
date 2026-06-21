package it.polimi.ingsw.mesos.network.protocol.messages;

import it.polimi.ingsw.mesos.dto.event.EventLogEntry;
import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessage;
import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessageVisitor;
import it.polimi.ingsw.mesos.network.client.GameUpdateListener;
import it.polimi.ingsw.mesos.network.client.socket.SocketGameClient;

/**
 * Serializable asynchronous notification summarizing a resolved game event
 * (an event round or a single turn-order return) to be shown to the user.
 *
 * @param gameName name of the game the event belongs to
 * @param entry structured event payload to render
 */
public record EventNotification(String gameName, EventLogEntry entry) implements GameServerMessage {

    /**
     * Validates the notification.
     *
     * @throws IllegalArgumentException if the game name is null/blank or the
     *                                  entry is null
     */
    public EventNotification {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (entry == null) {
            throw new IllegalArgumentException("entry cannot be null");
        }
    }

    /**
     * Delivers this notification to the current socket update listener, if one
     * is registered.
     *
     * @throws NullPointerException if the context is null
     */
    @Override
    public void accept(GameServerMessageVisitor visitor) {
        visitor.visitEventNotification(this);
    }

    /**
     * Sends this event notification to the registered socket update listener.
     *
     * @param context socket client context that may hold an update listener
     */
    @Override
    public void messageExecute(SocketGameClient.SocketClientContext context) {
        GameUpdateListener listener = context.updateListener();
        if (listener != null) {
            listener.onUpdate(this);
        }
    }
}
