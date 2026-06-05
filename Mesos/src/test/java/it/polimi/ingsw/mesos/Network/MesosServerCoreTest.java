package it.polimi.ingsw.mesos.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.HeartbeatRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.NicknameRequest;
import it.polimi.ingsw.mesos.Network.Requests.PassRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;

class MesosServerCoreTest {
    @Test
    void constructorRejectsNullCardData() {
        assertThrows(IllegalArgumentException.class, () -> new MesosServerCore(null));
    }

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
        assertFalse(fullJoin.success());
        assertEquals(OperationErrorCode.GAME_FULL, fullJoin.errorCode());
        assertNull(fullJoin.snapshot());
    }

    @Test
    void duplicateGameAndNicknamesReturnStructuredErrors() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("shared-name", 2)).success());

        GameOperationResult duplicateGame = serverCore.createGame(new CreateGameRequest("shared-name", 2));
        assertFalse(duplicateGame.success());
        assertEquals(OperationErrorCode.GAME_NAME_TAKEN, duplicateGame.errorCode());

        GameOperationResult aliceNickname = serverCore.nickname(new NicknameRequest("Alice"));
        assertTrue(aliceNickname.success());
        assertFalse(aliceNickname.rejoinAvailable());

        assertTrue(serverCore.joinGame(new JoinGameRequest("shared-name", "Alice")).success());
        assertTrue(serverCore.createGame(new CreateGameRequest("another-room", 2)).success());

        GameOperationResult duplicatePlayer = serverCore.nickname(new NicknameRequest("Alice"));
        assertFalse(duplicatePlayer.success());
        assertEquals(OperationErrorCode.PLAYER_NAME_TAKEN, duplicatePlayer.errorCode());
        assertNull(duplicatePlayer.snapshot());
    }

    @Test
    void startedGameRequiresDisconnectedNicknameForExplicitRejoin() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("rejoin-room", 2)).success());
        assertTrue(serverCore.nickname(new NicknameRequest("Alice")).success());
        assertTrue(serverCore.nickname(new NicknameRequest("Bob")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("rejoin-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("rejoin-room", "Bob")).success());

        GameOperationResult repeatedJoin = serverCore.joinGame(new JoinGameRequest("rejoin-room", "Alice"));
        assertFalse(repeatedJoin.success());
        assertEquals(OperationErrorCode.GAME_FULL, repeatedJoin.errorCode());

        GameOperationResult earlyRejoin = serverCore.joinGame(new JoinGameRequest(null, "Alice", true));
        assertFalse(earlyRejoin.success());
        assertEquals(OperationErrorCode.NICKNAME_DISCONNECTED, earlyRejoin.errorCode());

        assertTrue(serverCore.heartbeat(new HeartbeatRequest("rejoin-room", "Alice", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("rejoin-room", "Bob", true)).success());
        expireHeartbeat(serverCore, "Alice");
        checkDisconnectedClients(serverCore);

        GameOperationResult nickname = serverCore.nickname(new NicknameRequest("Alice"));
        assertTrue(nickname.success());
        assertTrue(nickname.rejoinAvailable());

        GameOperationResult rejoin = serverCore.joinGame(new JoinGameRequest(null, "Alice", true));

        assertTrue(rejoin.success());
        assertEquals(GameStatus.ROUND_OFFER, rejoin.snapshot().status());
        assertEquals(2, rejoin.snapshot().joinedPlayers());
    }

    @Test
    void heartbeatValidatesLobbyAndJoinedPlayerState() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        GameOperationResult unknownLobbyHeartbeat = serverCore.heartbeat(new HeartbeatRequest("Alice"));
        assertFalse(unknownLobbyHeartbeat.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, unknownLobbyHeartbeat.errorCode());

        assertTrue(serverCore.nickname(new NicknameRequest("Alice")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("Alice")).success());

        assertTrue(serverCore.createGame(new CreateGameRequest("heartbeat-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("heartbeat-room", "Alice")).success());

        GameOperationResult joinedAsLobbyHeartbeat = serverCore.heartbeat(new HeartbeatRequest("Alice"));
        assertFalse(joinedAsLobbyHeartbeat.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, joinedAsLobbyHeartbeat.errorCode());

        GameOperationResult missingGameHeartbeat = serverCore.heartbeat(new HeartbeatRequest("missing-room", "Alice", true));
        assertFalse(missingGameHeartbeat.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, missingGameHeartbeat.errorCode());

        GameOperationResult wrongPlayerHeartbeat = serverCore.heartbeat(new HeartbeatRequest("heartbeat-room", "Bob", true));
        assertFalse(wrongPlayerHeartbeat.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, wrongPlayerHeartbeat.errorCode());
    }

    @Test
    void passReturnsGameNotFoundForMissingSession() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        GameOperationResult result = serverCore.pass(new PassRequest("missing-pass-room", "Alice"));

        assertFalse(result.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, result.errorCode());
        assertNull(result.snapshot());
    }

    @SuppressWarnings("unchecked")
    private static void expireHeartbeat(MesosServerCore serverCore, String nickname) throws Exception {
        Field heartbeats = MesosServerCore.class.getDeclaredField("lastHeartbeatByNickname");
        heartbeats.setAccessible(true);
        ((Map<String, Long>) heartbeats.get(serverCore)).put(
                nickname,
                System.currentTimeMillis() - NetworkDefaults.HEARTBEAT_TIMEOUT_MILLIS - 1
        );
    }

    private static void checkDisconnectedClients(MesosServerCore serverCore) throws Exception {
        Method checkDisconnectedClients = MesosServerCore.class.getDeclaredMethod("checkDisconnectedClients");
        checkDisconnectedClients.setAccessible(true);
        try {
            checkDisconnectedClients.invoke(serverCore);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
    }
}
