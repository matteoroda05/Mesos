package it.polimi.ingsw.mesos.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @BeforeEach
    void setUp() {
        // avoid changing real database
        DatabaseManager.useTestMode();
    }

    /**
     * Verifies that the database initialization finishes successfully
     * and the Singleton instance is not null.
     */
    @Test
    void testInitialization() {
        DatabaseManager db = DatabaseManager.getInstance();
        assertNotNull(db, "The DatabaseManager instance should not be null.");
    }

    /**
     * Verifies the coherence between basic write and read operations.
     */
    @Test
    void testSaveAndReadCoherence() {
        DatabaseManager db = DatabaseManager.getInstance();

        String nickname = "Player1";
        int score = 150;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        int numPlayers = 3;

        // write
        boolean isSaved = db.saveMatchResult(nickname, score, timestamp, numPlayers);
        assertTrue(isSaved, "Saving the match result should return true.");

        // read
        List<LeaderboardEntry> leaderboard = db.getLeaderboard();
        assertEquals(1, leaderboard.size(), "The leaderboard should contain exactly 1 record.");

        LeaderboardEntry entry = leaderboard.getFirst();
        assertEquals(nickname, entry.nickname(), "The stored nickname does not match.");
        assertEquals(score, entry.score(), "The stored score does not match.");
        assertEquals(numPlayers, entry.numPlayers(), "The number of players does not match.");
        assertEquals(1, entry.rank(), "As the only player, their rank must be 1.");
    }

    /**
     * Verifies the correctness of a player's rank calculation based on
     * the score and the total number of participants in that specific match.
     */
    @Test
    void testGetPlayerRank() {
        DatabaseManager db = DatabaseManager.getInstance();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 4-player match
        db.saveMatchResult("Alice", 120, now, 4);
        db.saveMatchResult("Bob", 200, now, 4);
        db.saveMatchResult("Charlie", 80, now, 4);
        db.saveMatchResult("David", 90, now, 4);

        // very high score but for a 2-player match: must not interfere with the 4 player match
        db.saveMatchResult("Diana", 300, now, 2);

        // Expected 4-player rankings: Bob (200) -> 1st, Alice (120) -> 2nd, David (90) -> 3rd, Charlie (80) -> 4th
        assertEquals(1, db.getPlayerRank(200, 4), "Bob with 200 points should be 1st.");
        assertEquals(2, db.getPlayerRank(120, 4), "Alice with 120 points should be 2nd.");
        assertEquals(3, db.getPlayerRank(90, 4), "David with 90 points should be 3rd.");
        assertEquals(4, db.getPlayerRank(80, 4), "Charlie with 80 points should be 4th.");

        // Verify isolation of the num_players filter
        assertEquals(1, db.getPlayerRank(300, 2), "Diana in the 2-player category must be 1st.");
    }

    /**
     * Verifies that the leaderboard filtered by player count properly excludes
     * non-relevant matches and orders the results in descending order.
     */
    @Test
    void testGetLeaderboardByNumPlayers() {
        DatabaseManager db = DatabaseManager.getInstance();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // mixed data
        db.saveMatchResult("TargetLow", 50, now, 3);
        db.saveMatchResult("TargetHigh", 180, now, 3);
        db.saveMatchResult("IgnoredPlayer", 250, now, 4); // Different category, should be ignored

        // fetch leaderboard only for 3-player matches
        List<LeaderboardEntry> leaderboard3 = db.getLeaderboardByNumPlayers(3);

        assertEquals(2, leaderboard3.size(), "The filtered leaderboard for 3 players must contain exactly 2 elements.");

        // check descending order
        assertEquals("TargetHigh", leaderboard3.get(0).nickname(), "The first element must be the highest score.");
        assertEquals(180, leaderboard3.get(0).score());

        assertEquals("TargetLow", leaderboard3.get(1).nickname(), "The second element must be the lowest score.");
        assertEquals(50, leaderboard3.get(1).score());
    }

    /**
     * Verifies that in case of a tie in score, the ranking calculation
     * in the filtered method correctly applies the expected tie-breaking logic.
     */
    @Test
    void testLeaderboardExAequoHandling() {
        DatabaseManager db = DatabaseManager.getInstance();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        db.saveMatchResult("PlayerA", 100, now, 2);
        db.saveMatchResult("PlayerB", 100, now, 2); // Same score as PlayerA
        db.saveMatchResult("PlayerC", 50, now, 2);

        List<LeaderboardEntry> leaderboard = db.getLeaderboardByNumPlayers(2);

        // PlayerA and PlayerB should have rank 1 (tie),
        assertEquals(1, leaderboard.get(0).rank(), "The first player with 100 points has rank 1.");
        assertEquals(1, leaderboard.get(1).rank(), "The second player with 100 points must have rank 1 (tie).");
        assertEquals(3, leaderboard.get(2).rank(), "The next player must skip to rank 3.");
    }

    /**
     * Verifies that the lock system properly handles concurrent writes.
     * Pushes multiple threads to write to the DB simultaneously. If locks work,
     * all writes must succeed without throwing "Database locked" exceptions.
     * Simulate 10 matches ending at the exact same time.
     */
    @Test
    void testConcurrentWritesWithLock() throws InterruptedException {
        DatabaseManager db = DatabaseManager.getInstance();

        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // hold threads and release simultaneously
        CountDownLatch startLatch = new CountDownLatch(1);

        // ensure all threads have finished
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        // count number of successful writes
        AtomicInteger successfulWrites = new AtomicInteger(0);

        // create threads
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for signal

                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    // concurrent write attempt
                    boolean success = db.saveMatchResult("ThreadPlayer_" + threadId, 100 + threadId, now, 4);

                    if (success) {
                        successfulWrites.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // countdown for thread start
        startLatch.countDown();

        // wait for threads to finish
        boolean allThreadsFinished = finishLatch.await(3, TimeUnit.SECONDS);

        assertTrue(allThreadsFinished, "Threads got stuck in a deadlock or were too slow.");

        assertEquals(numberOfThreads, successfulWrites.get(),
                "Some concurrent writes failed (likely missing lock or SQL exception).");

        executor.shutdown();
    }
}