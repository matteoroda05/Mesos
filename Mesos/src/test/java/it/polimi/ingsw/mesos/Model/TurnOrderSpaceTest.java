package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TurnOrderSpaceTest {

	@Test
	void constructorSetsBonusAndStartsFreeWithNoOccupant() {
		// Verifies that a new space exposes the configured bonus and starts unoccupied.
		TurnOrderSpace space = new TurnOrderSpace(2);

		assertEquals(2, space.getBonus());
		assertNull(space.getOccupant());
		assertTrue(space.isFree());
	}

	@Test
	void constructorPreservesNegativeBonusValue() {
		// Verifies that a negative malus value is kept as configured.
		TurnOrderSpace space = new TurnOrderSpace(-1);

		assertEquals(-1, space.getBonus());
	}

	@Test
	void setOccupantMarksSpaceAsOccupiedAndStoresPlayerColor() {
		// Verifies that assigning a player color occupies the space.
		TurnOrderSpace space = new TurnOrderSpace(0);

		space.setOccupant(Color.BLUE);

		assertEquals(Color.BLUE, space.getOccupant());
		assertFalse(space.isFree());
	}

	@Test
	void setOccupantCanReplaceAnExistingOccupant() {
		// Verifies that setting a new occupant updates the space to the latest color.
		TurnOrderSpace space = new TurnOrderSpace(1);

		space.setOccupant(Color.BLUE);
		space.setOccupant(Color.YELLOW);

		assertEquals(Color.YELLOW, space.getOccupant());
		assertFalse(space.isFree());
	}

	@Test
	void setOccupantWithNullFreesTheSpace() {
		// Verifies that assigning null clears occupancy consistently.
		TurnOrderSpace space = new TurnOrderSpace(1);
		space.setOccupant(Color.ORANGE);

		space.setOccupant(null);

		assertNull(space.getOccupant());
		assertTrue(space.isFree());
	}

	@Test
	void clearRemovesOccupantAndRestoresFreeState() {
		// Verifies that clear always returns the space to a free state.
		TurnOrderSpace space = new TurnOrderSpace(3);
		space.setOccupant(Color.WHITE);

		space.clear();

		assertNull(space.getOccupant());
		assertTrue(space.isFree());
		assertEquals(3, space.getBonus());
	}
}
