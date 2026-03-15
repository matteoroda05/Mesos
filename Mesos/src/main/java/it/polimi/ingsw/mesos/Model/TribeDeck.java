package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;

public class TribeDeck {
    private List<TribeCard> deck;
    private int n_players;

    public TribeDeck(int n_players) {
        if (n_players < Globals.MIN_NUM_PLAYERS || n_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "Number of players must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }

        this.n_players = n_players;
    }

    public List<TribeCard> drawTopRow() {
        int num_cards = this.n_players + 4;
        List<TribeCard> output = new ArrayList<>();
        for (int i = 0; i < num_cards; i++) {
            output.add(deck.removeLast());
        } 
        return output;
    }

    public List<TribeCard> drawInitialBottomRow() {
        int num_cards = this.n_players + 1;
        List<TribeCard> output = new ArrayList<>();
        List<TribeCard> events = new ArrayList<>();
    
        for (int i = 0; i < num_cards; i++) {
            TribeCard card = deck.removeLast();
            // TODO: use type enum instead of instanceof
            if (card instanceof EventCard) {
                events.add(card);
                i--;
            } else {
                output.add(card);
            }
        }

        deck.addAll(events);
        return output;
    }

    public boolean isEmpty() {
        return deck.isEmpty();
    }
}