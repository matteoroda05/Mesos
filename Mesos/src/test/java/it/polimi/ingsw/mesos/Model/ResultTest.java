package it.polimi.ingsw.mesos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void negativePenalty() {
        assertThrows(IllegalArgumentException.class, () -> new Result(0, 0, -1));
    }

    @Test
    void positiveFoodWithPenalty() {
        assertThrows(IllegalArgumentException.class, () -> new Result(1, 0, 1));
    }

    @Test
    void negativeFoodWithoutPenalty() {
        assertThrows(IllegalArgumentException.class, () -> new Result(-1, 0, 0));
    }
}
