package it.polimi.ingsw.mesos.model.tiles;

import it.polimi.ingsw.mesos.model.enums.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TurnOrderSpaceTest {

	@Test
	void constructorSetsBonusAndStartsFreeWithNoOccupant() {
		TurnOrderSpace space = new TurnOrderSpace(2);

		assertEquals(2, space.getBonus());
		assertNull(space.getOccupant());
		assertTrue(space.isFree());
	}

	@Test
	void constructorPreservesNegativeBonusValue() {
		TurnOrderSpace space = new TurnOrderSpace(-1);

		assertEquals(-1, space.getBonus());
	}

	@Test
	void setOccupantMarksSpaceAsOccupiedAndStoresPlayerColor() {
		TurnOrderSpace space = new TurnOrderSpace(0);

		space.setOccupant(Color.BLUE);

		assertEquals(Color.BLUE, space.getOccupant());
		assertFalse(space.isFree());
	}

	@Test
	void setOccupantCanReplaceAnExistingOccupant() {
		TurnOrderSpace space = new TurnOrderSpace(1);

		space.setOccupant(Color.BLUE);
		space.setOccupant(Color.YELLOW);

		assertEquals(Color.YELLOW, space.getOccupant());
		assertFalse(space.isFree());
	}

	@Test
	void setOccupantWithNullFreesTheSpace() {
		TurnOrderSpace space = new TurnOrderSpace(1);
		space.setOccupant(Color.ORANGE);

		space.setOccupant(null);

		assertNull(space.getOccupant());
		assertTrue(space.isFree());
	}

	@Test
	void clearRemovesOccupantAndRestoresFreeState() {
		TurnOrderSpace space = new TurnOrderSpace(3);
		space.setOccupant(Color.WHITE);

		space.clear();

		assertNull(space.getOccupant());
		assertTrue(space.isFree());
		assertEquals(3, space.getBonus());
	}
}
