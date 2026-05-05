package it.polimi.ingsw.mesos.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.HeartbeatRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.ReconnectGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.ReconnectProbeRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;

class MesosServerCoreTest {
    @Test
    void createJoinAndSnapshotStartTheGame() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        GameOperationResult createGame = serverCore.createGame(new CreateGameRequest("local-core", 2));
        assertTrue(createGame.success());
        assertEquals(GameStatus.LOGIN, createGame.snapshot().status());

        GameOperationResult firstJoin = serverCore.joinGame(new JoinGameRequest("local-core", "Alice"));
        assertTrue(firstJoin.success());
        assertEquals(1, firstJoin.snapshot().joinedPlayers());

        GameOperationResult secondJoin = serverCore.joinGame(new JoinGameRequest("local-core", "Bob"));
        assertTrue(secondJoin.success());
        assertEquals(GameStatus.ROUND_OFFER, secondJoin.snapshot().status());
        assertEquals(1, secondJoin.snapshot().currentRound());
        assertNotNull(secondJoin.snapshot().currentPlayerNickname());
        assertNotNull(secondJoin.snapshot().players().stream()
                .filter(player -> "Bob".equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color());

        GameOperationResult snapshot = serverCore.getSnapshot(new SnapshotRequest("local-core"));
        assertTrue(snapshot.success());
        assertEquals(2, snapshot.snapshot().players().size());
    }

    @Test
    void lobbyListsOnlyJoinableGamesAndFullGamesRejectWithoutSnapshot() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("open-room", 3)).success());
        assertTrue(serverCore.createGame(new CreateGameRequest("full-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("full-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("full-room", "Bob")).success());

        GameOperationResult lobby = serverCore.getLobby();
        assertTrue(lobby.success());
        assertEquals(1, lobby.lobbySnapshot().openGames().size());
        assertEquals("open-room", lobby.lobbySnapshot().openGames().getFirst().gameName());

        GameOperationResult fullJoin = serverCore.joinGame(new JoinGameRequest("full-room", "Charlie"));
        assertTrue(!fullJoin.success());
        assertEquals(OperationErrorCode.GAME_FULL, fullJoin.errorCode());
        assertNull(fullJoin.snapshot());
    }

    @Test
    void duplicateGameAndPlayerNamesReturnStructuredErrors() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("shared-name", 2)).success());

        GameOperationResult duplicateGame = serverCore.createGame(new CreateGameRequest("shared-name", 2));
        assertTrue(!duplicateGame.success());
        assertEquals(OperationErrorCode.GAME_NAME_TAKEN, duplicateGame.errorCode());

        assertTrue(serverCore.joinGame(new JoinGameRequest("shared-name", "Alice")).success());
        assertTrue(serverCore.createGame(new CreateGameRequest("another-room", 2)).success());

        GameOperationResult duplicatePlayer = serverCore.joinGame(new JoinGameRequest("another-room", "Alice"));
        assertTrue(!duplicatePlayer.success());
        assertEquals(OperationErrorCode.PLAYER_NAME_TAKEN, duplicatePlayer.errorCode());
        assertNull(duplicatePlayer.snapshot());
    }

    @Test
    void heartbeatTimeoutMarksPlayerOfflineAndAllowsReconnectFromSameAddress() throws IOException {
        AtomicLong now = new AtomicLong(1_000L);
        try (MesosServerCore serverCore = testServer(now, 100L, 60_000L)) {
            assertTrue(serverCore.createGame(new CreateGameRequest("resilient-room", 3)).success());
            assertTrue(serverCore.joinGame(new JoinGameRequest("resilient-room", "Alice"), "10.0.0.1").success());
            assertTrue(serverCore.joinGame(new JoinGameRequest("resilient-room", "Bob"), "10.0.0.2").success());
            assertTrue(serverCore.joinGame(new JoinGameRequest("resilient-room", "Cara"), "10.0.0.3").success());

            LinkedBlockingQueue<GameViewUpdate> aliceUpdates = new LinkedBlockingQueue<>();
            assertTrue(serverCore.subscribe(new SubscribeGameRequest("resilient-room", "Alice"), aliceUpdates::offer).success());

            now.set(1_050L);
            assertTrue(serverCore.heartbeat(new HeartbeatRequest("resilient-room", "Alice"), "10.0.0.1").success());
            assertTrue(serverCore.heartbeat(new HeartbeatRequest("resilient-room", "Cara"), "10.0.0.3").success());

            now.set(1_110L);
            serverCore.checkConnectionsForTesting();

            GameSnapshot offlineSnapshot = serverCore.getSnapshot(new SnapshotRequest("resilient-room")).snapshot();
            assertTrue(offlineSnapshot.players().stream()
                    .filter(player -> "Bob".equals(player.nickname()))
                    .noneMatch(PlayerSummary::online));
            assertTrue(aliceUpdates.stream().anyMatch(update -> update.message().contains("Bob si e' disconnesso")));

            GameOperationResult reconnectProposal = serverCore.getReconnectProposal(
                    new ReconnectProbeRequest(),
                    "10.0.0.2"
            );
            assertNotNull(reconnectProposal.reconnectProposal());
            assertEquals("resilient-room", reconnectProposal.reconnectProposal().gameName());
            assertEquals("Bob", reconnectProposal.reconnectProposal().nickname());

            now.set(1_130L);
            GameOperationResult reconnect = serverCore.reconnectGame(
                    new ReconnectGameRequest("resilient-room", "Bob"),
                    "10.0.0.2"
            );
            assertTrue(reconnect.success());
            assertTrue(reconnect.snapshot().players().stream()
                    .filter(player -> "Bob".equals(player.nickname()))
                    .findFirst()
                    .orElseThrow()
                    .online());
        }
    }

    @Test
    void gameSuspendsWithOneOnlinePlayerAndDeclaresWinnerAfterTimeout() throws IOException {
        AtomicLong now = new AtomicLong(2_000L);
        try (MesosServerCore serverCore = testServer(now, 100L, 50L)) {
            assertTrue(serverCore.createGame(new CreateGameRequest("suspended-room", 2)).success());
            assertTrue(serverCore.joinGame(new JoinGameRequest("suspended-room", "Alice"), "10.0.0.1").success());
            assertTrue(serverCore.joinGame(new JoinGameRequest("suspended-room", "Bob"), "10.0.0.2").success());

            now.set(2_050L);
            assertTrue(serverCore.heartbeat(new HeartbeatRequest("suspended-room", "Alice"), "10.0.0.1").success());

            now.set(2_110L);
            serverCore.checkConnectionsForTesting();

            GameSnapshot suspended = serverCore.getSnapshot(new SnapshotRequest("suspended-room")).snapshot();
            assertEquals(GameStatus.SUSPENDED, suspended.status());
            assertTrue(!suspended.finished());

            now.set(2_150L);
            assertTrue(serverCore.heartbeat(new HeartbeatRequest("suspended-room", "Alice"), "10.0.0.1").success());
            now.set(2_161L);
            serverCore.checkConnectionsForTesting();

            GameSnapshot finished = serverCore.getSnapshot(new SnapshotRequest("suspended-room")).snapshot();
            assertTrue(finished.finished());
            assertEquals(GameStatus.END_GAME, finished.status());
            assertEquals("Alice", finished.winnerNickname());
        }
    }

    private MesosServerCore testServer(AtomicLong now, long heartbeatTimeoutMillis, long suspensionTimeoutMillis)
            throws IOException {
        return new MesosServerCore(
                CardLoader.load(),
                heartbeatTimeoutMillis,
                suspensionTimeoutMillis,
                10L,
                now::get,
                false
        );
    }
}
