package it.polimi.ingsw.mesos.Database;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

public record LeaderboardEntry(String nickname, int score, Timestamp matchDate,
                               int numPlayers) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return nickname + " - Points: " + score + " (" + matchDate.toString() + ")";
    }
}