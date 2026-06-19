package it.polimi.ingsw.mesos;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.Color;

/**
 * Renders {@link EventLogEntry} payloads into the human-readable text shown to
 * players (e.g. in a GUI popup or on the CLI). The server sends structured data;
 * all wording lives here so every front-end renders events identically.
 */
public final class EventMessageFormatter {

    /**
     * Prevents instantiation of this static formatting utility.
     */
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
     * Formats a player citation as {@code "<nickname> (<COLOR>)"} so events name
     * the player rather than only their color.
    *
    * @param nickname player nickname
    * @param color player color
    * @return the combined citation label
    */
   public static String playerLabel(String nickname, Color color) {
       return nickname + " (" + color + ")";
    }
    
    /**
     * Formats the non-zero food and prestige changes as one event message.
     * @param foodDelta food bonus/penalty
     * @param prestigeDelta prestige gained/lost
     * @return clauses such as {@code "gained 2 food and lost 1 prestige points"},
     *         or an empty string when both changes are zero
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

    /**
     * Formats one signed amount using the matching past-tense verb.
     *
     * @param delta signed amount to format
     * @return gained or lost amount text
     */
    private static String verb(int delta) {
        return (delta > 0 ? "gained" : "lost") + " " + Math.abs(delta);
    }
}
