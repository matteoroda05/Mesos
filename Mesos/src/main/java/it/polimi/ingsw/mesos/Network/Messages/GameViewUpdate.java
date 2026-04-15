package it.polimi.ingsw.mesos.Network.Messages;

import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.GameUpdateListener;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

public record GameViewUpdate(
        String gameName,
        String message,
        GameSnapshot snapshot
) implements GameServerMessage {

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

    @Override
    public void execute(SocketGameClient.SocketClientContext context) {
        GameUpdateListener listener = context.updateListener();
        if (listener != null) {
            listener.onUpdate(this);
        }
    }
}
