package it.polimi.ingsw.mesos.Network.Messages;

import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

public record GameOperationResult(
        boolean success,
        String message,
        GameSnapshot snapshot
) implements GameServerMessage {

    public GameOperationResult {
        if (message == null || message.isBlank()) {
            message = success ? "Operation completed" : "Operation failed";
        }
    }

    public static GameOperationResult success(String message, GameSnapshot snapshot) {
        return new GameOperationResult(true, message, snapshot);
    }

    public static GameOperationResult failure(String message, GameSnapshot snapshot) {
        return new GameOperationResult(false, message, snapshot);
    }

    @Override
    public void messageExecute(SocketGameClient.SocketClientContext context) {
        // TODO: check if the this has been added and throw/do something
        context.responses().offer(this);
    }
}
