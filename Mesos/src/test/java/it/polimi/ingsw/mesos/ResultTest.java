package it.polimi.ingsw.mesos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void validResults() {
        assertDoesNotThrow(() -> new Result(10, 5, 0));
        assertDoesNotThrow(() -> new Result(0, 5, 0));
        assertDoesNotThrow(() -> new Result(-1, 5, 2));
        assertDoesNotThrow(() -> new Result(0, 5, 2));
    }

    @Test
    void negativePenalty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Result(0, 0, -1));
    }

    @Test
    void positiveFoodWithPenalty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Result(1, 0, 1));
    }

    @Test
    void negativeFoodWithoutPenalty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Result(-1, 0, 0));
    }
}
