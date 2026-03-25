package it.polimi.ingsw.mesos.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class GameExceptionTest {

    @Test
    void messageConstructorStoresMessage() {
        GameException exception = new GameException("invalid move");

        assertEquals("invalid move", exception.getMessage());
    }

    @Test
    void messageAndCauseConstructorStoresBothValues() {
        Throwable cause = new IllegalStateException("root cause");
        GameException exception = new GameException("invalid state", cause);

        assertEquals("invalid state", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void causeConstructorStoresCause() {
        Throwable cause = new IllegalArgumentException("bad input");
        GameException exception = new GameException(cause);

        assertSame(cause, exception.getCause());
    }
}
