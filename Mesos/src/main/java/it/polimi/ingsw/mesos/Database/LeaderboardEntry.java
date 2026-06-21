package it.polimi.ingsw.mesos.database;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Serializable record with match history's data
 * @param rank player's rank
 * @param nickname player's nickname
 * @param score player's score
 * @param matchDate match start date
 * @param numPlayers number of players in that match
 */
public record LeaderboardEntry(int rank, String nickname, int score, Timestamp matchDate,
                               int numPlayers) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Formats this result as a single leaderboard line.
     *
     * @return rank, nickname, score, and match timestamp
     */
    @Override
    public String toString() {
        return rank + "° - " + nickname + " - Points: " + score + " (" + matchDate.toString() + ")";
    }
}