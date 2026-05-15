package it.polimi.ingsw.mesos.Network.Messages;

import it.polimi.ingsw.mesos.Network.GameServerMessage;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

/**
 * Serializable server response for operations that may complete successfully
 * or fail with an error code.
 *
 * @param success whether the operation succeeded
 * @param message human-readable result message
 * @param errorCode structured error code, normalized to
 *                  {@link OperationErrorCode#NONE} when null
 * @param snapshot optional game snapshot returned with the result
 * @param lobbySnapshot optional lobby snapshot returned with the result
 * @param rejoinAvailable whether the nickname operation found a rejoinable
 *                        disconnected session
 */
public record GameOperationResult(
        boolean success,
        String message,
        OperationErrorCode errorCode,
        GameSnapshot snapshot,
        LobbySnapshot lobbySnapshot,
        boolean rejoinAvailable
) implements GameServerMessage {

    /**
     * Normalizes blank messages and null error codes.
     * Blank messages become a generic success or failure message based on the
     * success flag.
     */
    public GameOperationResult {
        if (message == null || message.isBlank()) {
            message = success ? "Operation completed" : "Operation failed";
        }
        errorCode = errorCode == null ? OperationErrorCode.NONE : errorCode;
    }

    /**
     * Creates a successful operation result with an optional game snapshot.
     *
     * @param message result message, normalized when blank
     * @param snapshot optional game snapshot
     * @return successful operation result
     */
    public static GameOperationResult success(String message, GameSnapshot snapshot) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, snapshot, null, false);
    }

    /**
     * Creates a successful operation result with optional game and lobby
     * snapshots.
     *
     * @param message result message, normalized when blank
     * @param snapshot optional game snapshot
     * @param lobbySnapshot optional lobby snapshot
     * @return successful operation result
     */
    public static GameOperationResult success(String message, GameSnapshot snapshot, LobbySnapshot lobbySnapshot) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, snapshot, lobbySnapshot, false);
    }

    /**
     * Creates a successful lobby operation result.
     *
     * @param message result message, normalized when blank
     * @param lobbySnapshot optional lobby snapshot
     * @return successful lobby result
     */
    public static GameOperationResult successLobby(String message, LobbySnapshot lobbySnapshot) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, null, lobbySnapshot, false);
    }

    /**
     * Creates a failed operation result without a structured error code.
     *
     * @param message result message, normalized when blank
     * @param snapshot optional snapshot describing current state
     * @return failed operation result
     */
    public static GameOperationResult failure(String message, GameSnapshot snapshot) {
        return new GameOperationResult(false, message, OperationErrorCode.NONE, snapshot, null, false);
    }

    /**
     * Creates a failed operation result with a structured error code.
     *
     * @param message result message, normalized when blank
     * @param errorCode structured error code, normalized when null
     * @param snapshot optional snapshot describing current state
     * @return failed operation result
     */
    public static GameOperationResult failure(String message, OperationErrorCode errorCode, GameSnapshot snapshot) {
        return new GameOperationResult(false, message, errorCode, snapshot, null, false);
    }

    /**
     * Creates a failed operation result with optional game and lobby snapshots.
     *
     * @param message result message, normalized when blank
     * @param errorCode structured error code, normalized when null
     * @param snapshot optional game snapshot
     * @param lobbySnapshot optional lobby snapshot
     * @return failed operation result
     */
    public static GameOperationResult failure(
            String message,
            OperationErrorCode errorCode,
            GameSnapshot snapshot,
            LobbySnapshot lobbySnapshot
    ) {
        return new GameOperationResult(false, message, errorCode, snapshot, lobbySnapshot, false);
    }

    /**
     * Creates a successful nickname result.
     *
     * @param message result message, normalized when blank
     * @param rejoinAvailable true when a disconnected session can be rejoined
     * @return successful nickname result
     */
    public static GameOperationResult successNickname(String message, boolean rejoinAvailable) {
        return new GameOperationResult(true, message, OperationErrorCode.NONE, null, null, rejoinAvailable);
    }

    /**
     * Delivers this response to a socket client by enqueuing it for the waiting
     * request exchange.
     *
     * @param context socket client context containing the response queue
     * @throws NullPointerException if the context or response queue is null
     */
    @Override
    public void messageExecute(SocketGameClient.SocketClientContext context) {
        context.responses().offer(this);
    }
}
