package it.polimi.ingsw.mesos.dto.game;

import java.io.Serializable;

import it.polimi.ingsw.mesos.model.enums.Era;

/**
 * Serializable summary of a card exposed to network clients.
 *
 * @param selectionIndex zero-based index used by clients when choosing the card
 * @param type display type of the card
 * @param era era of the card, when available
 * @param event true when the card is an event card
 * @param details human-readable effect summary, normalized to an empty string
 *                when null
 * @param foodCost optional food cost for building cards
 * @param prestigePoints optional prestige value for building cards
 */
public record CardSummary(
        int selectionIndex,
        String type,
        Era era,
        boolean event,
        String details,
        Integer foodCost,
        Integer prestigePoints,
        String presentationId
) implements Serializable {

    /**
     * Validates card summary data and normalizes optional text fields.
     *
     * @throws IllegalArgumentException if selection index is negative or type
     *                                  is null/blank
     */
    public CardSummary(
            int selectionIndex,
            String type,
            Era era,
            boolean event,
            String details,
            Integer foodCost,
            Integer prestigePoints
    ) {
        this(selectionIndex, type, era, event, details, foodCost, prestigePoints, null);
    }

    /**
     * Validates canonical card data and normalizes nullable display fields.
     *
     * @throws IllegalArgumentException if the selection index is negative or
     *                                  the type is null or blank
     */
    public CardSummary {
        if (selectionIndex < 0) {
            throw new IllegalArgumentException("selectionIndex cannot be negative");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type cannot be null or blank");
        }
        if (details == null) {
            details = "";
        }
        if (presentationId != null && presentationId.isBlank()) {
            presentationId = null;
        }
    }
}
