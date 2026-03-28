package it.polimi.ingsw.mesos.Model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

public class GameContext {
    private final String game_name;
    private final int game_size;
    private int round_number;
    private Era current_era;
    private final GameTable game_table;
    private Map<Color, Player> players;

    public GameContext(String game_name, int game_size, GameTable game_table) {
        if (game_name == null || game_name.isBlank()) {
            throw new IllegalArgumentException("game_name cannot be null or blank");
        }
        if (game_size < Globals.MIN_NUM_PLAYERS || game_size > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "Game size must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
        if (game_table == null) {
            throw new IllegalArgumentException("game_table cannot be null");
        }

        this.round_number = 0;
        this.current_era = Era.FIRST;
        this.game_name = game_name;
        this.game_size = game_size;
        this.game_table = game_table;
        this.players = new EnumMap<>(Color.class);
    }

    public void setupPlayers(Map<Color, Player> players) {
        if (players == null)
            throw new IllegalArgumentException("players cannot be null");

        int num_players = players.size();

        if (num_players != this.game_size) {
            throw new IllegalArgumentException(
                    "Number of players must be equal to " + this.game_name + ".");
        }
        this.players = players;
        // Place totems on turn order tile on casual order
        TurnOrderTile tile = this.game_table.getTurnOrderTile();
        for (Player p : players.values()) {
            tile.setupTotem(p);
        }
    }

    public String getGameName() {
        return this.game_name;
    }

    public int getGameSize() {
        return this.game_size;
    }

    public int getRoundNumber() {
        return this.round_number;
    }

    public Era getCurrentEra() {
        return this.current_era;
    }

    public GameTable getGameTable() {
        return this.game_table;
    }

    public List<Player> getPlayers() {
        if (this.players == null)
            throw new IllegalStateException("players cannot be null");

        return List.copyOf(this.players.values());
    }

    public Player getPlayer(Color color) {
        if (players == null)
            throw new IllegalStateException("players cannot be null");
        if (players.get(color) == null)
            throw new IllegalArgumentException("color not present");

        return players.get(color);
    }

    public void newRound() {
        this.round_number += 1;
    }

    public void computeFinalScores() {
        for (Player player : players.values()) {
            player.changePrestigePoints(player.getTribe().tribeEndGamePrestige());
        }
    }
}
