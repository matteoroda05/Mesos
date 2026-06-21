package it.polimi.ingsw.mesos.network;

import it.polimi.ingsw.mesos.dto.game.*;
import it.polimi.ingsw.mesos.dto.lobby.*;
import it.polimi.ingsw.mesos.dto.event.*;
import it.polimi.ingsw.mesos.network.config.*;
import it.polimi.ingsw.mesos.network.protocol.*;
import it.polimi.ingsw.mesos.network.protocol.messages.*;
import it.polimi.ingsw.mesos.network.protocol.requests.*;
import it.polimi.ingsw.mesos.network.protocol.rmi.*;
import it.polimi.ingsw.mesos.network.client.*;
import it.polimi.ingsw.mesos.network.server.*;

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

import it.polimi.ingsw.mesos.controller.enums.GameStatus;
import it.polimi.ingsw.mesos.model.enums.Color;

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
        assertEquals(0, GameOperationResult.snapshotOf(createGame).joinedPlayers());

        GameOperationResult aliceJoin = aliceClient.joinGame(new JoinGameRequest(gameName, "Alice"));
        assertTrue(aliceJoin.success());
        assertEquals(GameStatus.LOGIN, GameOperationResult.snapshotOf(aliceJoin).status());
        assertEquals(1, GameOperationResult.snapshotOf(aliceJoin).joinedPlayers());

        GameOperationResult bobJoin = bobClient.joinGame(new JoinGameRequest(gameName, "Bob"));
        assertTrue(bobJoin.success());
        assertTrue(GameOperationResult.snapshotOf(bobJoin).started());
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(bobJoin).status());
        assertEquals(1, GameOperationResult.snapshotOf(bobJoin).currentRound());
        assertNotNull(GameOperationResult.snapshotOf(bobJoin).players().stream()
                .filter(player -> "Bob".equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color());

        Map<String, GameClient> clientsByNickname = Map.of(
                "Alice", aliceClient,
                "Bob", bobClient
        );

        GameSnapshot firstOfferSnapshot = GameOperationResult.snapshotOf(bobJoin);
        String firstPlayer = firstOfferSnapshot.currentPlayerNickname();
        OfferTileSummary firstOffer = firstOfferSnapshot.offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();

        GameOperationResult firstOfferResult = clientsByNickname.get(firstPlayer)
                .chooseOffer(new ChooseOfferRequest(gameName, firstPlayer, firstOffer.index()));
        assertTrue(firstOfferResult.success());
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(firstOfferResult).status());

        GameSnapshot secondOfferSnapshot = GameOperationResult.snapshotOf(firstOfferResult);
        String secondPlayer = secondOfferSnapshot.currentPlayerNickname();
        OfferTileSummary secondOffer = secondOfferSnapshot.offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();

        GameOperationResult secondOfferResult = clientsByNickname.get(secondPlayer)
                .chooseOffer(new ChooseOfferRequest(gameName, secondPlayer, secondOffer.index()));
        assertTrue(secondOfferResult.success());
        assertEquals(GameStatus.ROUND_ACTIONS, GameOperationResult.snapshotOf(secondOfferResult).status());

        GameSnapshot actionSnapshot = GameOperationResult.snapshotOf(secondOfferResult);
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
        assertTrue(GameOperationResult.snapshotOf(chooseCardResult).started());
        int rowSizeAfter = topRow ? GameOperationResult.snapshotOf(chooseCardResult).topRow().size() : GameOperationResult.snapshotOf(chooseCardResult).bottomRow().size();
        assertEquals(rowSizeBefore - 1, rowSizeAfter);

        GameOperationResult snapshotResult = aliceClient.getSnapshot(new SnapshotRequest(gameName));
        assertTrue(snapshotResult.success());
        assertEquals(gameName, GameOperationResult.snapshotOf(snapshotResult).gameName());
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
                update -> update.accept(new GameServerMessageVisitor() {
                    public void visitEventNotification(EventNotification n) {}
                    public void visitGameViewUpdate(GameViewUpdate v) { aliceUpdates.offer(v); }
                })
        );
        GameOperationResult bobSubscription = bobClient.subscribe(
                new SubscribeGameRequest(gameName, "Bob"),
                update -> update.accept(new GameServerMessageVisitor() {
                    public void visitEventNotification(EventNotification n) {}
                    public void visitGameViewUpdate(GameViewUpdate v) { bobUpdates.offer(v); }
                })
        );
        assertTrue(aliceSubscription.success());
        assertTrue(bobSubscription.success());

        Map<String, GameClient> clientsByNickname = Map.of(
                "Alice", aliceClient,
                "Bob", bobClient
        );

        GameSnapshot initialSnapshot = GameOperationResult.snapshotOf(bobSubscription);
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
        assertEquals(GameOperationResult.snapshotOf(firstOfferResult).status(), aliceFirstUpdate.snapshot().status());
        assertEquals(GameOperationResult.snapshotOf(firstOfferResult).status(), bobFirstUpdate.snapshot().status());

        GameOperationResult unsubscribeAlice = aliceClient.unsubscribe(new UnsubscribeGameRequest(gameName, "Alice"));
        assertTrue(unsubscribeAlice.success());

        String secondPlayer = GameOperationResult.snapshotOf(firstOfferResult).currentPlayerNickname();
        OfferTileSummary secondOffer = GameOperationResult.snapshotOf(firstOfferResult).offerTiles().stream()
                .filter(OfferTileSummary::available)
                .filter(tile -> tile.topPicks() + tile.bottomPicks() > 0)
                .findFirst()
                .orElseThrow();

        GameOperationResult secondOfferResult = clientsByNickname.get(secondPlayer)
                .chooseOffer(new ChooseOfferRequest(gameName, secondPlayer, secondOffer.index()));
        assertTrue(secondOfferResult.success());

        GameViewUpdate bobSecondUpdate = pollUpdate(bobUpdates);
        assertNotNull(bobSecondUpdate);
        assertEquals(GameOperationResult.snapshotOf(secondOfferResult).status(), bobSecondUpdate.snapshot().status());
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
        assertEquals(1, GameOperationResult.lobbyOf(lobby).openGames().size());
        assertEquals(openGameName, GameOperationResult.lobbyOf(lobby).openGames().getFirst().gameName());

        GameOperationResult fullJoin = observerClient.joinGame(new JoinGameRequest(fullGameName, "Charlie"));
        assertFalse(fullJoin.success());
        assertEquals(OperationErrorCode.GAME_FULL, GameOperationResult.errorCodeOf(fullJoin));
        assertNull(GameOperationResult.snapshotOf(fullJoin));
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
        assertEquals(GameStatus.LOGIN, GameOperationResult.snapshotOf(bobJoin).status());

        LinkedBlockingQueue<GameViewUpdate> bobUpdates = new LinkedBlockingQueue<>();
        assertTrue(aliceClient.subscribe(new SubscribeGameRequest(gameName, "Alice"), update -> { }).success());
        assertTrue(bobClient.subscribe(new SubscribeGameRequest(gameName, "Bob"),
                update -> update.accept(new GameServerMessageVisitor() {
                    public void visitEventNotification(EventNotification n) {}
                    public void visitGameViewUpdate(GameViewUpdate v) { bobUpdates.offer(v); }
                })).success());

        GameOperationResult aliceChooseBlue =
                aliceClient.chooseColor(new ChooseColorRequest(gameName, "Alice", Color.BLUE));
        assertTrue(aliceChooseBlue.success());
        assertEquals(Color.BLUE, playerColor(GameOperationResult.snapshotOf(aliceChooseBlue), "Alice"));
        assertEquals(java.util.List.of(Color.BLUE), GameOperationResult.snapshotOf(aliceChooseBlue).reservedColors());

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
        assertEquals(OperationErrorCode.COLOR_ALREADY_TAKEN, GameOperationResult.errorCodeOf(bobChooseBlue));
        assertEquals(Color.BLUE, playerColor(GameOperationResult.snapshotOf(bobChooseBlue), "Alice"));
        assertEquals(java.util.List.of(Color.BLUE), GameOperationResult.snapshotOf(bobChooseBlue).reservedColors());
        assertNull(playerColorOrNull(GameOperationResult.snapshotOf(bobChooseBlue), "Bob"));
        assertNull(pollUpdate(bobUpdates, 300));

        GameOperationResult carolNickname = carolClient.nickname(new NicknameRequest("Carol"));
        assertTrue(carolNickname.success());
        GameOperationResult carolChooseBlack =
                carolClient.chooseColor(new ChooseColorRequest(gameName, "Carol", Color.BLACK));
        assertTrue(carolChooseBlack.success());
        assertEquals(Color.BLUE, playerColor(GameOperationResult.snapshotOf(carolChooseBlack), "Alice"));
        assertEquals(java.util.List.of(Color.BLUE, Color.BLACK), GameOperationResult.snapshotOf(carolChooseBlack).reservedColors());

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
        assertTrue(GameOperationResult.snapshotOf(carolJoin).started());
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(carolJoin).status());
        assertEquals(Color.BLACK, playerColor(GameOperationResult.snapshotOf(carolJoin), "Carol"));
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
