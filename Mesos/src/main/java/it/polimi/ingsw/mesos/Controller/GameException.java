package it.polimi.ingsw.mesos.Controller;

/**
 * Runtime exception used for failures that are part of the game domain,
 * such as illegal player actions or impossible game-flow states.
 */
public class GameException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a game exception with a detail message.
     *
     * @param message detail message, which may be null
     */
    public GameException(String message) {
        super(message);
    }

    /**
     * Creates a game exception with a detail message and cause.
     *
     * @param message detail message, which may be null
     * @param cause underlying cause, which may be null
     */
    public GameException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a game exception with a cause.
     *
     * @param cause underlying cause, which may be null
     */
    public GameException(Throwable cause) {
        super(cause);
    }
}
