package it.polimi.ingsw.mesos.Model;

import java.util.UUID;
import java.util.Map;

import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.Color;

public class GameContext {
    private UUID game_id;
    private int round_number;
    private Era current_era;
    private GameTable game_table;
    private Map<Color, Player> players;
    private int current_player_index;
    private TribeDeck tribe_deck;
    private BuildingDeck building_deck;

    public GameContext() {
        this.game_id = UUID.randomUUID();
        this.round_number = 0;
        this.current_player_index = 0;
        this.current_era = Era.FIRST;
    }

    public void setupGame(Map<Color, Player> players) {
        int num_players = players.size();
        
        if (num_players < 2 || num_players > 5) {
            throw new IllegalArgumentException("Number of players must be between 2 and 5");
        }

        this.players = players;
        this.game_table = new GameTable(num_players);
        this.tribe_deck = new TribeDeck(num_players);
        this.building_deck = new BuildingDeck(num_players);
    }

    // TODO: computeFinalScores(), getters
}