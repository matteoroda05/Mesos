package it.polimi.ingsw.mesos.Network.Messages;

import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

public record GameOperationResult(
        boolean success,
        String message,
        OperationErrorCode errorCode,
        GameSnapshot snapshot,
        LobbySnapshot lobbySnapshot,
        boolean rejoinAvailable
) implements GameServerMessage {

    public GameOperationResult {
        if (message == null || message.isBlank()) {
            message = success ? "Operation completed" : "Operation failed";
        }
        errorCode = errorCode == null ? OperationErrorCode.NONE : errorCode;
    }

    public static GameOperationResult success(String message, GameSnapshot snapshot) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, snapshot, null, false);
    }

    public static GameOperationResult success(String message, GameSnapshot snapshot, LobbySnapshot lobbySnapshot) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, snapshot, lobbySnapshot, false);
    }

    public static GameOperationResult successLobby(String message, LobbySnapshot lobbySnapshot) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, null, lobbySnapshot, false);
    }

    public static GameOperationResult failure(String message, GameSnapshot snapshot) {
        return new GameOperationResult(false, message, OperationErrorCode.NONE, snapshot, null, false);
    }

    public static GameOperationResult failure(String message, OperationErrorCode errorCode, GameSnapshot snapshot) {
        return new GameOperationResult(false, message, errorCode, snapshot, null, false);
    }

    public static GameOperationResult failure(
            String message,
            OperationErrorCode errorCode,
            GameSnapshot snapshot,
            LobbySnapshot lobbySnapshot
    ) {
        return new GameOperationResult(false, message, errorCode, snapshot, lobbySnapshot, false);
    }

    public static GameOperationResult successNickname(String message, boolean rejoinAvailable) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, null, null, rejoinAvailable);
    }

    @Override
    public void messageExecute(SocketGameClient.SocketClientContext context) {
        context.responses().offer(this);
    }
}
