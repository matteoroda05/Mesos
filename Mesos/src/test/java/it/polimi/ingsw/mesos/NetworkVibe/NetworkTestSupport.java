package it.polimi.ingsw.mesos.NetworkVibe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;

final class NetworkTestSupport {
    private NetworkTestSupport() {
    }

    static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    static void assertBasicRemoteTurn(GameClient aliceClient, GameClient bobClient, String gameName) throws IOException {
        GameOperationResult createGame = aliceClient.createGame(new CreateGameRequest(gameName, 2));
        assertTrue(createGame.success());
        assertEquals(0, createGame.snapshot().joinedPlayers());

        GameOperationResult aliceJoin = aliceClient.joinGame(new JoinGameRequest(gameName, "Alice"));
        assertTrue(aliceJoin.success());
        assertEquals(GameStatus.LOGIN, aliceJoin.snapshot().status());
        assertEquals(1, aliceJoin.snapshot().joinedPlayers());

        GameOperationResult bobJoin = bobClient.joinGame(new JoinGameRequest(gameName, "Bob"));
        assertTrue(bobJoin.success());
        assertTrue(bobJoin.snapshot().started());
        assertEquals(GameStatus.ROUND_OFFER, bobJoin.snapshot().status());
        assertEquals(1, bobJoin.snapshot().currentRound());
        assertNotNull(bobJoin.assignedColor());

        Map<String, GameClient> clientsByNickname = Map.of(
                "Alice", aliceClient,
                "Bob", bobClient
        );

        GameSnapshot firstOfferSnapshot = bobJoin.snapshot();
        String firstPlayer = firstOfferSnapshot.currentPlayerNickname();
        OfferTileSummary firstOffer = firstOfferSnapshot.offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();

        GameOperationResult firstOfferResult = clientsByNickname.get(firstPlayer)
                .chooseOffer(new ChooseOfferRequest(gameName, firstPlayer, firstOffer.index()));
        assertTrue(firstOfferResult.success());
        assertEquals(GameStatus.ROUND_OFFER, firstOfferResult.snapshot().status());

        GameSnapshot secondOfferSnapshot = firstOfferResult.snapshot();
        String secondPlayer = secondOfferSnapshot.currentPlayerNickname();
        OfferTileSummary secondOffer = secondOfferSnapshot.offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();

        GameOperationResult secondOfferResult = clientsByNickname.get(secondPlayer)
                .chooseOffer(new ChooseOfferRequest(gameName, secondPlayer, secondOffer.index()));
        assertTrue(secondOfferResult.success());
        assertEquals(GameStatus.ROUND_ACTIONS, secondOfferResult.snapshot().status());

        GameSnapshot actionSnapshot = secondOfferResult.snapshot();
        String actingPlayer = actionSnapshot.currentPlayerNickname();
        OfferTileSummary actingTile = actionSnapshot.offerTiles().stream()
                .filter(tile -> actingPlayer.equals(tile.occupantNickname()))
                .findFirst()
                .orElseThrow();

        boolean topRow = actingTile.topPicks() > 0;
        CardSummary chosenCard = (topRow ? actionSnapshot.topRow() : actionSnapshot.bottomRow()).stream()
                .filter(card -> !card.event())
                .filter(card -> card.foodCost() == null)
                .findFirst()
                .orElseThrow();

        int rowSizeBefore = topRow ? actionSnapshot.topRow().size() : actionSnapshot.bottomRow().size();
        GameOperationResult chooseCardResult = clientsByNickname.get(actingPlayer)
                .chooseCard(new ChooseCardRequest(gameName, actingPlayer, chosenCard.selectionIndex(), topRow));

        assertTrue(chooseCardResult.success());
        assertTrue(chooseCardResult.snapshot().started());
        int rowSizeAfter = topRow ? chooseCardResult.snapshot().topRow().size() : chooseCardResult.snapshot().bottomRow().size();
        assertEquals(rowSizeBefore - 1, rowSizeAfter);

        GameOperationResult snapshotResult = aliceClient.getSnapshot(new SnapshotRequest(gameName));
        assertTrue(snapshotResult.success());
        assertEquals(gameName, snapshotResult.snapshot().gameName());
    }

    static void assertPushUpdates(GameClient aliceClient, GameClient bobClient, String gameName) throws IOException {
        GameOperationResult createGame = aliceClient.createGame(new CreateGameRequest(gameName, 2));
        assertTrue(createGame.success());

        GameOperationResult aliceJoin = aliceClient.joinGame(new JoinGameRequest(gameName, "Alice"));
        assertTrue(aliceJoin.success());
        GameOperationResult bobJoin = bobClient.joinGame(new JoinGameRequest(gameName, "Bob"));
        assertTrue(bobJoin.success());

        LinkedBlockingQueue<GameViewUpdate> aliceUpdates = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<GameViewUpdate> bobUpdates = new LinkedBlockingQueue<>();

        GameOperationResult aliceSubscription = aliceClient.subscribe(
                new SubscribeGameRequest(gameName, "Alice"),
                aliceUpdates::offer
        );
        GameOperationResult bobSubscription = bobClient.subscribe(
                new SubscribeGameRequest(gameName, "Bob"),
                bobUpdates::offer
        );
        assertTrue(aliceSubscription.success());
        assertTrue(bobSubscription.success());

        Map<String, GameClient> clientsByNickname = Map.of(
                "Alice", aliceClient,
                "Bob", bobClient
        );

        GameSnapshot initialSnapshot = bobSubscription.snapshot();
        String firstPlayer = initialSnapshot.currentPlayerNickname();
        OfferTileSummary firstOffer = initialSnapshot.offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();

        GameOperationResult firstOfferResult = clientsByNickname.get(firstPlayer)
                .chooseOffer(new ChooseOfferRequest(gameName, firstPlayer, firstOffer.index()));
        assertTrue(firstOfferResult.success());

        GameViewUpdate aliceFirstUpdate = pollUpdate(aliceUpdates);
        GameViewUpdate bobFirstUpdate = pollUpdate(bobUpdates);
        assertNotNull(aliceFirstUpdate);
        assertNotNull(bobFirstUpdate);
        assertEquals(gameName, aliceFirstUpdate.gameName());
        assertEquals(firstOfferResult.snapshot().status(), aliceFirstUpdate.snapshot().status());
        assertEquals(firstOfferResult.snapshot().status(), bobFirstUpdate.snapshot().status());

        GameOperationResult unsubscribeAlice = aliceClient.unsubscribe(new UnsubscribeGameRequest(gameName, "Alice"));
        assertTrue(unsubscribeAlice.success());

        String secondPlayer = firstOfferResult.snapshot().currentPlayerNickname();
        OfferTileSummary secondOffer = firstOfferResult.snapshot().offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();

        GameOperationResult secondOfferResult = clientsByNickname.get(secondPlayer)
                .chooseOffer(new ChooseOfferRequest(gameName, secondPlayer, secondOffer.index()));
        assertTrue(secondOfferResult.success());

        GameViewUpdate bobSecondUpdate = pollUpdate(bobUpdates);
        assertNotNull(bobSecondUpdate);
        assertEquals(secondOfferResult.snapshot().status(), bobSecondUpdate.snapshot().status());
        assertNull(pollUpdate(aliceUpdates, 300));
    }

    private static GameViewUpdate pollUpdate(LinkedBlockingQueue<GameViewUpdate> queue) {
        return pollUpdate(queue, 2_000);
    }

    private static GameViewUpdate pollUpdate(LinkedBlockingQueue<GameViewUpdate> queue, long timeoutMillis) {
        try {
            return queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for a pushed update", exception);
        }
    }
}
