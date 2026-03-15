package it.polimi.ingsw.mesos.Model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public class GameContext {
    private final UUID game_id;
    private int round_number;
    private Era current_era;
    private GameTable game_table;
    private Map<Color, Player> players;

    public GameContext() {
        this.game_id = UUID.randomUUID();
        this.round_number = 0;
        this.current_era = Era.FIRST;
    }

    public void setupGame(Map<Color, Player> players) {
        int num_players = players.size();
        
        if (num_players < Globals.MIN_NUM_PLAYERS || num_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "Number of players must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }

        this.players = players;
        this.game_table = new GameTable(num_players);
    }

    public UUID getGameId() {
        return game_id;
    }

    public int getRoundNumber() {
        return round_number;
    }

    public Era getCurrentEra() {
        return current_era;
    }

    public GameTable getGameTable() {
        return game_table;
    }

    public List<Player> getPlayers() {
        if (this.players == null) throw new IllegalStateException("players cannot be null");

        return List.copyOf(this.players.values());
    }

    public Player getPlayer(Color color) {
        if (players == null) throw new IllegalStateException("players cannot be null");
        if (players.get(color) == null) throw new IllegalArgumentException("color not present");

        return players.get(color);
    }

    public void newRound(){
        this.round_number += 1;
        boolean new_era = this.game_table.newRound();
        if (new_era) {
            this.current_era = this.current_era.next();
        }
    }

    public void computeFinalScores() {
        for (Player p : players.values()) {
            p.changePrestigePoints(p.getTribe().tribeEndGamePrestige());
        }
    }
}
