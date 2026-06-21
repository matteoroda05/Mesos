package it.polimi.ingsw.mesos.common;

import it.polimi.ingsw.mesos.model.enums.Era;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Shared game constants and small lookup tables.
 */
public class Globals {
    public static final int TOTAL_ROUNDS = 10;
    public static final int MAX_NUM_PLAYERS = 5;
    public static final int MIN_NUM_PLAYERS = 2;
    public static final char FIRST_OFFER = 'A';
    public static final char LAST_OFFER = 'G';
    private static final int[] _PLAYER_STARTING_FOOD = { 2, 3, 3, 4, 4}; // private to prevent modification, use getter to access
    private static final Map<Integer, Map<Era, Integer>> _BUILDING_COUNT_BY_ERA = Map.of(
            2, eraCounts(1, 2, 3),
            3, eraCounts(2, 2, 4),
            4, eraCounts(2, 3, 4),
            5, eraCounts(2, 3, 5)
    );

    /**
     * Returns the starting-food table indexed by zero-based turn-order seat.
     * The first player in turn order receives the first value, and so on.
     */
    public static int[] PLAYER_STARTING_FOOD() {
        return _PLAYER_STARTING_FOOD;
    }

    /**
     * Returns the number of building cards to select for each era at a given game size.
     *
     * @param player_count number of players in the game
     * @return immutable era-to-card-count map, or {@code null} for an unsupported game size
     */
    public static Map<Era, Integer> BUILDING_COUNT_BY_ERA(int player_count) {
        return _BUILDING_COUNT_BY_ERA.get(player_count);
    }

    /**
     * Builds an immutable count map whose arguments correspond to the first,
     * second, and third eras in that order.
     */
    private static Map<Era, Integer> eraCounts(int first, int second, int third) {
        EnumMap<Era, Integer> counts = new EnumMap<>(Era.class);
        counts.put(Era.FIRST, first);
        counts.put(Era.SECOND, second);
        counts.put(Era.THIRD, third);
        return Collections.unmodifiableMap(counts);
    }
}
