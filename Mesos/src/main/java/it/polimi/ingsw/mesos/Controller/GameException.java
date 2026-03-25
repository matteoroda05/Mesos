package it.polimi.ingsw.mesos.Controller;

/**
 * Runtime exception used for failures that are part of the game domain,
 * such as illegal player actions or impossible game-flow states.
 */
public class GameException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameException(Throwable cause) {
        super(cause);
    }
}
