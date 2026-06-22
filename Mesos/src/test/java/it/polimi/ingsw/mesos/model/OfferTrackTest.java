package it.polimi.ingsw.mesos.model;

import it.polimi.ingsw.mesos.common.Globals;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.tiles.OfferTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferTrackTest {

    private OfferTrack offer_track;
    private OfferTile tile_a;
    private OfferTile tile_b;
    private OfferTile tile_c;
    private OfferTile tile_d;
    private OfferTile tile_e;
    private OfferTile tile_f;
    private OfferTile tile_g;

    @BeforeEach
    void setUp() {
        tile_a = new OfferTile('A', 0, 0, 3, 5);
        tile_b = new OfferTile('B', 0, 1, 0, 2);
        tile_c = new OfferTile('C', 1, 0, 0, 2);
        tile_d = new OfferTile('D', 0, 2, 0, 3);
        tile_e = new OfferTile('E', 1, 1, 0, 2);
        tile_f = new OfferTile('F', 2, 0, 0, 2);
        tile_g = new OfferTile('G', 2, 1, 0, 4);

        offer_track = new OfferTrack(List.of(tile_a, tile_b, tile_c, tile_d, tile_e, tile_f, tile_g), 5);
    }

    @Test
    void constructorThrowsExceptionWhenPlayersBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTrack(List.of(tile_b, tile_c, tile_e, tile_f), Globals.MIN_NUM_PLAYERS - 1));
    }

    @Test
    void constructorThrowsExceptionWhenPlayersAboveMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTrack(List.of(tile_a, tile_b, tile_c, tile_d), Globals.MAX_NUM_PLAYERS + 1));
    }

    @Test
    void constructorThrowsExceptionWhenNumberOfTilesIsNotPlayersPlusTwo() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTrack(List.of(tile_a, tile_b, tile_c), 2));
    }

    @Test
    void constructorThrowsExceptionWhenTrackContainsNullTiles() {
        List <OfferTile> list_with_null = new ArrayList<>();
        list_with_null.add(tile_a);
        list_with_null.add(null);
        list_with_null.add(tile_d);
        list_with_null.add(tile_e);
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTrack(list_with_null, 2));
    }

    @Test
    void constructorThrowsExceptionWhenTrackContainsDuplicateTiles() {
        OfferTile dup_b = new OfferTile('B', 1, 1, 1, 2);
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTrack(List.of(dup_b, tile_b, tile_c, tile_e), 2));
    }

    @Test
    void constructorThrowsExceptionWhenTilesAndNumPlayersDontMatch() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTrack(List.of(tile_a, tile_b, tile_c, tile_d, tile_e), 3));
    }

    @Test
    void constructorThrowsExceptionWhenTilesIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTrack(null, 3));
    }

    @Test
    void getAvailableTilesReturnsOnlyAvailableTiles() {
        tile_b.occupy(Color.BLUE);
        tile_d.occupy(Color.YELLOW);
        tile_e.occupy(Color.WHITE);

        assertIterableEquals(List.of(tile_a, tile_c, tile_f, tile_g), offer_track.getAvailableTiles());
    }

    @Test
    void getAvailableTilesThrowsWhenNoTileIsAvailable() {
        tile_a.occupy(Color.BLUE);
        tile_b.occupy(Color.YELLOW);
        tile_c.occupy(Color.WHITE);
        tile_d.occupy(Color.BLACK);
        tile_e.occupy(Color.ORANGE);
        tile_f.occupy(Color.BLUE);
        tile_g.occupy(Color.YELLOW);

        assertThrows(IllegalStateException.class, () -> offer_track.getAvailableTiles());
    }

    @Test
    void getOccupiedTilesReturnsOnlyOccupiedTiles() {
        tile_a.occupy(Color.BLUE);
        tile_c.occupy(Color.YELLOW);

        assertIterableEquals(List.of(tile_a, tile_c), offer_track.getOccupiedTiles());
    }

    @Test
    void getOccupiedTilesReturnsEmptyListWhenNoTileIsOccupied() {
        assertEquals(List.of(), offer_track.getOccupiedTiles());
    }

    @Test
    void clearOccupantByColorClearsOnlyMatchingTile() {
        tile_b.occupy(Color.BLUE);
        tile_d.occupy(Color.YELLOW);

        offer_track.clearOccupantByColor(Color.BLUE);

        assertTrue(tile_b.isAvailable());
        assertEquals(Color.YELLOW, tile_d.getOccupant());
        assertEquals(List.of(tile_d), offer_track.getOccupiedTiles());
    }

    @Test
    void getTileReturnsTileAtRequestedIndex() {
        assertSame(tile_c, offer_track.getTile(2));
    }

    @Test
    void getTileThrowsExceptionWhenIndexIsNegative() {
        assertThrows(IndexOutOfBoundsException.class, () -> offer_track.getTile(-1));
    }

    @Test
    void getTileThrowsExceptionWhenIndexIsOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> offer_track.getTile(7));
    }
}
