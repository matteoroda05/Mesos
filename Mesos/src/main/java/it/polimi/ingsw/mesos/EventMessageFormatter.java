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
        if (entry instanceof EventLogEntry.Round round) {
            return round.title();
        }
        if (entry instanceof EventLogEntry.Notice notice) {
            return notice.title();
        }
        return "Turn order";
    }

    /**
     * Returns the multi-line body describing an event entry, without the
     * heading returned by {@link #title(EventLogEntry)}.
     *
     * @param entry event entry to describe
     * @return body text (may contain newlines)
     */
    public static String body(EventLogEntry entry) {
        if (entry instanceof EventLogEntry.Round round) {
            StringBuilder builder = new StringBuilder();
            for (EventSummary event : round.events()) {
                builder.append(event.eventName()).append('\n');
                for (PlayerEventLine line : event.lines()) {
                    builder.append("- ").append(describeLine(line)).append('\n');
                }
                for (String activation : event.buildingActivations()) {
                    builder.append(activation).append('\n');
                }
            }
            return builder.toString().stripTrailing();
        }
        if (entry instanceof EventLogEntry.TurnOrder turnOrder) {
            return "Player " + turnOrder.color()
                    + " return to the turn order tile" + turnOrderSuffix(turnOrder);
        }
        if (entry instanceof EventLogEntry.Notice notice) {
            return notice.body();
        }
        return "";
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

    private static String describeLine(PlayerEventLine line) {
        String change = joinChanges(line.foodDelta(), line.prestigeDelta(), false);
        return "Player " + line.color() + " " + change;
    }

    private static String turnOrderSuffix(EventLogEntry.TurnOrder turnOrder) {
        if (turnOrder.foodDelta() == 0 && turnOrder.prestigeDelta() == 0) {
            return " with no change";
        }
        return " " + joinChanges(turnOrder.foodDelta(), turnOrder.prestigeDelta(), true);
    }

    private static String joinChanges(int foodDelta, int prestigeDelta, boolean gerund) {
        List<String> clauses = new ArrayList<>();
        if (foodDelta != 0) {
            clauses.add(verb(foodDelta, gerund) + " " + Math.abs(foodDelta) + " food");
        }
        if (prestigeDelta != 0) {
            clauses.add(verb(prestigeDelta, gerund) + " " + Math.abs(prestigeDelta) + " prestige points");
        }
        return String.join(" and ", clauses);
    }

    private static String verb(int delta, boolean gerund) {
        if (gerund) {
            return delta > 0 ? "gaining" : "losing";
        }
        return delta > 0 ? "gained" : "lost";
    }
}
