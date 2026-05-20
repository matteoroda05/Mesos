package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Locale;

public final class MesosUserMessageMapper {
    private MesosUserMessageMapper() {
    }

    public static String describeResult(GameOperationResult result, String successFallback) {
        if (result == null) {
            return "Operation completed.";
        }
        if (result.success()) {
            return successFallback;
        }

        String mappedError = describeErrorCode(result.errorCode());
        if (mappedError != null) {
            return mappedError;
        }
        return translateFreeform(result.message());
    }

    public static String describeUpdate(GameViewUpdate update) {
        if (update == null) {
            return "Board updated.";
        }
        return translateFreeform(update.message());
    }

    public static String describeException(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ConnectException || cause instanceof java.rmi.ConnectException) {
            return "Could not reach the MESOS server. Check the host and make sure the server is running.";
        }
        if (cause instanceof IOException ioException) {
            String message = ioException.getMessage();
            if (message == null || message.isBlank()) {
                return "A network error interrupted the connection to the MESOS server.";
            }
            return "Network error: " + message;
        }

        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return translateFreeform(message);
    }

    public static String describePhase(GameStatus status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case LOADING -> "Loading";
            case LOGIN -> "Waiting Room";
            case SETUP -> "Setup";
            case ROUND_OFFER -> "Offer Selection";
            case ROUND_ACTIONS -> "Card Selection";
            case ROUND_EVENTS -> "Round Events";
            case ROUND_END -> "Round End";
            case FINAL_EVENTS -> "Final Events";
            case END_GAME, END_SCREEN -> "Game Over";
        };
    }

    private static String describeErrorCode(OperationErrorCode errorCode) {
        if (errorCode == null) {
            return null;
        }
        return switch (errorCode) {
            case GAME_NAME_TAKEN -> "A game with this name already exists.";
            case GAME_NOT_FOUND -> "The selected game does not exist anymore.";
            case GAME_FULL -> "The selected game is already full.";
            case PLAYER_NAME_TAKEN -> "This nickname is already in use.";
            case PLAYER_NOT_IN_GAME -> "This player is not registered in the selected game.";
            case COLOR_ALREADY_TAKEN -> "That color has already been chosen by another player.";
            case COLOR_SELECTION_CLOSED -> "Color selection is closed because the game has already started.";
            case INVALID_LISTENER -> "Live updates could not be enabled. You can still use manual refresh.";
            case NICKNAME_DISCONNECTED -> "This nickname belongs to a disconnected session.";
            case NONE -> null;
        };
    }

    private static String translateFreeform(String message) {
        if (message == null || message.isBlank()) {
            return "Operation completed.";
        }

        String normalized = message
                .replace("(dumbass)", "")
                .replace("  ", " ")
                .trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.contains("wrong turn")) {
            return "It is not your turn.";
        }
        if (lower.contains("wrong row")) {
            return "You cannot pick from that row right now.";
        }
        if (lower.contains("wrong game state")) {
            return "This action is not available in the current phase.";
        }
        if (lower.contains("cannot take event card")) {
            return "Event cards cannot be selected.";
        }
        if (lower.contains("out of bounds")) {
            return "The selected card or offer tile is no longer available.";
        }
        if (lower.contains("already occupied")) {
            return "That offer tile is already occupied.";
        }
        if (lower.contains("lobby aggiornata")) {
            return "Lobby refreshed.";
        }
        if (lower.contains("partita creata")) {
            return "Game created.";
        }
        if (lower.contains("snapshot aggiornato")) {
            return "Board refreshed.";
        }
        if (lower.contains("tessera offerta scelta")) {
            return "Offer tile selected.";
        }
        if (lower.contains("carta scelta")) {
            return "Card selected.";
        }
        if (lower.contains("colore riservato") && lower.contains("partita inizia")) {
            return "Color selected. The match is starting.";
        }
        if (lower.contains("colore riservato")) {
            return "Color selected.";
        }
        if (lower.contains("iscrizione agli aggiornamenti attiva")) {
            return "Live updates enabled.";
        }
        if (lower.contains("iscrizione agli aggiornamenti disattivata")) {
            return "Live updates disabled.";
        }
        if (lower.contains("giocatore entrato: la partita inizia")) {
            return "All players joined. The match is starting.";
        }
        if (lower.contains("giocatore entrato nella lobby")) {
            return "A player joined the lobby.";
        }
        if (lower.contains("la partita non e' ancora iniziata")) {
            return "The game has not started yet.";
        }
        if (lower.contains("connessione socket chiusa")) {
            return "The socket connection was closed.";
        }

        return normalized;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && current instanceof RuntimeException
                && !(current instanceof IOException)) {
            current = current.getCause();
        }
        return current;
    }
}
