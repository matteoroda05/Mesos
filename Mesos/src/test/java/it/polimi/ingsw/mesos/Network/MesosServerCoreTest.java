package it.polimi.ingsw.mesos.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertNotNull(secondJoin.assignedColor());

        GameOperationResult snapshot = serverCore.getSnapshot(new SnapshotRequest("local-core"));
        assertTrue(snapshot.success());
        assertEquals(2, snapshot.snapshot().players().size());
    }
}
