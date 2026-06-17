package it.polimi.ingsw.mesos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventMessageFormatterTest {

    @Test
    void joinChanges_foodAndPrestigeBothNonZero_listsBothClauses() {
        assertEquals("gained 2 food and lost 3 prestige points",
                EventMessageFormatter.joinChanges(2, -3));
    }

    @Test
    void joinChanges_onlyFoodNonZero_listsFoodClauseOnly() {
        assertEquals("lost 1 food", EventMessageFormatter.joinChanges(-1, 0));
    }

    @Test
    void joinChanges_onlyPrestigeNonZero_listsPrestigeClauseOnly() {
        assertEquals("gained 5 prestige points", EventMessageFormatter.joinChanges(0, 5));
    }

    @Test
    void joinChanges_bothDeltasZero_returnsEmptyString() {
        assertEquals("", EventMessageFormatter.joinChanges(0, 0));
    }

    @Test
    void title_entry_returnsEntryTitle() {
        EventLogEntry entry = new EventLogEntry.Notice("Heading", "Body text");
        assertEquals("Heading", EventMessageFormatter.title(entry));
    }

    @Test
    void body_entry_returnsEntryBody() {
        EventLogEntry entry = new EventLogEntry.Notice("Heading", "Body text");
        assertEquals("Body text", EventMessageFormatter.body(entry));
    }

    @Test
    void format_entry_combinesTitleAndBodySeparatedByNewline() {
        EventLogEntry entry = new EventLogEntry.Notice("Heading", "Body text");
        assertEquals("Heading\nBody text", EventMessageFormatter.format(entry));
    }
}
