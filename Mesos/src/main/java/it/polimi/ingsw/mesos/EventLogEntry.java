package it.polimi.ingsw.mesos;

import it.polimi.ingsw.mesos.Model.Enums.Color;

import java.io.Serializable;
import java.util.List;

import static it.polimi.ingsw.mesos.EventMessageFormatter.joinChanges;

/**
 * A loggable game event to be broadcast to clients as a popup. Two shapes are
 * supported: a full {@link Round} summary covering every event resolved in an
 * event round, and a single-player {@link TurnOrder} notification.
 */
public sealed interface EventLogEntry extends Serializable
        permits EventLogEntry.Round, EventLogEntry.TurnOrder, EventLogEntry.Notice {
    /**
     * Interface to ensure every implementation has a {@code title} method for
     * rendering purposes.
     * @return title of the log entry
     */
    String title();

    /**
     * Interface to ensure every implementation has a {@code body} method for
     * rendering purposes.
     * @return body of the log entry
     */
    String body();

    /**
     * Reports whether this entry describes a resolved event round, as opposed to
     * an ancillary notification (turn-order return, building/character notice).
     * Front-ends can use this to give actual events more emphasis.
     *
     * @return true only for a {@link Round} entry
     */
    default boolean isEventRound() {
        return false;
    }

    /**
     * Summary of a whole event round.
     *
     * @param title popup header (e.g. {@code "EVENTS ROUND"} or {@code "FINAL EVENTS"})
     * @param events the resolved events in resolution order
     */
    record Round(String title, List<EventSummary> events) implements EventLogEntry {
        @Override
        public boolean isEventRound() {
            return true;
        }

        @Override
        public String body() {
            StringBuilder builder = new StringBuilder();

            for (EventSummary event : this.events()) {
                builder.append(event.eventName()).append('\n');

                event.lines().forEach(line -> builder.append("- ").append(line.describeLine()).append('\n'));
                event.buildingActivations().forEach(activation -> builder.append(activation).append('\n'));
            }

            return builder.toString().stripTrailing();
        }
    }

    /**
     * A single player returning to the turn-order tile.
     *
     * @param nickname nickname of the player who returned to the turn-order tile
     * @param color player who returned to the turn-order tile
     * @param foodDelta signed food change applied to the player
     * @param prestigeDelta signed prestige change applied to the player
     */
    record TurnOrder(String nickname, Color color, int foodDelta, int prestigeDelta) implements EventLogEntry {
        @Override
        public String title() {
            return "Turn order";
        }

        @Override
        public String body() {
            String suffix = (this.foodDelta() == 0 && this.prestigeDelta() == 0) ?
                    " with no change" :
                    " " + joinChanges(this.foodDelta(), this.prestigeDelta());

            return "Player " + EventMessageFormatter.playerLabel(this.nickname(), this.color())
                    + " return to the turn order tile" + suffix;
        }
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
