package it.polimi.ingsw.mesos.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Color;

class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Nicola", Color.YELLOW,3);
    }

    @Test
    void constructorInitializesPlayerCorrectly() {
        assertEquals("Nicola", player.getNickname());
        assertEquals(Color.YELLOW,player.getColor());
        assertEquals(3, player.getFood());
        assertEquals(0, player.getPrestigePoints());
        assertNotNull(player.getTribe());
    }

    @Test
    void differentPlayersHaveDifferentTribes() {
        Player player1 = new Player("Matteo", Color.BLUE, 3);
        Player player2 = new Player("Elia", Color.YELLOW, 3);

        assertNotSame(player1.getTribe(), player2.getTribe());
    }

    @Test
    void constructorThrowsExceptionWhenInitialFoodIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("Mathis", Color.BLUE, -1));
    }

    @Test
    void gainFoodIncreasesFoodCorrectly() {
        player.gainFood(2);
        assertEquals(5, player.getFood());
    }

    @Test
    void gainFoodThrowsExceptionWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> player.gainFood(-1));
    }

    @Test
    void loseFoodDecreasesFoodCorrectly() {
        assertEquals(0, player.loseFood(2));
        assertEquals(1, player.getFood());
    }

    @Test
    void loseFoodThrowsExceptionWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> player.loseFood(-2));
    }

    @Test
    void loseFoodReturnsPrestigeLossCorrectly() {
        assertEquals(3, player.loseFood(6));
        assertEquals(0, player.getFood());
    }

    @Test
    void changePrestigePointsUpdatesPrestigeCorrectly() {
        player.changePrestigePoints(5);
        player.changePrestigePoints(-2);
        assertEquals(3, player.getPrestigePoints());
    }

    @Test
    void getTribeReturnsSameInstanceEveryTime() {
        Tribe firstCall = player.getTribe();
        Tribe secondCall = player.getTribe();
        assertSame(firstCall, secondCall);
    }

}