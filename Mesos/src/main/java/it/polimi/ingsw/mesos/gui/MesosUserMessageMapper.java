package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;

import java.io.IOException;
import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MesosUserMessageMapper {
    private static final Map<String, String> TRANSLATIONS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("giocatore entrato: la partita inizia", "All players joined. The match is starting."),
            Map.entry("colore riservato", "Color selected."),
            Map.entry("wrong turn", "It is not your turn."),
            Map.entry("wrong row", "You cannot pick from that row right now."),
            Map.entry("wrong game state", "This action is not available in the current phase."),
            Map.entry("cannot take event card", "Event cards cannot be selected."),
            Map.entry("out of bounds", "The selected card or offer tile is no longer available."),
            Map.entry("already occupied", "That offer tile is already occupied."),
            Map.entry("lobby aggiornata", "Lobby refreshed."),
            Map.entry("partita creata", "Game created."),
            Map.entry("snapshot aggiornato", "Board refreshed."),
            Map.entry("tessera offerta scelta", "Offer tile selected."),
            Map.entry("carta scelta", "Card selected."),
            Map.entry("carta extra saltata", "Optional extra pick skipped."),
            Map.entry("iscrizione agli aggiornamenti attiva", "Live updates enabled."),
            Map.entry("iscrizione agli aggiornamenti disattivata", "Live updates disabled."),
            Map.entry("giocatore entrato nella lobby", "A player joined the lobby."),
            Map.entry("la partita non e' ancora iniziata", "The game has not started yet."),
            Map.entry("connessione socket chiusa", "The socket connection was closed.")
    ));

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
                .replace("  ", " ")
                .trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        return TRANSLATIONS.entrySet().stream()
                .filter(entry -> lower.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(lower);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && current instanceof RuntimeException) {
            current = current.getCause();
        }
        return current;
    }
}
