package it.polimi.ingsw.mesos.model.tiles;

import it.polimi.ingsw.mesos.model.enums.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OfferTileTest {

    @Test
    void constructorStoresConfiguredValuesAndStartsAvailable() {
        OfferTile tile = new OfferTile('G', 2, 0, 3, 5);

        assertEquals('G', tile.getOfferTileId());
        assertEquals(2, tile.getTopPicks());
        assertEquals(0, tile.getBottomPicks());
        assertEquals(3, tile.getFoodGain());
        assertEquals(5, tile.getMinNumPlayers());
        assertNull(tile.getOccupant());
        assertTrue(tile.isAvailable());
    }

    @Test
    void constructorRejectsOfferIdOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('@', 1, 1, 1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('H', 1, 1, 1, 2));
    }

    @Test
    void constructorRejectsTopPicksOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', -1, 1, 1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', 3, 1, 1, 2));
    }

    @Test
    void constructorRejectsBottomPicksOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', 1, -1, 1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', 1, 3, 1, 2));
    }

    @Test
    void constructorRejectsFoodGainOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', 1, 1, -1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', 1, 1, 4, 2));
    }

    @Test
    void constructorRejectsMinPlayersOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTile('A', 1, 1, 1, 6));
    }

    @Test
    void occupyMarksTileAsUnavailableAndStoresOccupant() {
        OfferTile tile = createTile();

        tile.occupy(Color.BLUE);

        assertEquals(Color.BLUE, tile.getOccupant());
        assertFalse(tile.isAvailable());
    }

    @Test
    void occupyRejectsNullOccupant() {
        OfferTile tile = createTile();

        assertThrows(IllegalArgumentException.class,
                () -> tile.occupy(null));
    }

    @Test
    void occupyRejectsSecondOccupantWhenTileIsAlreadyTaken() {
        OfferTile tile = createTile();
        tile.occupy(Color.BLUE);

        assertThrows(IllegalStateException.class,
                () -> tile.occupy(Color.YELLOW));
    }

    @Test
    void clearReturnsPreviousOccupantAndMakesTileAvailableAgain() {
        OfferTile tile = createTile();
        tile.occupy(Color.ORANGE);

        Color clearedOccupant = tile.clear();

        assertEquals(Color.ORANGE, clearedOccupant);
        assertNull(tile.getOccupant());
        assertTrue(tile.isAvailable());
    }

    private OfferTile createTile() {
        return new OfferTile('E', 1, 1, 0, 2);
    }
}
