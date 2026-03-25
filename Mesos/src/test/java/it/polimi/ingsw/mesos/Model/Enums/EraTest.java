package it.polimi.ingsw.mesos.Model.Enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EraTest {

    @Test
    void nextReturnsFollowingEra() {
        assertEquals(Era.SECOND, Era.FIRST.next());
        assertEquals(Era.THIRD, Era.SECOND.next());
    }

    @Test
    void nextThrowsWhenCalledOnLastEra() {
        assertThrows(IllegalStateException.class, Era.THIRD::next);
    }
}
