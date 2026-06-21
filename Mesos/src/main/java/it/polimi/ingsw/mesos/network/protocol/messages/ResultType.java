package it.polimi.ingsw.mesos.network.protocol.messages;

/**
 * Discriminator for the concrete {@link GameOperationResult} variant carried by
 * a server response. Consumers branch on {@link GameOperationResult#type()} and
 * downcast to the matching record to read its payload.
 */
public enum ResultType {
    /** Failure response ({@link GameOperationResult.Err}). */
    ERROR,
    /** Success carrying a game snapshot ({@link GameOperationResult.Snapshot}). */
    SNAPSHOT,
    /** Success with no payload ({@link GameOperationResult.SimpleOk}). */
    SIMPLE_OK,
    /** Success carrying a lobby snapshot ({@link GameOperationResult.Lobby}). */
    LOBBY,
    /** Success carrying leaderboard rankings ({@link GameOperationResult.Rankings}). */
    RANKINGS,
    /** Success of a nickname operation ({@link GameOperationResult.Nickname}). */
    NICKNAME
}
