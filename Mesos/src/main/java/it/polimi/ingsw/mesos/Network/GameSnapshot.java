package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;
import java.util.List;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;

/**
 * Serializable immutable view of the current game state.
 *
 * @param gameName game name
 * @param playerCount configured number of players
 * @param joinedPlayers number of players currently joined
 * @param started true after the controller has been created
 * @param finished true when the game is complete or a resilience winner exists
 * @param status current game status
 * @param currentRound current round number
 * @param currentEra current era, when available
 * @param currentPlayerNickname active player's nickname, or null when no turn
 *                              is active
 * @param currentPlayerColor active player's color, or null when no turn is
 *                           active
 * @param remainingTopChoices remaining choices from the top row
 * @param remainingBottomChoices remaining choices from the bottom row
 * @param reservedColors colors already reserved in this game before start,
 *                       normalized to an immutable empty list when null
 * @param players player summaries, normalized to an immutable empty list when
 *                null
 * @param offerTiles offer tile summaries, normalized to an immutable empty list
 *                   when null
 * @param turnOrder turn order summaries, normalized to an immutable empty list
 *                  when null
 * @param topRow top market row summaries, normalized to an immutable empty list
 *               when null
 * @param bottomRow bottom market row summaries, normalized to an immutable
 *                  empty list when null
 */
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
        List<Color> reservedColors,
        List<PlayerSummary> players,
        List<OfferTileSummary> offerTiles,
        List<TurnOrderSpaceSummary> turnOrder,
        List<CardSummary> topRow,
        List<CardSummary> bottomRow
) implements Serializable {

    /**
     * Validates required snapshot fields and defensively copies list fields.
     *
     * @throws IllegalArgumentException if game name is null/blank, player count
     *                                  is not positive, joined players is
     *                                  outside {@code [0, playerCount]}, status
     *                                  is null, or current round is negative
     */
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
        reservedColors = reservedColors == null ? List.of() : List.copyOf(reservedColors);
        players = players == null ? List.of() : List.copyOf(players);
        offerTiles = offerTiles == null ? List.of() : List.copyOf(offerTiles);
        turnOrder = turnOrder == null ? List.of() : List.copyOf(turnOrder);
        topRow = topRow == null ? List.of() : List.copyOf(topRow);
        bottomRow = bottomRow == null ? List.of() : List.copyOf(bottomRow);
    }
}
