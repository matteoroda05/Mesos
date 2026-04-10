package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Model.Enums.Color;

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
}
