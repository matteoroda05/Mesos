package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TurnOrderTileTest {

	@Test
	void constructorThrowsExceptionWhenPlayersBelowMinimum() {
		// Verifies that the tile rejects a number of players smaller than the allowed minimum.
		assertThrows(IllegalArgumentException.class,
				() -> new TurnOrderTile(Globals.MIN_NUM_PLAYERS - 1));
	}

	@Test
	void constructorThrowsExceptionWhenPlayersAboveMaximum() {
		// Verifies that the tile rejects a number of players greater than the allowed maximum.
		assertThrows(IllegalArgumentException.class,
				() -> new TurnOrderTile(Globals.MAX_NUM_PLAYERS + 1));
	}

	@Test
	void constructorCreatesExpectedSpacesAndBonuses() {
		// Verifies that the 4-player tile creates spaces with bonuses 2, 1, 0, -1 in order.
		TurnOrderTile tile = new TurnOrderTile(4);

		List<TurnOrderSpace> spaces = tile.getSpaces();

		assertEquals(4, spaces.size());
		assertEquals(2, spaces.get(0).getBonus());
		assertEquals(1, spaces.get(1).getBonus());
		assertEquals(0, spaces.get(2).getBonus());
		assertEquals(-1, spaces.get(3).getBonus());
		assertTrue(spaces.stream().allMatch(TurnOrderSpace::isFree));
	}

	@Test
	void constructorAcceptsMinimumNumberOfPlayers() {
		// Verifies that the minimum valid number of players creates a 2-space tile with bonuses 1 and -1.
		TurnOrderTile tile = new TurnOrderTile(Globals.MIN_NUM_PLAYERS);

		assertEquals(Globals.MIN_NUM_PLAYERS, tile.getSpaces().size());
		assertEquals(1, tile.getSpaces().get(0).getBonus());
		assertEquals(-1, tile.getSpaces().get(1).getBonus());
		assertFalse(tile.isFull());
	}

	@Test
	void constructorAcceptsMaximumNumberOfPlayers() {
		// Verifies that the maximum valid number of players creates a 5-space tile with bonuses 3, 1, 0, 0, -1.
		TurnOrderTile tile = new TurnOrderTile(Globals.MAX_NUM_PLAYERS);

		assertEquals(Globals.MAX_NUM_PLAYERS, tile.getSpaces().size());
		assertEquals(3, tile.getSpaces().get(0).getBonus());
		assertEquals(1, tile.getSpaces().get(1).getBonus());
		assertEquals(0, tile.getSpaces().get(2).getBonus());
		assertEquals(0, tile.getSpaces().get(3).getBonus());
		assertEquals(-1, tile.getSpaces().get(4).getBonus());
		assertFalse(tile.isFull());
	}

	@Test
	void currentOrderIsEmptyWhenNoTotemsHaveBeenPlaced() {
		// Verifies that the current order starts empty before any player is placed.
		TurnOrderTile tile = new TurnOrderTile(3);

		assertTrue(tile.currentOrderTopToBottom().isEmpty());
	}

	@Test
	void placeTotemThrowsExceptionWhenPlayerIsNull() {
		// Verifies that placing a null player is rejected immediately.
		TurnOrderTile tile = new TurnOrderTile(2);

		assertThrows(IllegalArgumentException.class,
				() -> tile.placeTotem(null));
	}

	@Test
	void placeTotemOccupiesFirstFreeSpacesInOrderAndAppliesBonuses() {
		// Verifies that players are placed from top to bottom and receive the 4-player bonuses 2, 1 and 0.
		TurnOrderTile tile = new TurnOrderTile(4);
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
		// Verifies that on the natural last space with a -1 bonus the player loses 1 food and no prestige when food is sufficient.
		TurnOrderTile tile = new TurnOrderTile(3);
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
		// Verifies that on the natural last space with a -1 bonus a player with zero food loses 2 prestige and does not go below zero food.
		TurnOrderTile tile = new TurnOrderTile(3);
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
		// Verifies that the tile reports full only after every available space has been taken.
		TurnOrderTile tile = new TurnOrderTile(2);
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
		// Verifies that no additional player can be placed once every space is occupied.
		TurnOrderTile tile = new TurnOrderTile(2);

		tile.placeTotem(new Player("Alice", Color.BLUE, 0));
		tile.placeTotem(new Player("Bob", Color.YELLOW, 0));

		assertThrows(IllegalStateException.class,
				() -> tile.placeTotem(new Player("Carol", Color.ORANGE, 0)));
	}

	@Test
	void currentOrderContainsAllPlayersWhenEverySpaceIsOccupied() {
		// Verifies that the complete turn order is returned correctly when all spaces are occupied.
		TurnOrderTile tile = new TurnOrderTile(4);

		tile.placeTotem(new Player("Alice", Color.BLUE, 0));
		tile.placeTotem(new Player("Bob", Color.YELLOW, 0));
		tile.placeTotem(new Player("Carol", Color.ORANGE, 0));
		tile.placeTotem(new Player("Dave", Color.WHITE, 0));

		assertEquals(List.of(Color.BLUE, Color.YELLOW, Color.ORANGE, Color.WHITE), tile.currentOrderTopToBottom());
		assertTrue(tile.isFull());
	}

	@Test
	void clearRemovesAllOccupantsAndResetsState() {
		// Verifies that clearing the tile frees every space and resets the visible turn order.
		TurnOrderTile tile = new TurnOrderTile(3);

		tile.placeTotem(new Player("Alice", Color.BLUE, 0));
		tile.placeTotem(new Player("Bob", Color.YELLOW, 0));

		tile.clear();

		assertTrue(tile.getSpaces().stream().allMatch(TurnOrderSpace::isFree));
		assertTrue(tile.currentOrderTopToBottom().isEmpty());
		assertFalse(tile.isFull());
	}
}
