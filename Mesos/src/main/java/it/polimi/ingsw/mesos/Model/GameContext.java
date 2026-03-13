package it.polimi.ingsw.mesos.Model;

import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.Color;

public class GameContext {
    UUID game_id;
    int round_number;
    Era current_era;
    GameTable game_table;
    Map<Color, Player> players;
    int current_player_index;
    TribeDeck tribe_deck;
    Map<Era, BuildingDeck> building_decks;

    public GameContext() {
        this.game_id = UUID.randomUUID();
        this.round_number = 0;
        this.current_player_index = 0;
        this.current_era = Era.FIRST;
    }

    public void setupGame(Map<Color, Player> players) {
        int size = players.size();
        
        if (num_players < 2 || num_players > 5) {
            throw new IllegalArgumentException("Number of players must be between 2 and 5");
        }

        this.players = players;
        this.game_table = new GameTable(num_players);
        this.tribe_deck = new TribeDeck(num_players);
        this.building_decks = Map.of(
            Era.FIRST, new BuildingDeck(Era.FIRST, num_players),
            Era.SECOND, new BuildingDeck(Era.SECOND, num_players),
            Era.THIRD, new BuildingDeck(Era.THIRD, num_players)
        );
    }

    // TODO: computeFinalScores(), getters
}