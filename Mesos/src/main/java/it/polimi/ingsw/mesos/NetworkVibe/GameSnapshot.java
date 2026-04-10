package it.polimi.ingsw.mesos.NetworkVibe;

import java.io.Serializable;
import java.util.List;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public record GameSnapshot(
        String gameName,
        int playerCount,
        int joinedPlayers,
        boolean started,
        boolean finished,
        GameStatus status,
        int currentRound,
        Era currentEra,
        String currentPlayerNickname,
        Color currentPlayerColor,
        int remainingTopChoices,
        int remainingBottomChoices,
        List<PlayerSummary> players,
        List<OfferTileSummary> offerTiles,
        List<TurnOrderSpaceSummary> turnOrder,
        List<CardSummary> topRow,
        List<CardSummary> bottomRow
) implements Serializable {

    public GameSnapshot {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (playerCount <= 0) {
            throw new IllegalArgumentException("playerCount must be positive");
        }
        if (joinedPlayers < 0 || joinedPlayers > playerCount) {
            throw new IllegalArgumentException("joinedPlayers must be between 0 and playerCount");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (currentRound < 0) {
            throw new IllegalArgumentException("currentRound cannot be negative");
        }
        players = players == null ? List.of() : List.copyOf(players);
        offerTiles = offerTiles == null ? List.of() : List.copyOf(offerTiles);
        turnOrder = turnOrder == null ? List.of() : List.copyOf(turnOrder);
        topRow = topRow == null ? List.of() : List.copyOf(topRow);
        bottomRow = bottomRow == null ? List.of() : List.copyOf(bottomRow);
    }
}
