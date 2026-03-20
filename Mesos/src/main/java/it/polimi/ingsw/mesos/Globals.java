package it.polimi.ingsw.mesos;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Globals {
    public static final int MAX_NUM_PLAYERS = 5;
    public static final int MIN_NUM_PLAYERS = 2;
    public static final char FIRST_OFFER = 'A';
    public static final char LAST_OFFER = 'G';
    private static final int[] _PLAYER_STARTING_FOOD = { 2, 3, 3, 4, 4}; // private to prevent modification, use getter to access

    public static int[] PLAYER_STARTING_FOOD() {
        return _PLAYER_STARTING_FOOD;
    }
}