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
}
