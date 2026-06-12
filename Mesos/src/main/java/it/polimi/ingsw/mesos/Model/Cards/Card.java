package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.CardSummary;

abstract public class Card {
    private final Era era;
    private final int min_players;
    private String presentationId;

    /**
     * Creates a card with its era and minimum player-count requirement.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range 0 to 5
     */
    public Card(Era era, int min_players) {
        if (min_players < 0 || min_players > Globals.MAX_NUM_PLAYERS) throw new IllegalArgumentException("min_players must be between 0 and 5");

        this.era = era;
        this.min_players = min_players;
        this.presentationId = null;
    }

    /**
     * Returns the era this card belongs to.
     *
     * @return card era
     */
    public Era getEra(){
        return this.era;
    }

    /**
     * Returns the minimum player count required by this card.
     *
     * @return minimum players
     */
    public int getMinPlayers() {
        return this.min_players;
    }

    @JsonProperty("presentation_id")
    public String getPresentationId() {
        return presentationId;
    }

    @JsonProperty("presentation_id")
    public void setPresentationId(String presentationId) {
        this.presentationId = (presentationId == null || presentationId.isBlank())
                ? null
                : presentationId;
    }

    /**
     * Indicates whether this card is an event card.
     *
     * @return false for non-event cards; event cards override this method
     */
    public boolean isEvent() {
        return false;
    }

    /**
     * Accepts a description visitor and returns this card's effect summary.
     *
     * @param visitor description visitor
     * @return human-readable effect description, never null
     */
    public abstract String accept(CardDescriptionVisitor visitor);

    /**
     * Creates the card description for serialization.
     *
     * @return serializable record
     */
    public abstract CardSummary getSummary(int selectionIndex);

    /**
     * Type name for serialization and CLI printing.
     *
     * @return class name as string.
     */
    public abstract String getReadableName();
}
