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

    /**
     * Converts a server operation result into a user-facing status message.
     *
     * @param result server result, possibly null
     * @param successFallback message used for successful operations
     * @return translated error or success message
     */
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

    /**
     * Converts a pushed game update into a user-facing status message.
     *
     * @param update update to describe, possibly null
     * @return translated update message or a generic refresh message
     */
    public static String describeUpdate(GameViewUpdate update) {
        if (update == null) {
            return "Board updated.";
        }
        return translateFreeform(update.message());
    }

    /**
     * Classifies an exception and returns a concise connection or fallback message.
     * Runtime wrappers are unwrapped one level at a time.
     *
     * @param throwable failure to describe
     * @return user-facing failure description
     */
    public static String describeException(Throwable throwable) {
        try {
            throw throwable;
        } catch (ConnectException | java.rmi.ConnectException connectException) {
            return "Could not reach the MESOS server. Check the host and make sure the server is running.";
        } catch (IOException ioException) {
            String message = ioException.getMessage();
            if (message == null || message.isBlank()) {
                return "A network error interrupted the connection to the MESOS server.";
            }
            return "Network error: " + message;
        } catch (RuntimeException runtimeException) {
            // Runtime wrappers (e.g. CompletionException) hide the real cause: peel
            // one layer and classify it the same way.
            Throwable cause = runtimeException.getCause();
            if (cause != null) {
                return describeException(cause);
            }
            return describeFallback(runtimeException);
        } catch (Throwable other) {
            return describeFallback(other);
        }
    }

    /**
     * Reports whether a throwable represents a lost or unreachable network
     * connection, unwrapping runtime wrappers the same way
     * {@link #describeException} does.
     *
     * @param throwable throwable to classify, may be null
     * @return true when the failure is a connection or transport error
     */
    public static boolean isConnectionLost(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        try {
            throw throwable;
        } catch (IOException networkFailure) {
            return true;
        } catch (RuntimeException runtimeException) {
            return runtimeException.getCause() != null
                    && isConnectionLost(runtimeException.getCause());
        } catch (Throwable other) {
            return false;
        }
    }

    /**
     * Maps a structured server error code to its user-facing explanation.
     *
     * @param errorCode server error code, possibly null
     * @return translated explanation, or null when no mapping applies
     */
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

    /**
     * Translates known free-form server text and normalizes unknown messages.
     *
     * @param message server-provided message, possibly null or blank
     * @return matching translation, normalized lowercase text, or a generic
     *         completion message
     */
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

    /**
     * Derives a message from an otherwise unrecognized throwable.
     *
     * @param cause throwable to inspect
     * @return translated throwable message or its simple class name
     */
    private static String describeFallback(Throwable cause) {
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return translateFreeform(message);
    }
}
