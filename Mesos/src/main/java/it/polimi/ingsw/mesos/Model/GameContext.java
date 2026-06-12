package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.EventLogEntry;
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
    private final List<EventLogEntry> event_log = new ArrayList<>();

    /**
     * Creates a game context with round zero, the first era, and an empty player
     * registry.
     *
     * @param game_name non-blank game name
     * @param game_size expected number of players
     * @param game_table table used by this game
     * @throws IllegalArgumentException if the name is null or blank, the game
     *                                  size is outside the allowed range, or
     *                                  the table is null
     */
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

    /**
     * Registers the players for this game and places their totems on the turn
     * order tile in the iteration order provided by the players map.
     *
     * @param players the players keyed by their color
     * @throws IllegalArgumentException if the map is null, has a size different
     *                                  from the configured game size, or
     *                                  contains a null player value
     * @throws IllegalStateException if the turn-order tile has no free space
     *                               for a player totem
     */
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

    /**
     * Returns the configured game name.
     *
     * @return game name
     */
    public String getGameName() {
        return this.game_name;
    }

    /**
     * Returns the number of players this game expects.
     *
     * @return configured game size
     */
    public int getGameSize() {
        return this.game_size;
    }

    /**
     * Returns the current round counter.
     *
     * @return round number, starting from zero before the first round advance
     */
    public int getRoundNumber() {
        return this.round_number;
    }

    /**
     * Returns the era currently active for the game.
     *
     * @return current era
     */
    public Era getCurrentEra() {
        return this.current_era;
    }

    /**
     * Returns the table associated with this context.
     *
     * @return game table
     */
    public GameTable getGameTable() {
        return this.game_table;
    }

    /**
     * Returns an immutable snapshot of registered players.
     *
     * @return players currently registered in this context
     * @throws IllegalStateException if the player registry is unexpectedly null
     */
    public List<Player> getPlayers() {
        if (this.players == null)
            throw new IllegalStateException("players cannot be null");

        return List.copyOf(this.players.values());
    }

    /**
     * Finds a registered player by color.
     *
     * @param color player color to look up
     * @return the player registered with the given color
     * @throws IllegalStateException if the player registry is unexpectedly null
     * @throws IllegalArgumentException if the color is null or not registered
     */
    public Player getPlayer(Color color) {
        if (players == null)
            throw new IllegalStateException("players cannot be null");
        if (players.get(color) == null)
            throw new IllegalArgumentException("color not present");

        return players.get(color);
    }

    /**
     * Records a structured game event so the network layer can broadcast it to
     * clients as a popup.
     *
     * @param entry event entry to record
     */
    public void logEventEntry(EventLogEntry entry) {
        if (entry != null) {
            this.event_log.add(entry);
        }
    }

    /**
     * Returns the events accumulated since the last drain and clears the buffer.
     *
     * @return events recorded since the previous call, in insertion order
     */
    public List<EventLogEntry> drainEventLog() {
        List<EventLogEntry> drained = List.copyOf(this.event_log);
        this.event_log.clear();
        return drained;
    }

    /**
     * Advances the round counter by one without changing the current era.
     */
    public void newRound() {
        this.round_number += 1;
    }

    /**
     * Moves the game to the next era according to {@link Era#next()}.
     *
     * @throws IllegalStateException if the current era is already the last one
     */
    public void newEra() {
        this.current_era = this.current_era.next();
    }

    /**
     * Applies all end-game prestige generated by each player's tribe to the
     * corresponding player score.
     * Building end-game effects do not count in these calculations.
     */
    public void computeFinalScores() {
        for (Player player : players.values()) {
            player.changePrestigePoints(player.getTribe().tribeEndGamePrestige());
        }
    }
}
