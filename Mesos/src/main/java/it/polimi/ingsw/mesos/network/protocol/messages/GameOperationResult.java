package it.polimi.ingsw.mesos.network.protocol.messages;

import it.polimi.ingsw.mesos.database.LeaderboardEntry;
import it.polimi.ingsw.mesos.dto.game.GameSnapshot;
import it.polimi.ingsw.mesos.dto.lobby.LobbySnapshot;
import it.polimi.ingsw.mesos.network.protocol.OperationErrorCode;
import it.polimi.ingsw.mesos.network.client.socket.SocketGameClient;

import java.util.List;

/**
 * Serializable server response for an operation, delivered through the
 * request/response channel. Each response category is its own record so the
 * type system enforces which payload a response may carry: a {@link Snapshot}
 * carries a game snapshot, a {@link Lobby} a lobby snapshot, and so on.
 *
 * <p>The interface exposes only the fields common to every variant
 * ({@link #message()}, {@link #success()}, {@link #type()}). Variant-specific
 * payload lives on the concrete records: consumers branch on {@link #type()} and
 * downcast, or use {@link #snapshotOf(GameOperationResult)} /
 * {@link #lobbyOf(GameOperationResult)} for the snapshot/lobby fields that more
 * than one variant can carry.
 */
public sealed interface GameOperationResult extends GameServerMessage
        permits GameOperationResult.Snapshot, GameOperationResult.Lobby,
                GameOperationResult.Rankings, GameOperationResult.Nickname,
                GameOperationResult.SimpleOk, GameOperationResult.Err {

    /**
     * @return human-readable result message
     */
    String message();

    /**
     * @return discriminator identifying the concrete variant
     */
    ResultType type();

    /**
     * @return whether the operation succeeded; true for every variant except
     *         {@link Err}
     */
    default boolean success() {
        return type() != ResultType.ERROR;
    }

    /**
     * Not supported: a {@code GameOperationResult} is a request/response result
     * delivered through the dedicated responses queue, never a broadcast update
     * dispatched through the {@link GameServerMessageVisitor}.
     *
     * @param visitor update visitor (unused)
     * @throws UnsupportedOperationException always
     */
    @Override
    default void accept(GameServerMessageVisitor visitor) {
        throw new UnsupportedOperationException(
                "GameOperationResult is a request/response result, not a broadcast update");
    }

    /**
     * Delivers this response to a socket client by enqueuing it for the waiting
     * request exchange.
     *
     * @param context socket client context containing the response queue
     * @throws NullPointerException if the context or response queue is null
     */
    @Override
    default void messageExecute(SocketGameClient.SocketClientContext context) {
        context.responses().offer(this);
    }

    /**
     * Creates a successful result. When {@code snapshot} is null the response
     * carries no payload ({@link SimpleOk}); otherwise it carries the snapshot
     * ({@link Snapshot}).
     *
     * @param message result message
     * @param snapshot optional game snapshot
     * @return matching success variant
     */
    static GameOperationResult success(String message, GameSnapshot snapshot) {
        return snapshot == null ? new SimpleOk(message) : new Snapshot(message, snapshot);
    }

    /**
     * Creates a successful lobby result.
     *
     * @param message result message
     * @param lobbySnapshot lobby snapshot
     * @return lobby success variant
     */
    static GameOperationResult successLobby(String message, LobbySnapshot lobbySnapshot) {
        return new Lobby(message, lobbySnapshot);
    }

    /**
     * Creates a successful rankings result.
     *
     * @param message result message
     * @param rankings list of ranking entries
     * @return rankings success variant
     */
    static GameOperationResult successRankings(String message, List<LeaderboardEntry> rankings) {
        return new Rankings(message, rankings);
    }

    /**
     * Creates a successful nickname result.
     *
     * @param message result message
     * @param rejoinAvailable true when a disconnected session can be rejoined
     * @return nickname success variant
     */
    static GameOperationResult successNickname(String message, boolean rejoinAvailable) {
        return new Nickname(message, rejoinAvailable);
    }

    /**
     * Creates a failed result without a structured error code.
     *
     * @param message result message, normalized when blank
     * @param snapshot optional snapshot describing current state
     * @return error variant
     */
    static GameOperationResult failure(String message, GameSnapshot snapshot) {
        return new Err(message, OperationErrorCode.NONE, snapshot, null);
    }

    /**
     * Creates a failed result with a structured error code.
     *
     * @param message result message, normalized when blank
     * @param errorCode structured error code, normalized when null
     * @param snapshot optional snapshot describing current state
     * @return error variant
     */
    static GameOperationResult failure(String message, OperationErrorCode errorCode, GameSnapshot snapshot) {
        return new Err(message, errorCode, snapshot, null);
    }

    /**
     * Creates a failed result with optional game and lobby snapshots.
     *
     * @param message result message, normalized when blank
     * @param errorCode structured error code, normalized when null
     * @param snapshot optional game snapshot
     * @param lobbySnapshot optional lobby snapshot
     * @return error variant
     */
    static GameOperationResult failure(
            String message,
            OperationErrorCode errorCode,
            GameSnapshot snapshot,
            LobbySnapshot lobbySnapshot
    ) {
        return new Err(message, errorCode, snapshot, lobbySnapshot);
    }

    /**
     * Extracts the structured error code of a result. Only {@link Err} carries
     * one; every successful variant reports {@link OperationErrorCode#NONE}.
     *
     * @param result result to inspect, possibly null
     * @return the error code, or {@link OperationErrorCode#NONE} when the result
     *         is not an error
     */
    static OperationErrorCode errorCodeOf(GameOperationResult result) {
        if (result != null && result.type() == ResultType.ERROR) {
            return ((Err) result).errorCode();
        }
        return OperationErrorCode.NONE;
    }

    /**
     * Extracts the game snapshot carried by a result, if any. Both
     * {@link Snapshot} (success) and {@link Err} (failure with current state)
     * can carry one.
     *
     * @param result result to inspect, possibly null
     * @return the snapshot, or null when none is present
     */
    static GameSnapshot snapshotOf(GameOperationResult result) {
        if (result == null) {
            return null;
        }
        if (result.type() == ResultType.SNAPSHOT) {
            return ((Snapshot) result).snapshot();
        }
        if (result.type() == ResultType.ERROR) {
            return ((Err) result).snapshot();
        }
        return null;
    }

    /**
     * Extracts the lobby snapshot carried by a result, if any. Both
     * {@link Lobby} (success) and {@link Err} (failure with current state) can
     * carry one.
     *
     * @param result result to inspect, possibly null
     * @return the lobby snapshot, or null when none is present
     */
    static LobbySnapshot lobbyOf(GameOperationResult result) {
        if (result == null) {
            return null;
        }
        if (result.type() == ResultType.LOBBY) {
            return ((Lobby) result).lobby();
        }
        if (result.type() == ResultType.ERROR) {
            return ((Err) result).lobby();
        }
        return null;
    }

    /**
     * Successful result carrying a game snapshot.
     *
     * @param message result message
     * @param snapshot game snapshot
     */
    record Snapshot(String message, GameSnapshot snapshot) implements GameOperationResult {
        @Override
        public ResultType type() {
            return ResultType.SNAPSHOT;
        }
    }

    /**
     * Successful result carrying a lobby snapshot.
     *
     * @param message result message
     * @param lobby lobby snapshot
     */
    record Lobby(String message, LobbySnapshot lobby) implements GameOperationResult {
        @Override
        public ResultType type() {
            return ResultType.LOBBY;
        }
    }

    /**
     * Successful result carrying leaderboard rankings.
     *
     * @param message result message
     * @param rankings list of ranking entries
     */
    record Rankings(String message, List<LeaderboardEntry> rankings) implements GameOperationResult {
        @Override
        public ResultType type() {
            return ResultType.RANKINGS;
        }
    }

    /**
     * Successful nickname result.
     *
     * @param message result message
     * @param rejoinAvailable true when a disconnected session can be rejoined
     */
    record Nickname(String message, boolean rejoinAvailable) implements GameOperationResult {
        @Override
        public ResultType type() {
            return ResultType.NICKNAME;
        }
    }

    /**
     * Successful result with no payload (heartbeat, pass, no-op acknowledgements).
     *
     * @param message result message
     */
    record SimpleOk(String message) implements GameOperationResult {
        @Override
        public ResultType type() {
            return ResultType.SIMPLE_OK;
        }
    }

    /**
     * Failure result. May carry the current game or lobby state alongside the
     * error code. Normalizes a blank message and a null error code.
     *
     * @param message result message, normalized to "Operation failed" when blank
     * @param errorCode structured error code, normalized to
     *                  {@link OperationErrorCode#NONE} when null
     * @param snapshot optional game snapshot describing current state
     * @param lobby optional lobby snapshot describing current state
     */
    record Err(String message, OperationErrorCode errorCode, GameSnapshot snapshot, LobbySnapshot lobby)
            implements GameOperationResult {
        public Err {
            if (message == null || message.isBlank()) {
                message = "Operation failed";
            }
            errorCode = errorCode == null ? OperationErrorCode.NONE : errorCode;
        }

        @Override
        public ResultType type() {
            return ResultType.ERROR;
        }
    }
}
