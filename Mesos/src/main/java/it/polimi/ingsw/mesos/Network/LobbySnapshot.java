package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

public record LobbySnapshot(
        List<LobbyGameSummary> openGames
) implements Serializable {

    public LobbySnapshot {
        openGames = openGames == null
                ? List.of()
                : openGames.stream()
                .sorted(Comparator.comparingInt(LobbyGameSummary::playerCount)
                        .thenComparing(LobbyGameSummary::gameName))
                .toList();
    }
}
