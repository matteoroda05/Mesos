package it.polimi.ingsw.mesos;

import java.util.List;
import java.util.Map;

public class Globals {
    public static final int MAX_NUM_PLAYERS = 5;
    public static final int MIN_NUM_PLAYERS = 2;
    public static final char FIRST_OFFER = 'A';
    public static final char LAST_OFFER = 'G';
    private static final int[] _PLAYER_STARTING_FOOD = { 2, 3, 3, 4, 4}; // private to prevent modification, use getter to access
    private static final Map<Integer, int[]> _BUILDING_COUNT_BY_ERA = Map.of(
            2, new int[]{1, 2, 3},
            3, new int[]{2, 2, 4},
            4, new int[]{2, 3, 4},
            5, new int[]{2, 3, 5}
    );

    public static int[] PLAYER_STARTING_FOOD() {
        return _PLAYER_STARTING_FOOD;
    }
    public static int[] BUILDING_COUNT_BY_ERA(int player_count) {return _BUILDING_COUNT_BY_ERA.get(player_count);}
}