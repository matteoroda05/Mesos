package it.polimi.ingsw.mesos.model.tiles;

import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.Player;
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
    void constructorRejectsNullSpaceList() {
        assertThrows(IllegalArgumentException.class, () -> new TurnOrderTile(null));
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
    void setupTotemValidatesPlayerAndUsesFirstFreeSpace() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));
        Player player = new Player("Alice", Color.BLUE, 0);

        assertThrows(IllegalArgumentException.class, () -> tile.setupTotem(null));

        tile.setupTotem(player);

        assertEquals(List.of(Color.BLUE), tile.currentOrderTopToBottom());
        assertEquals(Color.BLUE, tile.getSpaces().getFirst().getOccupant());
    }

    @Test
    void setupTotemThrowsWhenNoFreeSpaceExists() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1));
        tile.setupTotem(new Player("Alice", Color.BLUE, 0));

        assertThrows(IllegalStateException.class,
                () -> tile.setupTotem(new Player("Bob", Color.YELLOW, 0)));
    }

    @Test
    void placeTotemOccupiesFirstFreeSpacesInOrder() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 1, 0, -1));
        Player firstPlayer = new Player("Alice", Color.BLUE, 2);
        Player secondPlayer = new Player("Bob", Color.YELLOW, 2);
        Player thirdPlayer = new Player("Carol", Color.ORANGE, 2);

        tile.placeTotem(firstPlayer.getColor());
        tile.placeTotem(secondPlayer.getColor());
        tile.placeTotem(thirdPlayer.getColor());

        assertEquals(Color.BLUE, tile.getSpaces().get(0).getOccupant());
        assertEquals(Color.YELLOW, tile.getSpaces().get(1).getOccupant());
        assertEquals(Color.ORANGE, tile.getSpaces().get(2).getOccupant());
        assertNull(tile.getSpaces().get(3).getOccupant());
        assertEquals(List.of(Color.BLUE, Color.YELLOW, Color.ORANGE), tile.currentOrderTopToBottom());
    }

    @Test
    void isFullReturnsTrueOnlyWhenAllSpacesAreOccupied() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));
        Player firstPlayer = new Player("Alice", Color.BLUE, 0);
        Player secondPlayer = new Player("Bob", Color.YELLOW, 0);

        assertFalse(tile.isFull());

        tile.placeTotem(firstPlayer.getColor());
        assertFalse(tile.isFull());

        tile.placeTotem(secondPlayer.getColor());
        assertTrue(tile.isFull());
    }

    @Test
    void placeTotemThrowsExceptionWhenTileIsFull() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));

        tile.placeTotem(Color.BLUE);
        tile.placeTotem(Color.YELLOW);

        assertThrows(IllegalStateException.class,
                () -> tile.placeTotem(Color.ORANGE));
    }

    @Test
    void currentOrderContainsAllPlayersWhenEverySpaceIsOccupied() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 1, 0, -1));

        tile.placeTotem(Color.BLUE);
        tile.placeTotem(Color.YELLOW);
        tile.placeTotem(Color.ORANGE);
        tile.placeTotem(Color.WHITE);

        assertEquals(List.of(Color.BLUE, Color.YELLOW, Color.ORANGE, Color.WHITE), tile.currentOrderTopToBottom());
        assertTrue(tile.isFull());
    }

    @Test
    void clearRemovesAllOccupantsAndResetsState() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(2, 0, -1));

        tile.placeTotem(Color.BLUE);
        tile.placeTotem(Color.YELLOW);

        tile.clear();

        assertTrue(tile.getSpaces().stream().allMatch(TurnOrderSpace::isFree));
        assertTrue(tile.currentOrderTopToBottom().isEmpty());
        assertFalse(tile.isFull());
    }

    @Test
    void clearFirstRemovesFirstOccupiedSpaceNotFirstPhysicalSpace() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));

        tile.setupTotem(new Player("Alice", Color.BLUE, 0));
        tile.setupTotem(new Player("Bob", Color.YELLOW, 0));

        tile.clearFirst();
        assertEquals(List.of(Color.YELLOW), tile.currentOrderTopToBottom());

        tile.clearFirst();
        assertTrue(tile.currentOrderTopToBottom().isEmpty());
    }

    @Test
    void clearFirstThrowsWhenNoOccupiedSpaceExists() {
        TurnOrderTile tile = new TurnOrderTile(createSpaces(1, -1));

        assertThrows(IllegalStateException.class, tile::clearFirst);
    }

    private List<TurnOrderSpace> createSpaces(int... bonuses) {
        return java.util.Arrays.stream(bonuses)
                .mapToObj(TurnOrderSpace::new)
                .toList();
    }
}
