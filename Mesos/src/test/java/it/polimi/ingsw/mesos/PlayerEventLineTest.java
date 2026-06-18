package it.polimi.ingsw.mesos;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerEventLineTest {

    @Test
    void describeLine_foodAndPrestigeChanges_describesBothDeltas() {
        assertEquals(
                "Player Alice (BLUE) gained 2 food and lost 3 prestige points",
                new PlayerEventLine("Alice", Color.BLUE, 2, -3).describeLine());
    }

    @Test
    void describeLine_noChanges_returnsPlayerPrefixWithEmptyChange() {
        assertEquals("Player Bob (ORANGE) ", new PlayerEventLine("Bob", Color.ORANGE, 0, 0).describeLine());
    }
}
