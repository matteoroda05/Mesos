package it.polimi.ingsw.mesos.model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.common.Globals;
import it.polimi.ingsw.mesos.model.cards.TribeCard;

/**
 * Manages the remaining tribe cards for a match and draws the market rows in
 * their required order.
 */
public class TribeDeck {
    private final List<TribeCard> deck;
    private final int n_players;

    /**
     * Creates a tribe deck for the given number of players.
     *
     * @param deck cards to copy into the deck
     * @param n_players number of players in the game
     * @throws IllegalArgumentException if the player count is outside the
     *                                  allowed range or the deck is null
     */
    public TribeDeck(List<TribeCard> deck, int n_players) {
        if (n_players < Globals.MIN_NUM_PLAYERS || n_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "Number of players must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
        if (deck == null) {
            throw new IllegalArgumentException("deck cannot be null");
        }

        this.n_players = n_players;
        this.deck = new ArrayList<>(deck);
    }

    /**
     * Creates a tribe deck for the given number of players.
     *
     * @param n_players number of players in the game
     * @param deck cards to copy into the deck
     * @throws IllegalArgumentException if the player count is outside the
     *                                  allowed range or the deck is null
     */
    public TribeDeck(int n_players, List<TribeCard> deck) {
        if (n_players < Globals.MIN_NUM_PLAYERS || n_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "Number of players must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
        if (deck == null) {
            throw new IllegalArgumentException("deck cannot be null");
        }

        this.n_players = n_players;
        this.deck = new ArrayList<>(deck);
    }

    /**
     * Draws the cards needed to refill the top market row, capped by the
     * remaining deck size.
     *
     * @return cards drawn from the end of the deck; empty if the deck is empty
     */
    public List<TribeCard> drawTopRow() {
        int num_cards = Math.min(this.n_players + 4, deck.size());
        List<TribeCard> output = new ArrayList<>();
        for (int i = 0; i < num_cards; i++) {
            output.add(deck.removeLast());
        }
        return output;
    }

    /**
     * Draws the initial bottom row while skipping event cards, then puts the
     * skipped events back at the end of the deck (tribe deck draws from last).
     *
     * @return non-event tribe cards for the initial bottom market row, possibly
     *         fewer than requested if the deck does not contain enough
     *         non-event cards
     */
    public List<TribeCard> drawInitialBottomRow() {
        int num_cards = this.n_players + 1;
        List<TribeCard> output = new ArrayList<>();
        List<TribeCard> events = new ArrayList<>();

        while (output.size() < num_cards && !deck.isEmpty()) {
            TribeCard card = deck.removeLast();
            if (card.isEvent()) {
                events.add(card);
            } else {
                output.add(card);
            }
        }

        deck.addAll(events);
        return output;
    }

    /**
     * Checks whether the tribe deck has no cards left.
     *
     * @return true when the deck is empty
     */
    public boolean isEmpty() {
        return deck.isEmpty();
    }
}
