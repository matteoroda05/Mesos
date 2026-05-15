package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Era;

abstract public class Card {
    private final Era era;
    // private final Image image;
    private final int min_players;

    /**
     * Creates a card with its era and minimum player-count requirement.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range 0 to 5
     */
    public Card(Era era, int min_players) {
        if (min_players < 0 || min_players > 5) throw new IllegalArgumentException("min_players must be between 0 and 5");

        this.era = era;
        this.min_players = min_players;
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

    /**
     * Indicates whether this card is an event card.
     *
     * @return false for non-event cards; event cards override this method
     */
    public boolean isEvent() {
        return false;
    }
}
