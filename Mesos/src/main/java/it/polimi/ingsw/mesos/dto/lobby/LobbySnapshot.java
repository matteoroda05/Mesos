package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * Serializable snapshot of lobby games.
 *
 * @param openGames open games, normalized to an empty list when null and sorted
 *                  by player count, then game name
 */
public record LobbySnapshot(
        List<LobbyGameSummary> openGames
) implements Serializable {

    /**
     * Normalizes and sorts lobby games for deterministic client display.
     * A null input is treated as an empty lobby.
     */
    public LobbySnapshot {
        openGames = openGames == null
                ? List.of()
                : openGames.stream()
                .sorted(Comparator.comparingInt(LobbyGameSummary::playerCount)
                        .thenComparing(LobbyGameSummary::gameName))
                .toList();
    }
}
