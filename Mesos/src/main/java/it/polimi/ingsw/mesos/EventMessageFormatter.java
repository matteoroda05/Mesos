package it.polimi.ingsw.mesos;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders {@link EventLogEntry} payloads into the human-readable text shown to
 * players (e.g. in a GUI popup or on the CLI). The server sends structured data;
 * all wording lives here so every front-end renders events identically.
 */
public final class EventMessageFormatter {

    private EventMessageFormatter() {
    }

    /**
     * Returns the short heading for an event entry (e.g. the round title or
     * {@code "Turn order"}).
     *
     * @param entry event entry to describe
     * @return heading text
     */
    public static String title(EventLogEntry entry) {
        return entry.title();
    }

    /**
     * Returns the multi-line body describing an event entry, without the
     * heading returned by {@link #title(EventLogEntry)}.
     *
     * @param entry event entry to describe
     * @return body text (may contain newlines)
     */
    public static String body(EventLogEntry entry) {
        return entry.body();
    }

    /**
     * Returns the full text (heading followed by body) for an event entry.
     *
     * @param entry event entry to describe
     * @return combined heading and body
     */
    public static String format(EventLogEntry entry) {
        return title(entry) + "\n" + body(entry);
    }

    /**
     * Formatting utility
     * @param foodDelta food bonus/penalty
     * @param prestigeDelta prestige gained/lost
     * @return message listing the changes
     */
    public static String joinChanges(int foodDelta, int prestigeDelta) {
        List<String> clauses = new ArrayList<>();
        if (foodDelta != 0) {
            clauses.add(verb(foodDelta) + " food");
        }
        if (prestigeDelta != 0) {
            clauses.add(verb(prestigeDelta) + " prestige points");
        }
        return String.join(" and ", clauses);
    }

    private static String verb(int delta) {
        return (delta > 0 ? "gained" : "lost") + " " + Math.abs(delta);
    }
}
