package it.polimi.ingsw.mesos.database;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implement a local database with the scores of matches played.
 * Has function utilities to interact with the database.
 * Implements the singleton pattern.
 */
public class DatabaseManager {
    /**
     * Name of local instance of the database.
     */
    private static String dbUrl = "jdbc:sqlite:game_leaderboard.db";

    /**
     * Singleton instance. \verb{volatile} is needed for thread safety during construction.
     */
    private static volatile DatabaseManager instance;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(); // lock for write operations

    /**
     * Initializes the local leaderboard database and creates its table if needed.
     */
    private DatabaseManager() {
        initializeDatabase();
    }

    /**
     * Creates the instance if not already created. Ensures thread safety during creation.
     * @return instance of database manager.
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Call during testing: results will be saved in a RAM-hosted database,
     * without modifying the real database.
     */
    public static void useTestMode() {
        synchronized (DatabaseManager.class) {
            dbUrl = "jdbc:sqlite::memory:";
            instance = null; // Forza il Singleton a rigenerarsi con il nuovo URL in RAM
        }
    }

    /**
     * Opens a new connection to the local SQLite leaderboard database.
     *
     * @return an open database connection
     * @throws SQLException if the connection cannot be established
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * If there is no database create one, otherwise do nothing.
     */
    private void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS match_history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nickname TEXT NOT NULL, "
                + "final_score INTEGER NOT NULL, "
                + "match_date TEXT NOT NULL, "
                + "num_players INTEGER NOT NULL"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("[DB] SQLite database initialized correctly: ready.");
        } catch (SQLException e) {
            System.err.println("[DB] Error during database initialization: " + e.getMessage());
        }
    }

    /**
     * Insert an entry in the database.
     * @param nickname player nickname
     * @param finalScore player score to save
     * @param timestamp match timestamp
     * @param numPlayers number of players in the match
     * @return false if an exception occurs during the database update, true otherwise.
     */
    public boolean saveMatchResult(String nickname, int finalScore, Timestamp timestamp, int numPlayers) {
        String insertSQL = "INSERT INTO match_history (nickname, final_score, match_date, num_players) VALUES (?, ?, ?, ?)";

        rwLock.writeLock().lock();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, nickname);
            pstmt.setInt(2, finalScore);
            pstmt.setString(3, timestamp.toString());
            pstmt.setInt(4, numPlayers);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error during results save: " + e.getMessage());
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }

        return true;
    }

    /**
     * Return the player rank based on its score and number of players in the match.
     * @param finalScore player's score
     * @param numPlayers number of players in the match
     * @return rank
     */
    public int getPlayerRank(int finalScore, int numPlayers) {
        String rankSQL = "SELECT COUNT(*) + 1 AS rank FROM match_history WHERE num_players = ? AND final_score > ?";

        rwLock.readLock().lock();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(rankSQL)) {

            pstmt.setInt(1, numPlayers);
            pstmt.setInt(2, finalScore);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("rank");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore calcolo rank: " + e.getMessage());
        } finally {
            rwLock.readLock().unlock();
        }
        return -1;
    }

    /**
     * Creates a list of serializable records with the top scores in matches with {@code numPlayers} players
     * @param numPlayers number of players of the matches to filter
     * @return top 50 rankings
     */
    public List<LeaderboardEntry> getLeaderboardByNumPlayers(int numPlayers) {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String querySQL = "SELECT nickname, final_score, match_date FROM match_history WHERE num_players = ? ORDER BY final_score DESC LIMIT 50";

        rwLock.readLock().lock();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            pstmt.setInt(1, numPlayers);

            try (ResultSet rs = pstmt.executeQuery()) {
                int currRank = 0;
                int lastScore = Integer.MAX_VALUE;
                int counter = 1;

                while (rs.next()) {
                    String nickname = rs.getString("nickname");
                    int score = rs.getInt("final_score");
                    String dateStr = rs.getString("match_date");
                    Timestamp timestamp = Timestamp.valueOf(dateStr);

                    if (score < lastScore) {
                        currRank += counter;
                        lastScore = score;
                        counter = 1;
                    } else {
                        counter++;
                    }

                    leaderboard.add(new LeaderboardEntry(currRank, nickname, score, timestamp, numPlayers));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore recupero classifica: " + e.getMessage());
        } finally {
            rwLock.readLock().unlock();
        }

        return leaderboard;
    }

    /**
     * Create a list of serializable records with the contents of the history database
     * @return ordered list of the top 50 scores
     */
    public List<LeaderboardEntry> getLeaderboard() {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String querySQL = "SELECT nickname, final_score, match_date, num_players FROM match_history ORDER BY final_score DESC LIMIT 50";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            try (ResultSet rs = pstmt.executeQuery()) {
                int currRank = 0;
                int lastScore = Integer.MAX_VALUE;

                while (rs.next()) {
                    String nickname = rs.getString("nickname");
                    int score = rs.getInt("final_score");
                    String dateStr = rs.getString("match_date");
                    Timestamp timestamp = Timestamp.valueOf(dateStr);
                    final var numPlayers = rs.getInt("num_players");

                    if (score < lastScore) {
                        currRank++;
                        lastScore = score;
                    }

                    leaderboard.add(new LeaderboardEntry(currRank, nickname, score, timestamp, numPlayers));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error retrieving data from database: " + e.getMessage());
        }
        return leaderboard;
    }
}