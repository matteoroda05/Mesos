package it.polimi.ingsw.mesos.dto.event;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.model.enums.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventLogEntryTest {

    @Test
    void formatBuildingName_buildingPrefixedCamelCase_stripsPrefixAndSpacesWords() {
        assertEquals("Shamanic Ritual", EventLogEntry.formatBuildingName("BuildingShamanicRitual"));
    }

    @Test
    void formatBuildingName_nameWithoutBuildingPrefix_spacesCamelCaseBoundaries() {
        assertEquals("End Game", EventLogEntry.formatBuildingName("EndGame"));
    }

    @Test
    void formatBuildingName_exactlyBuildingPrefix_returnsEmptyString() {
        assertEquals("", EventLogEntry.formatBuildingName("Building"));
    }

    @Test
    void turnOrderTitle_anyEntry_returnsTurnOrderHeading() {
        assertEquals("Turn order", new EventLogEntry.TurnOrder("Alice", Color.BLUE, 0, 0).title());
    }

    @Test
    void turnOrderBody_withFoodAndPrestigeChanges_describesGainsAndLosses() {
        assertEquals(
                "Player Alice (BLUE) return to the turn order tile gained 2 food and lost 1 prestige points",
                new EventLogEntry.TurnOrder("Alice", Color.BLUE, 2, -1).body());
    }

    @Test
    void turnOrderBody_withNoChanges_appendsNoChangeSuffix() {
        assertEquals(
                "Player Bob (YELLOW) return to the turn order tile with no change",
                new EventLogEntry.TurnOrder("Bob", Color.YELLOW, 0, 0).body());
    }

    @Test
    void roundBody_eventSummary_concatenatesNameLinesAndActivations() {
        EventLogEntry.Round round = new EventLogEntry.Round(
                "EVENTS ROUND",
                List.of(new EventSummary(
                        "Hunt Event",
                        List.of(new PlayerEventLine("Alice", Color.BLUE, 2, 1)),
                        List.of("Shamanic Ritual activated"))));

        assertEquals(
                "Hunt Event\n- Player Alice (BLUE) gained 2 food and gained 1 prestige points\nShamanic Ritual activated",
                round.body());
    }
}
