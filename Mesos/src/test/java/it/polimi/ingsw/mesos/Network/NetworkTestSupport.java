package it.polimi.ingsw.mesos.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

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
        assertNotNull(bobJoin.snapshot().players().stream()
                .filter(player -> "Bob".equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color());

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

    static void assertLobbyVisibilityAndFullGameRejection(
            GameClient aliceClient,
            GameClient bobClient,
            GameClient observerClient,
            String openGameName,
            String fullGameName
    ) throws IOException {
        assertTrue(aliceClient.createGame(new CreateGameRequest(openGameName, 3)).success());
        assertTrue(aliceClient.createGame(new CreateGameRequest(fullGameName, 2)).success());
        assertTrue(aliceClient.joinGame(new JoinGameRequest(fullGameName, "Alice")).success());
        assertTrue(bobClient.joinGame(new JoinGameRequest(fullGameName, "Bob")).success());

        GameOperationResult lobby = observerClient.getLobby();
        assertTrue(lobby.success());
        assertEquals(1, lobby.lobbySnapshot().openGames().size());
        assertEquals(openGameName, lobby.lobbySnapshot().openGames().getFirst().gameName());

        GameOperationResult fullJoin = observerClient.joinGame(new JoinGameRequest(fullGameName, "Charlie"));
        assertFalse(fullJoin.success());
        assertEquals(OperationErrorCode.GAME_FULL, fullJoin.errorCode());
        assertNull(fullJoin.snapshot());
    }

    static void assertWaitingRoomColorSelection(
            GameClient aliceClient,
            GameClient bobClient,
            GameClient carolClient,
            String gameName
    ) throws IOException {
        GameOperationResult createGame = aliceClient.createGame(new CreateGameRequest(gameName, 3));
        assertTrue(createGame.success());

        GameOperationResult aliceJoin = aliceClient.joinGame(new JoinGameRequest(gameName, "Alice"));
        assertTrue(aliceJoin.success());
        GameOperationResult bobJoin = bobClient.joinGame(new JoinGameRequest(gameName, "Bob"));
        assertTrue(bobJoin.success());
        assertEquals(GameStatus.LOGIN, bobJoin.snapshot().status());

        LinkedBlockingQueue<GameViewUpdate> bobUpdates = new LinkedBlockingQueue<>();
        assertTrue(aliceClient.subscribe(new SubscribeGameRequest(gameName, "Alice"), update -> { }).success());
        assertTrue(bobClient.subscribe(new SubscribeGameRequest(gameName, "Bob"), bobUpdates::offer).success());

        GameOperationResult aliceChooseBlue =
                aliceClient.chooseColor(new ChooseColorRequest(gameName, "Alice", Color.BLUE));
        assertTrue(aliceChooseBlue.success());
        assertEquals(Color.BLUE, playerColor(aliceChooseBlue.snapshot(), "Alice"));
        assertEquals(java.util.List.of(Color.BLUE), aliceChooseBlue.snapshot().reservedColors());

        GameViewUpdate aliceBlueUpdate = pollUpdateMatching(
                bobUpdates,
                update -> update.snapshot() != null && Color.BLUE.equals(playerColor(update.snapshot(), "Alice"))
        );
        assertNotNull(aliceBlueUpdate);
        assertEquals(gameName, aliceBlueUpdate.gameName());
        assertEquals(Color.BLUE, playerColor(aliceBlueUpdate.snapshot(), "Alice"));

        GameOperationResult bobChooseBlue =
                bobClient.chooseColor(new ChooseColorRequest(gameName, "Bob", Color.BLUE));
        assertFalse(bobChooseBlue.success());
        assertEquals(OperationErrorCode.COLOR_ALREADY_TAKEN, bobChooseBlue.errorCode());
        assertEquals(Color.BLUE, playerColor(bobChooseBlue.snapshot(), "Alice"));
        assertEquals(java.util.List.of(Color.BLUE), bobChooseBlue.snapshot().reservedColors());
        assertNull(playerColorOrNull(bobChooseBlue.snapshot(), "Bob"));
        assertNull(pollUpdate(bobUpdates, 300));

        GameOperationResult carolNickname = carolClient.nickname(new NicknameRequest("Carol"));
        assertTrue(carolNickname.success());
        GameOperationResult carolChooseBlack =
                carolClient.chooseColor(new ChooseColorRequest(gameName, "Carol", Color.BLACK));
        assertTrue(carolChooseBlack.success());
        assertEquals(Color.BLUE, playerColor(carolChooseBlack.snapshot(), "Alice"));
        assertEquals(java.util.List.of(Color.BLUE, Color.BLACK), carolChooseBlack.snapshot().reservedColors());

        GameViewUpdate carolReservedUpdate = pollUpdateMatching(
                bobUpdates,
                update -> update.message() != null && update.message().contains("Carol")
        );
        assertNotNull(carolReservedUpdate);
        assertEquals(gameName, carolReservedUpdate.gameName());
        assertNotNull(carolReservedUpdate.snapshot());
        assertFalse(carolReservedUpdate.snapshot().started());
        assertEquals(2, carolReservedUpdate.snapshot().joinedPlayers());
        assertEquals(Color.BLUE, playerColor(carolReservedUpdate.snapshot(), "Alice"));
        assertEquals(java.util.List.of(Color.BLUE, Color.BLACK), carolReservedUpdate.snapshot().reservedColors());
        assertNull(playerColorOrNull(carolReservedUpdate.snapshot(), "Bob"));

        GameOperationResult carolJoin = carolClient.joinGame(new JoinGameRequest(gameName, "Carol"));
        assertTrue(carolJoin.success());
        assertTrue(carolJoin.snapshot().started());
        assertEquals(GameStatus.ROUND_OFFER, carolJoin.snapshot().status());
        assertEquals(Color.BLACK, playerColor(carolJoin.snapshot(), "Carol"));
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

    private static GameViewUpdate pollUpdateMatching(
            LinkedBlockingQueue<GameViewUpdate> queue,
            Predicate<GameViewUpdate> predicate
    ) {
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            GameViewUpdate update = pollUpdate(queue, deadline - System.currentTimeMillis());
            if (update == null) {
                return null;
            }
            if (predicate.test(update)) {
                return update;
            }
        }
        return null;
    }

    private static Color playerColor(GameSnapshot snapshot, String nickname) {
        return snapshot.players().stream()
                .filter(player -> nickname.equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color();
    }

    private static Color playerColorOrNull(GameSnapshot snapshot, String nickname) {
        return snapshot.players().stream()
                .filter(player -> nickname.equals(player.nickname()))
                .findFirst()
                .map(PlayerSummary::color)
                .orElse(null);
    }
}
