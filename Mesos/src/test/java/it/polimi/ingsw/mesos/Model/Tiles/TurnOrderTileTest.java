package it.polimi.ingsw.mesos.Model.Tiles;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TurnOrderTileTest {

    @Test
    void constructorCreatesExpectedSpacesAndBonuses() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 1, 0, -1));

        List<TurnOrderSpace> spaces = tile.getSpaces();

        assertEquals(4, spaces.size());
        assertEquals(2, spaces.get(0).getBonus());
        assertEquals(1, spaces.get(1).getBonus());
        assertEquals(0, spaces.get(2).getBonus());
        assertEquals(-1, spaces.get(3).getBonus());
        assertTrue(spaces.stream().allMatch(TurnOrderSpace::isFree));
    }

    @Test
    void constructorAcceptsTwoSpacesConfiguration() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));

        assertEquals(2, tile.getSpaces().size());
        assertEquals(1, tile.getSpaces().get(0).getBonus());
        assertEquals(-1, tile.getSpaces().get(1).getBonus());
        assertFalse(tile.isFull());
    }

    @Test
    void constructorAcceptsFiveSpacesConfiguration() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(3, 1, 0, 0, -1));

        assertEquals(5, tile.getSpaces().size());
        assertEquals(3, tile.getSpaces().get(0).getBonus());
        assertEquals(1, tile.getSpaces().get(1).getBonus());
        assertEquals(0, tile.getSpaces().get(2).getBonus());
        assertEquals(0, tile.getSpaces().get(3).getBonus());
        assertEquals(-1, tile.getSpaces().get(4).getBonus());
        assertFalse(tile.isFull());
    }

    @Test
    void currentOrderIsEmptyWhenNoTotemsHaveBeenPlaced() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 0, -1));

        assertTrue(tile.currentOrderTopToBottom().isEmpty());
    }

    @Test
    void placeTotemThrowsExceptionWhenPlayerIsNull() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));

        assertThrows(IllegalArgumentException.class,
                () -> tile.placeTotem(null));
    }

    @Test
    void placeTotemOccupiesFirstFreeSpacesInOrderAndAppliesBonuses() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 1, 0, -1));
        Player firstPlayer = new Player("Alice", Color.BLUE, 2);
        Player secondPlayer = new Player("Bob", Color.YELLOW, 2);
        Player thirdPlayer = new Player("Carol", Color.ORANGE, 2);

        tile.placeTotem(firstPlayer);
        tile.placeTotem(secondPlayer);
        tile.placeTotem(thirdPlayer);

        assertEquals(Color.BLUE, tile.getSpaces().get(0).getOccupant());
        assertEquals(Color.YELLOW, tile.getSpaces().get(1).getOccupant());
        assertEquals(Color.ORANGE, tile.getSpaces().get(2).getOccupant());
        assertNull(tile.getSpaces().get(3).getOccupant());
        assertEquals(4, firstPlayer.getFood());
        assertEquals(3, secondPlayer.getFood());
        assertEquals(2, thirdPlayer.getFood());
        assertEquals(List.of(Color.BLUE, Color.YELLOW, Color.ORANGE), tile.currentOrderTopToBottom());
    }

    @Test
    void placeTotemOnLastSpaceWithEnoughFoodRemovesOneFoodWithoutLosingPrestige() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 0, -1));
        Player firstPlayer = new Player("Alice", Color.BLUE, 0);
        Player secondPlayer = new Player("Bob", Color.YELLOW, 0);
        Player thirdPlayer = new Player("Carol", Color.ORANGE, 2);

        tile.placeTotem(firstPlayer);
        tile.placeTotem(secondPlayer);
        tile.placeTotem(thirdPlayer);

        assertEquals(1, thirdPlayer.getFood());
        assertEquals(0, thirdPlayer.getPrestigePoints());
        assertEquals(Color.ORANGE, tile.getSpaces().get(2).getOccupant());
    }

    @Test
    void placeTotemOnLastSpaceWithInsufficientFoodLosesPrestigeWithoutGoingBelowZeroFood() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 0, -1));
        Player firstPlayer = new Player("Alice", Color.BLUE, 0);
        Player secondPlayer = new Player("Bob", Color.YELLOW, 0);
        Player thirdPlayer = new Player("Carol", Color.ORANGE, 0);

        tile.placeTotem(firstPlayer);
        tile.placeTotem(secondPlayer);
        tile.placeTotem(thirdPlayer);

        assertEquals(0, thirdPlayer.getFood());
        assertEquals(-2, thirdPlayer.getPrestigePoints());
        assertEquals(Color.ORANGE, tile.getSpaces().get(2).getOccupant());
    }

    @Test
    void isFullReturnsTrueOnlyWhenAllSpacesAreOccupied() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));
        Player firstPlayer = new Player("Alice", Color.BLUE, 0);
        Player secondPlayer = new Player("Bob", Color.YELLOW, 0);

        assertFalse(tile.isFull());

        tile.placeTotem(firstPlayer);
        assertFalse(tile.isFull());

        tile.placeTotem(secondPlayer);
        assertTrue(tile.isFull());
    }

    @Test
    void placeTotemThrowsExceptionWhenTileIsFull() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));

        tile.placeTotem(new Player("Alice", Color.BLUE, 0));
        tile.placeTotem(new Player("Bob", Color.YELLOW, 0));

        assertThrows(IllegalStateException.class,
                () -> tile.placeTotem(new Player("Carol", Color.ORANGE, 0)));
    }

    @Test
    void currentOrderContainsAllPlayersWhenEverySpaceIsOccupied() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 1, 0, -1));

        tile.placeTotem(new Player("Alice", Color.BLUE, 0));
        tile.placeTotem(new Player("Bob", Color.YELLOW, 0));
        tile.placeTotem(new Player("Carol", Color.ORANGE, 0));
        tile.placeTotem(new Player("Dave", Color.WHITE, 0));

        assertEquals(List.of(Color.BLUE, Color.YELLOW, Color.ORANGE, Color.WHITE), tile.currentOrderTopToBottom());
        assertTrue(tile.isFull());
    }

    @Test
    void clearRemovesAllOccupantsAndResetsState() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 0, -1));

        tile.placeTotem(new Player("Alice", Color.BLUE, 0));
        tile.placeTotem(new Player("Bob", Color.YELLOW, 0));

        tile.clear();

        assertTrue(tile.getSpaces().stream().allMatch(TurnOrderSpace::isFree));
        assertTrue(tile.currentOrderTopToBottom().isEmpty());
        assertFalse(tile.isFull());
    }

    private List<TurnOrderSpace> createSpaces(int... bonuses) {
        return java.util.Arrays.stream(bonuses)
                .mapToObj(TurnOrderSpace::new)
                .toList();
    }
}