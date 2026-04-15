package it.polimi.ingsw.mesos.Network.Messages;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

public record GameOperationResult(
        boolean success,
        String message,
        GameSnapshot snapshot,
        String nickname,
        Color assignedColor
) implements GameServerMessage {

    public GameOperationResult {
        if (message == null || message.isBlank()) {
            message = success ? "Operation completed" : "Operation failed";
        }
    }

    public static GameOperationResult success(String message, GameSnapshot snapshot) {
        return new GameOperationResult(true, message, snapshot, null, null);
    }

    public static GameOperationResult success(String message, GameSnapshot snapshot, String nickname, Color color) {
        return new GameOperationResult(true, message, snapshot, nickname, color);
    }

    public static GameOperationResult failure(String message, GameSnapshot snapshot) {
        return new GameOperationResult(false, message, snapshot, null, null);
    }

    public static GameOperationResult failure(String message, GameSnapshot snapshot, String nickname, Color color) {
        return new GameOperationResult(false, message, snapshot, nickname, color);
    }

    @Override
    public void execute(SocketGameClient.SocketClientContext context) {
        // TODO: check if the this has been added and throw/do something
        context.responses().offer(this);
    }
}
