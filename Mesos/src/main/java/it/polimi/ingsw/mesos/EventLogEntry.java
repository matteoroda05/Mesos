package it.polimi.ingsw.mesos;

import it.polimi.ingsw.mesos.Model.Enums.Color;

import java.io.Serializable;
import java.util.List;

/**
 * A loggable game event to be broadcast to clients as a popup. Two shapes are
 * supported: a full {@link Round} summary covering every event resolved in an
 * event round, and a single-player {@link TurnOrder} notification.
 */
public sealed interface EventLogEntry extends Serializable
        permits EventLogEntry.Round, EventLogEntry.TurnOrder, EventLogEntry.Notice {

    /**
     * Summary of a whole event round.
     *
     * @param title popup header (e.g. {@code "EVENTS ROUND"} or {@code "FINAL EVENTS"})
     * @param events the resolved events in resolution order
     */
    record Round(String title, List<EventSummary> events) implements EventLogEntry {
    }

    /**
     * A single player returning to the turn-order tile.
     *
     * @param color player who returned to the turn-order tile
     * @param foodDelta signed food change applied to the player
     * @param prestigeDelta signed prestige change applied to the player
     */
    record TurnOrder(Color color, int foodDelta, int prestigeDelta) implements EventLogEntry {
    }

    /**
     * A free-form notification with a heading and a body. Used for building
     * activations, building purchases, immediate character effects, and final
     * scoring, where the descriptive text is built server-side.
     *
     * @param title heading shown for this entry
     * @param body descriptive text (may contain newlines)
     */
    record Notice(String title, String body) implements EventLogEntry {
    }

    /**
     * Converts a building card class simple name into a display name, e.g.
     * {@code "BuildingShamanicRitual"} into {@code "Shamanic Ritual"}.
     *
     * @param simpleName the building class simple name
     * @return the spaced, human-readable building name
     */
    static String formatBuildingName(String simpleName) {
        String base = simpleName.startsWith("Building")
                ? simpleName.substring("Building".length())
                : simpleName;

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < base.length(); index++) {
            char character = base.charAt(index);
            if (index > 0 && Character.isUpperCase(character)) {
                builder.append(' ');
            }
            builder.append(character);
        }
        return builder.toString().trim();
    }
}
