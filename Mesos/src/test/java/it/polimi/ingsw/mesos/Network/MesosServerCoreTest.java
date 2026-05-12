package it.polimi.ingsw.mesos.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
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
    void missingGameOperationsReturnStructuredErrors() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        GameOperationResult snapshot = serverCore.getSnapshot(new SnapshotRequest("missing"));
        assertFalse(snapshot.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, snapshot.errorCode());

        GameOperationResult offer = serverCore.chooseOffer(new ChooseOfferRequest("missing", "Alice", 0));
        assertFalse(offer.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, offer.errorCode());

        GameOperationResult card = serverCore.chooseCard(new ChooseCardRequest("missing", "Alice", 0, true));
        assertFalse(card.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, card.errorCode());

        GameOperationResult subscribe = serverCore.subscribe(new SubscribeGameRequest("missing", "Alice"), update -> { });
        assertFalse(subscribe.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, subscribe.errorCode());

        GameOperationResult unsubscribe = serverCore.unsubscribe(new UnsubscribeGameRequest("missing", "Alice"), update -> { });
        assertFalse(unsubscribe.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, unsubscribe.errorCode());
    }

    @Test
    void operationsRejectInvalidPhasePlayerAndListener() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("guarded-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("guarded-room", "Alice")).success());

        GameOperationResult earlyOffer = serverCore.chooseOffer(new ChooseOfferRequest("guarded-room", "Alice", 0));
        assertFalse(earlyOffer.success());
        assertEquals(GameStatus.LOGIN, earlyOffer.snapshot().status());

        GameOperationResult invalidListener = serverCore.subscribe(new SubscribeGameRequest("guarded-room", "Alice"), null);
        assertFalse(invalidListener.success());
        assertEquals(OperationErrorCode.INVALID_LISTENER, invalidListener.errorCode());

        GameOperationResult unknownSubscriber = serverCore.subscribe(new SubscribeGameRequest("guarded-room", "Charlie"), update -> { });
        assertFalse(unknownSubscriber.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, unknownSubscriber.errorCode());

        GameOperationResult bobJoin = serverCore.joinGame(new JoinGameRequest("guarded-room", "Bob"));
        assertTrue(bobJoin.success());

        GameOperationResult unknownOffer = serverCore.chooseOffer(new ChooseOfferRequest("guarded-room", "Charlie", 0));
        assertFalse(unknownOffer.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, unknownOffer.errorCode());

        GameOperationResult unknownCard = serverCore.chooseCard(new ChooseCardRequest("guarded-room", "Charlie", 0, true));
        assertFalse(unknownCard.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, unknownCard.errorCode());

        GameOperationResult wrongPhaseCard = serverCore.chooseCard(new ChooseCardRequest(
                "guarded-room",
                bobJoin.snapshot().currentPlayerNickname(),
                0,
                true));
        assertFalse(wrongPhaseCard.success());
        assertEquals(GameStatus.ROUND_OFFER, wrongPhaseCard.snapshot().status());
    }

    @Test
    void failedSubscribersArePrunedAfterPublishing() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("push-prune-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("push-prune-room", "Alice")).success());
        GameOperationResult started = serverCore.joinGame(new JoinGameRequest("push-prune-room", "Bob"));
        assertTrue(started.success());

        AtomicInteger failedSubscriberCalls = new AtomicInteger();
        AtomicInteger successfulSubscriberCalls = new AtomicInteger();
        assertTrue(serverCore.subscribe(new SubscribeGameRequest("push-prune-room", "Alice"), update -> {
            failedSubscriberCalls.incrementAndGet();
            throw new IOException("disconnected test sink");
        }).success());
        assertTrue(serverCore.subscribe(new SubscribeGameRequest("push-prune-room", "Bob"), update ->
                successfulSubscriberCalls.incrementAndGet()).success());

        chooseFirstAvailableOffer(serverCore, started.snapshot());
        assertEquals(1, failedSubscriberCalls.get());
        assertEquals(1, successfulSubscriberCalls.get());

        chooseFirstAvailableOffer(serverCore, serverCore.getSnapshot(new SnapshotRequest("push-prune-room")).snapshot());
        assertEquals(1, failedSubscriberCalls.get());
        assertEquals(2, successfulSubscriberCalls.get());
    }

    private static void chooseFirstAvailableOffer(MesosServerCore serverCore, GameSnapshot snapshot) {
        OfferTileSummary offer = snapshot.offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();
        GameOperationResult result = serverCore.chooseOffer(new ChooseOfferRequest(
                snapshot.gameName(),
                snapshot.currentPlayerNickname(),
                offer.index()));
        assertTrue(result.success());
    }
}
