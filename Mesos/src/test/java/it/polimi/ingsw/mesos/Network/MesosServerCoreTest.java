package it.polimi.ingsw.mesos.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
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
}
