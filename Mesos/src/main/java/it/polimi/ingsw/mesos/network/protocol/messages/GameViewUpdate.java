package it.polimi.ingsw.mesos.network.protocol.messages;

import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessage;
import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessageVisitor;
import it.polimi.ingsw.mesos.dto.game.GameSnapshot;
import it.polimi.ingsw.mesos.network.client.GameUpdateListener;
import it.polimi.ingsw.mesos.network.client.socket.SocketGameClient;

/**
 * Serializable asynchronous game-view update sent by the server.
 *
 * @param gameName name of the game that changed
 * @param message human-readable update message
 * @param snapshot updated game snapshot
 */
public record GameViewUpdate(
        String gameName,
        String message,
        GameSnapshot snapshot
) implements GameServerMessage {

    /**
     * Validates the update and normalizes blank messages.
     *
     * @throws IllegalArgumentException if the game name is null/blank or the
     *                                  snapshot is null
     */
    public GameViewUpdate {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (message == null || message.isBlank()) {
            message = "Aggiornamento tavolo";
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }
    }

    /**
     * Delivers this update to the current socket update listener, if one is
     * registered.
     *
     * @throws NullPointerException if the context is null
     */
    @Override
    public void accept(GameServerMessageVisitor visitor) {
        visitor.visitGameViewUpdate(this);
    }

    /**
     * Sends this game-view update to the registered socket update listener.
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
