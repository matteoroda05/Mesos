package it.polimi.ingsw.mesos;

import java.io.Serializable;
import java.util.List;

/**
 * Summary of a single resolved event: a display name, one line per player
 * describing the applied food/prestige change, and any buildings that were
 * activated while resolving the event.
 *
 * @param eventName human-readable event name (e.g. {@code "Hunt Event"})
 * @param lines per-player applied changes for this event
 * @param buildingActivations ready-to-display notes for buildings activated
 *                            during this event
 */
public record EventSummary(String eventName, List<PlayerEventLine> lines, List<String> buildingActivations)
        implements Serializable {

    /**
     * Converts an event card class simple name into a display name, e.g.
     * {@code "HuntEvent"} into {@code "Hunt Event"}.
     *
     * @param simpleName the card class simple name
     * @return the spaced, human-readable event name
     */
    public static String formatEventName(String simpleName) {
        String baseName = simpleName
                .replace("Card", "")
                .replace("Event", " Event");

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < baseName.length(); index++) {
            char character = baseName.charAt(index);
            if (index > 0 && Character.isUpperCase(character) && baseName.charAt(index - 1) != ' ') {
                builder.append(' ');
            }
            builder.append(character);
        }
        return builder.toString().trim();
    }
}
