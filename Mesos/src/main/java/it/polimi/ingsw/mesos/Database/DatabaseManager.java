package it.polimi.ingsw.mesos.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:game_leaderboard.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

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

    public boolean saveMatchResult(String nickname, int finalScore, Timestamp timestamp, int numPlayers) {
        String insertSQL = "INSERT INTO match_history (nickname, final_score, match_date, num_players) VALUES (?, ?, ?, ?)";

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
        }

        return true;
    }

    public int getPlayerRank(int finalScore, int numPlayers) {
        String rankSQL = "SELECT COUNT(*) + 1 AS rank FROM match_history WHERE num_players = ? AND final_score > ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(rankSQL)) {

            pstmt.setInt(1, numPlayers);
            pstmt.setInt(2, finalScore);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("rank");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore calcolo rank: " + e.getMessage());
        }
        return -1;
    }

    public List<LeaderboardEntry> getLeaderboardByPlayerCount(int numPlayers) {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String querySQL = "SELECT nickname, final_score, match_date FROM match_history WHERE num_players = ? ORDER BY final_score DESC LIMIT 50";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            pstmt.setInt(1, numPlayers);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nickname = rs.getString("nickname");
                    int score = rs.getInt("final_score");
                    String dateStr = rs.getString("match_date");
                    Timestamp timestamp = Timestamp.valueOf(dateStr);

                    leaderboard.add(new LeaderboardEntry(nickname, score, timestamp, numPlayers));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore recupero classifica: " + e.getMessage());
        }
        return leaderboard;
    }

    public List<LeaderboardEntry> getLeaderboard() {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String querySQL = "SELECT nickname, final_score, match_date, num_players FROM match_history ORDER BY final_score DESC LIMIT 50";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String nickname = rs.getString("nickname");
                    int score = rs.getInt("final_score");
                    String dateStr = rs.getString("match_date");
                    Timestamp timestamp = Timestamp.valueOf(dateStr);
                    final var numPlayers = rs.getInt("num_players");

                    leaderboard.add(new LeaderboardEntry(nickname, score, timestamp, numPlayers));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error retrieving data from database: " + e.getMessage());
        }
        return leaderboard;
    }
}