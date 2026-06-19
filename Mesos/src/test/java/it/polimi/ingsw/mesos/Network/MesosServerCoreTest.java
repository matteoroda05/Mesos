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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseColorRequest;
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
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;

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
    void offerTileOccupantsStayIsolatedBetweenGames() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("room-a", 2)).success());
        assertTrue(serverCore.createGame(new CreateGameRequest("room-b", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("room-a", "Alice")).success());
        GameOperationResult roomAStarted = serverCore.joinGame(new JoinGameRequest("room-a", "Bob"));
        assertTrue(roomAStarted.success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("room-b", "Carol")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("room-b", "Dana")).success());

        GameOperationResult roomAOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("room-a", roomAStarted.snapshot().currentPlayerNickname(), 0));
        assertTrue(roomAOffer.success());

        GameOperationResult roomBSnapshot = serverCore.getSnapshot(new SnapshotRequest("room-b"));

        assertTrue(roomBSnapshot.success());
        assertTrue(roomBSnapshot.snapshot().offerTiles().stream().allMatch(OfferTileSummary::available));
        assertTrue(roomBSnapshot.snapshot().offerTiles().stream().allMatch(tile -> tile.occupantColor() == null));
        assertTrue(roomBSnapshot.snapshot().offerTiles().stream().allMatch(tile -> tile.occupantNickname() == null));
    }

    @Test
    void reservedColorBeforeFinalJoinIsAppliedWhenTheGameStarts() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("color-start-room", 2)).success());
        assertTrue(serverCore.nickname(new NicknameRequest("Alice")).success());
        assertTrue(serverCore.nickname(new NicknameRequest("Bob")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("color-start-room", "Alice")).success());

        GameOperationResult reserveColor =
                serverCore.chooseColor(new ChooseColorRequest("color-start-room", "Bob", Color.BLACK));

        assertTrue(reserveColor.success());
        assertEquals(GameStatus.LOGIN, reserveColor.snapshot().status());
        assertEquals(1, reserveColor.snapshot().joinedPlayers());
        assertEquals(java.util.List.of(Color.BLACK), reserveColor.snapshot().reservedColors());

        GameOperationResult finalJoin = serverCore.joinGame(new JoinGameRequest("color-start-room", "Bob"));

        assertTrue(finalJoin.success());
        assertEquals(GameStatus.ROUND_OFFER, finalJoin.snapshot().status());
        assertEquals(Color.BLACK, finalJoin.snapshot().players().stream()
                .filter(player -> "Bob".equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color());
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

    @Test
    void passRejectsPassOutsideOptionalAfterEveryoneState() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("pass-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("pass-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("pass-room", "Bob")).success());

        GameOperationResult result = serverCore.pass(new PassRequest("pass-room", "Alice"));

        assertFalse(result.success());
        assertNotNull(result.snapshot());
        assertEquals(GameStatus.ROUND_OFFER, result.snapshot().status());
        assertTrue(result.message().toLowerCase().contains("pass"));
    }

    @Test
    void passSucceedsDuringOptionalAfterEveryoneChoice() throws Exception {
        MesosServerCore serverCore = new MesosServerCore(singleTopPickCardData(2));

        assertTrue(serverCore.createGame(new CreateGameRequest("pass-extra-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("pass-extra-room", "Alice")).success());
        GameOperationResult secondJoin = serverCore.joinGame(new JoinGameRequest("pass-extra-room", "Bob"));
        assertTrue(secondJoin.success());

        String firstPlayer = secondJoin.snapshot().currentPlayerNickname();
        String secondPlayer = "Alice".equals(firstPlayer) ? "Bob" : "Alice";
        giveAfterEveryoneBuilding(serverCore, "pass-extra-room", firstPlayer);
        giveFood(serverCore, "pass-extra-room", 20);

        GameOperationResult firstOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("pass-extra-room", firstPlayer, 0));
        assertTrue(firstOffer.success());

        GameOperationResult secondOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("pass-extra-room", secondPlayer, 1));
        assertTrue(secondOffer.success());
        assertNotNull(secondOffer.snapshot());
        assertEquals(GameStatus.ROUND_ACTIONS, secondOffer.snapshot().status());
        assertEquals(1, secondOffer.snapshot().currentRound());
        assertFalse(secondOffer.snapshot().passAvailable());

        GameOperationResult firstAction =
                serverCore.chooseCard(new ChooseCardRequest(
                        "pass-extra-room",
                        firstPlayer,
                        firstPickableTopCardIndex(secondOffer.snapshot()),
                        true));
        assertTrue(firstAction.success());
        assertNotNull(firstAction.snapshot());
        assertEquals(secondPlayer, firstAction.snapshot().currentPlayerNickname());
        assertFalse(firstAction.snapshot().passAvailable());

        GameOperationResult secondAction =
                serverCore.chooseCard(new ChooseCardRequest(
                        "pass-extra-room",
                        secondPlayer,
                        firstPickableTopCardIndex(firstAction.snapshot()),
                        true));
        assertTrue(secondAction.success());
        assertNotNull(secondAction.snapshot());
        assertEquals(GameStatus.ROUND_ACTIONS, secondAction.snapshot().status());
        assertEquals(firstPlayer, secondAction.snapshot().currentPlayerNickname());
        assertEquals(1, secondAction.snapshot().currentRound());
        assertTrue(secondAction.snapshot().passAvailable());
        String extraPlayer = secondAction.snapshot().currentPlayerNickname();
        String otherPlayer = "Alice".equals(extraPlayer) ? "Bob" : "Alice";

        GameOperationResult wrongPlayerPass =
                serverCore.pass(new PassRequest("pass-extra-room", otherPlayer));
        assertFalse(wrongPlayerPass.success());

        GameOperationResult passResult =
                serverCore.pass(new PassRequest("pass-extra-room", extraPlayer));

        assertTrue(passResult.success());
        assertNotNull(passResult.snapshot());
        assertEquals(GameStatus.ROUND_OFFER, passResult.snapshot().status());
        assertEquals(2, passResult.snapshot().currentRound());
        assertFalse(passResult.snapshot().passAvailable());
    }

    @Test
    void optionalAfterEveryoneChoiceAllowsOnlyOneExtraTopCard() throws Exception {
        MesosServerCore serverCore = new MesosServerCore(singleTopPickCardData(2));

        assertTrue(serverCore.createGame(new CreateGameRequest("one-extra-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("one-extra-room", "Alice")).success());
        GameOperationResult secondJoin = serverCore.joinGame(new JoinGameRequest("one-extra-room", "Bob"));
        assertTrue(secondJoin.success());

        String firstPlayer = secondJoin.snapshot().currentPlayerNickname();
        String secondPlayer = "Alice".equals(firstPlayer) ? "Bob" : "Alice";
        giveAfterEveryoneBuilding(serverCore, "one-extra-room", firstPlayer);
        giveFood(serverCore, "one-extra-room", 20);

        assertTrue(serverCore.chooseOffer(new ChooseOfferRequest("one-extra-room", firstPlayer, 0)).success());
        GameOperationResult secondOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("one-extra-room", secondPlayer, 1));
        assertTrue(secondOffer.success());
        assertTrue(serverCore.chooseCard(new ChooseCardRequest(
                "one-extra-room",
                firstPlayer,
                firstPickableTopCardIndex(secondOffer.snapshot()),
                true
        )).success());

        GameOperationResult secondAction =
                serverCore.chooseCard(new ChooseCardRequest(
                        "one-extra-room",
                        secondPlayer,
                        firstPickableTopCardIndex(serverCore.getSnapshot(new SnapshotRequest("one-extra-room")).snapshot()),
                        true
                ));
        assertTrue(secondAction.success());
        assertTrue(secondAction.snapshot().passAvailable());
        assertEquals(firstPlayer, secondAction.snapshot().currentPlayerNickname());

        GameOperationResult extraPick =
                serverCore.chooseCard(new ChooseCardRequest(
                        "one-extra-room",
                        firstPlayer,
                        firstPickableTopCardIndex(secondAction.snapshot()),
                        true
                ));
        assertTrue(extraPick.success());
        assertNotNull(extraPick.snapshot());
        assertEquals(GameStatus.ROUND_OFFER, extraPick.snapshot().status());
        assertEquals(2, extraPick.snapshot().currentRound());
        assertFalse(extraPick.snapshot().passAvailable());

        GameOperationResult secondExtraPick =
                serverCore.chooseCard(new ChooseCardRequest("one-extra-room", firstPlayer, 0, true));
        assertFalse(secondExtraPick.success());
    }

    @Test
    void disconnectedActivePlayerIsSkippedThroughResiliencePipelineDuringRoundActions() throws Exception {
        MesosServerCore serverCore = new MesosServerCore(singleTopPickCardData(3));

        assertTrue(serverCore.createGame(new CreateGameRequest("disconnect-actions-room", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("disconnect-actions-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("disconnect-actions-room", "Bob")).success());
        GameOperationResult thirdJoin =
                serverCore.joinGame(new JoinGameRequest("disconnect-actions-room", "Carol"));
        assertTrue(thirdJoin.success());

        String firstPlayer = thirdJoin.snapshot().currentPlayerNickname();

        assertTrue(serverCore.heartbeat(new HeartbeatRequest("disconnect-actions-room", "Alice", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("disconnect-actions-room", "Bob", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("disconnect-actions-room", "Carol", true)).success());

        GameOperationResult firstOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("disconnect-actions-room", firstPlayer, 0));
        assertTrue(firstOffer.success());
        String secondPlayer = firstOffer.snapshot().currentPlayerNickname();

        GameOperationResult secondOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("disconnect-actions-room", secondPlayer, 1));
        assertTrue(secondOffer.success());
        String thirdPlayer = secondOffer.snapshot().currentPlayerNickname();

        GameOperationResult thirdOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("disconnect-actions-room", thirdPlayer, 2));
        assertTrue(thirdOffer.success());
        assertEquals(GameStatus.ROUND_ACTIONS, thirdOffer.snapshot().status());
        assertEquals(firstPlayer, thirdOffer.snapshot().currentPlayerNickname());

        expireHeartbeat(serverCore, firstPlayer);
        checkDisconnectedClients(serverCore);

        GameOperationResult snapshot =
                serverCore.getSnapshot(new SnapshotRequest("disconnect-actions-room"));
        assertTrue(snapshot.success());
        assertNotNull(snapshot.snapshot());
        assertEquals(GameStatus.ROUND_ACTIONS, snapshot.snapshot().status());
        assertEquals(secondPlayer, snapshot.snapshot().currentPlayerNickname());
        assertFalse(snapshot.snapshot().passAvailable());
    }

    @Test
    void chooseColorReservesWaitingRoomColorRejectsDuplicatesAndAppearsInPreStartSnapshot() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("color-room", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("color-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("color-room", "Bob")).success());

        GameOperationResult chooseColor = serverCore.chooseColor(new ChooseColorRequest("color-room", "Alice", Color.BLACK));

        assertTrue(chooseColor.success());
        assertEquals(Color.BLACK, chooseColor.snapshot().players().stream()
                .filter(player -> "Alice".equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color());
        assertEquals(java.util.List.of(Color.BLACK), chooseColor.snapshot().reservedColors());

        GameOperationResult duplicateColor = serverCore.chooseColor(new ChooseColorRequest("color-room", "Bob", Color.BLACK));

        assertFalse(duplicateColor.success());
        assertEquals(OperationErrorCode.COLOR_ALREADY_TAKEN, duplicateColor.errorCode());
        assertEquals(java.util.List.of(Color.BLACK), duplicateColor.snapshot().reservedColors());
    }

    @Test
    void chooseColorRejectsSelectionAfterGameStart() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("started-color-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("started-color-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("started-color-room", "Bob")).success());

        GameOperationResult lateChoice = serverCore.chooseColor(new ChooseColorRequest("started-color-room", "Alice", Color.YELLOW));

        assertFalse(lateChoice.success());
        assertEquals(OperationErrorCode.COLOR_SELECTION_CLOSED, lateChoice.errorCode());
    }

    @Test
    void chooseColorMarksDisconnectedNicknameAsConnectedAgain() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("liveness-room", 3)).success());
        assertTrue(serverCore.nickname(new NicknameRequest("Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("liveness-room", "Alice")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("liveness-room", "Alice", true)).success());

        expireHeartbeat(serverCore, "Alice");
        checkDisconnectedClients(serverCore);

        GameOperationResult rejoinAvailable = serverCore.nickname(new NicknameRequest("Alice"));
        assertTrue(rejoinAvailable.success());
        assertTrue(rejoinAvailable.rejoinAvailable());

        GameOperationResult chooseColor =
                serverCore.chooseColor(new ChooseColorRequest("liveness-room", "Alice", Color.YELLOW));

        assertTrue(chooseColor.success());

        GameOperationResult takenAgain = serverCore.nickname(new NicknameRequest("Alice"));
        assertFalse(takenAgain.success());
        assertEquals(OperationErrorCode.PLAYER_NAME_TAKEN, takenAgain.errorCode());
    }

    @Test
    void operationsOnMissingGameReturnGameNotFound() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                serverCore.getSnapshot(new SnapshotRequest("nope")).errorCode());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                serverCore.chooseOffer(new ChooseOfferRequest("nope", "Alice", 0)).errorCode());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                serverCore.chooseCard(new ChooseCardRequest("nope", "Alice", 0, true)).errorCode());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                serverCore.chooseColor(new ChooseColorRequest("nope", "Alice", Color.BLUE)).errorCode());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                serverCore.subscribe(new SubscribeGameRequest("nope", "Alice"), update -> { }).errorCode());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                serverCore.unsubscribe(new UnsubscribeGameRequest("nope", "Alice"), update -> { }).errorCode());
    }

    @Test
    void commandsBeforeGameStartAreRejected() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("waiting-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("waiting-room", "Alice")).success());

        assertFalse(serverCore.chooseOffer(new ChooseOfferRequest("waiting-room", "Alice", 0)).success());
        assertFalse(serverCore.chooseCard(new ChooseCardRequest("waiting-room", "Alice", 0, true)).success());
        assertFalse(serverCore.pass(new PassRequest("waiting-room", "Alice")).success());
    }

    @Test
    void startedGameRejectsCommandsFromUnknownPlayers() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("started-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("started-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("started-room", "Bob")).success());

        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                serverCore.chooseOffer(new ChooseOfferRequest("started-room", "Mallory", 0)).errorCode());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                serverCore.chooseCard(new ChooseCardRequest("started-room", "Mallory", 0, true)).errorCode());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                serverCore.pass(new PassRequest("started-room", "Mallory")).errorCode());
    }

    @Test
    void chooseColorBeforeStartRejectsNonMemberNickname() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("color-wait", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("color-wait", "Alice")).success());

        GameOperationResult result =
                serverCore.chooseColor(new ChooseColorRequest("color-wait", "Stranger", Color.BLUE));

        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, result.errorCode());
    }

    @Test
    void subscribeRejectsNullSinkAndNonMembersThenUnsubscribes() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("subs-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("subs-room", "Alice")).success());

        assertEquals(OperationErrorCode.INVALID_LISTENER,
                serverCore.subscribe(new SubscribeGameRequest("subs-room", "Alice"), null).errorCode());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                serverCore.subscribe(new SubscribeGameRequest("subs-room", "Ghost"), update -> { }).errorCode());

        GameUpdateSink sink = update -> { };
        assertTrue(serverCore.subscribe(new SubscribeGameRequest("subs-room", "Alice"), sink).success());
        assertTrue(serverCore.unsubscribe(new UnsubscribeGameRequest("subs-room", "Alice"), sink).success());
        assertTrue(serverCore.unsubscribe(new UnsubscribeGameRequest("subs-room", "Alice"), null).success());
    }

    @Test
    void singleConnectedPlayerSuspendsCommandsThenForcedWinAfterTimeout() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("resilience-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("resilience-room", "Alice")).success());
        GameOperationResult started = serverCore.joinGame(new JoinGameRequest("resilience-room", "Bob"));
        assertTrue(started.success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("resilience-room", "Alice", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("resilience-room", "Bob", true)).success());

        expireHeartbeat(serverCore, "Bob");
        checkDisconnectedClients(serverCore);

        GameOperationResult suspended =
                serverCore.chooseOffer(new ChooseOfferRequest("resilience-room", "Alice", 0));
        assertFalse(suspended.success());
        assertTrue(suspended.message().toLowerCase().contains("suspended"));
        assertFalse(serverCore.chooseCard(new ChooseCardRequest("resilience-room", "Alice", 0, true)).success());
        assertFalse(serverCore.pass(new PassRequest("resilience-room", "Alice")).success());

        forceSingleConnectedTimeout(serverCore, "resilience-room");
        checkDisconnectedClients(serverCore);

        GameOperationResult snapshot = serverCore.getSnapshot(new SnapshotRequest("resilience-room"));
        assertTrue(snapshot.success());
        assertTrue(snapshot.snapshot().finished());
        assertEquals(GameStatus.END_GAME, snapshot.snapshot().status());
        List<String> forcedWinners = snapshot.snapshot().players().stream()
                .filter(PlayerSummary::winner)
                .map(PlayerSummary::nickname)
                .toList();
        assertEquals(List.of("Alice"), forcedWinners);

        assertFalse(serverCore.chooseOffer(new ChooseOfferRequest("resilience-room", "Alice", 0)).success());
        assertTrue(serverCore.unsubscribe(
                new UnsubscribeGameRequest("resilience-room", "Alice"), update -> { }).success());
    }

    @Test
    void joinAndRejoinEdgeCasesAreHandled() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();

        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                serverCore.joinGame(new JoinGameRequest("ghost-room", "Alice")).errorCode());

        assertTrue(serverCore.nickname(new NicknameRequest("Lobster")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("Lobster")).success());
        expireHeartbeat(serverCore, "Lobster");
        checkDisconnectedClients(serverCore);
        assertTrue(serverCore.joinGame(new JoinGameRequest(null, "Lobster", true)).success());

        assertTrue(serverCore.createGame(new CreateGameRequest("room-a", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("room-a", "Cara")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("room-a", "Cara", true)).success());
        expireHeartbeat(serverCore, "Cara");
        checkDisconnectedClients(serverCore);
        assertTrue(serverCore.createGame(new CreateGameRequest("room-b", 2)).success());
        assertEquals(OperationErrorCode.NICKNAME_DISCONNECTED,
                serverCore.joinGame(new JoinGameRequest("room-b", "Cara")).errorCode());
    }

    @Test
    void publishRemovesSubscriberThatThrowsOnDelivery() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("publish-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("publish-room", "Alice")).success());

        GameUpdateSink throwingSink = update -> { throw new IOException("boom"); };
        assertTrue(serverCore.subscribe(new SubscribeGameRequest("publish-room", "Alice"), throwingSink).success());

        assertTrue(serverCore.joinGame(new JoinGameRequest("publish-room", "Bob")).success());
    }

    @Test
    void chooseColorAllowsReReservingTheSameColor() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("recolor-room", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("recolor-room", "Alice")).success());

        assertTrue(serverCore.chooseColor(new ChooseColorRequest("recolor-room", "Alice", Color.BLUE)).success());
        assertTrue(serverCore.chooseColor(new ChooseColorRequest("recolor-room", "Alice", Color.BLUE)).success());
    }

    @Test
    void allPlayersDisconnectedResetsSingleConnectedTracking() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("alloff-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("alloff-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("alloff-room", "Bob")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("alloff-room", "Alice", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("alloff-room", "Bob", true)).success());

        expireHeartbeat(serverCore, "Alice");
        expireHeartbeat(serverCore, "Bob");
        checkDisconnectedClients(serverCore);

        assertTrue(serverCore.getSnapshot(new SnapshotRequest("alloff-room")).success());
    }

    @Test
    void naturallyFinishedGameReportsEndGameStatusAndWinner() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("finished-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("finished-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("finished-room", "Bob")).success());

        finishGameNaturally(serverCore, "finished-room");

        GameOperationResult snapshot = serverCore.getSnapshot(new SnapshotRequest("finished-room"));
        assertTrue(snapshot.success());
        assertTrue(snapshot.snapshot().finished());
        assertEquals(GameStatus.END_GAME, snapshot.snapshot().status());
        assertTrue(snapshot.snapshot().players().stream().anyMatch(PlayerSummary::winner));

        // disconnecting a player in a finished game must not touch model connection state
        expireHeartbeat(serverCore, "Alice");
        checkDisconnectedClients(serverCore);
        assertTrue(serverCore.getSnapshot(new SnapshotRequest("finished-room")).snapshot().finished());
    }

    @Test
    void prestigeTieIsBrokenByFoodYieldingSingleWinner() throws Exception {
        MesosServerCore serverCore = finishedTwoPlayerGame("food-tiebreak-room");
        setPlayerScore(serverCore, "food-tiebreak-room", "Alice", 10, 5);
        setPlayerScore(serverCore, "food-tiebreak-room", "Bob", 10, 8);

        assertEquals(List.of("Bob"), winnerNicknames(serverCore, "food-tiebreak-room"));
    }

    @Test
    void fullTieOnPrestigeAndFoodYieldsSharedVictory() throws Exception {
        MesosServerCore serverCore = finishedTwoPlayerGame("shared-room");
        setPlayerScore(serverCore, "shared-room", "Alice", 10, 5);
        setPlayerScore(serverCore, "shared-room", "Bob", 10, 5);

        List<String> winners = winnerNicknames(serverCore, "shared-room");
        assertEquals(2, winners.size());
        assertTrue(winners.contains("Alice"));
        assertTrue(winners.contains("Bob"));
    }

    @Test
    void strictlyHighestPrestigeYieldsSingleWinnerRegardlessOfFood() throws Exception {
        MesosServerCore serverCore = finishedTwoPlayerGame("strict-room");
        setPlayerScore(serverCore, "strict-room", "Alice", 12, 0);
        setPlayerScore(serverCore, "strict-room", "Bob", 9, 99);

        assertEquals(List.of("Alice"), winnerNicknames(serverCore, "strict-room"));
    }

    @Test
    void skippedPlayerReconnectionAndRedisconnectionDriveModelStatusTransitions() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("skip-room", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("skip-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("skip-room", "Bob")).success());
        GameOperationResult started = serverCore.joinGame(new JoinGameRequest("skip-room", "Carol"));
        assertTrue(started.success());

        String activePlayer = started.snapshot().currentPlayerNickname();
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("skip-room", "Alice", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("skip-room", "Bob", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("skip-room", "Carol", true)).success());

        // disconnect the active player: disconnected, then skipped by the resilience pipeline
        expireHeartbeat(serverCore, activePlayer);
        checkDisconnectedClients(serverCore);

        // reconnecting moves the skipped player to rejoined
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("skip-room", activePlayer, true)).success());

        // disconnecting again moves the rejoined player back to skipped
        expireHeartbeat(serverCore, activePlayer);
        checkDisconnectedClients(serverCore);

        assertTrue(serverCore.getSnapshot(new SnapshotRequest("skip-room")).success());
    }

    @Test
    void chooseColorWaitingRoomRejectsAlreadyJoinedAndDisconnectedNicknames() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("wait-a", 3)).success());
        assertTrue(serverCore.createGame(new CreateGameRequest("wait-b", 3)).success());

        // a reserved nickname that joined another game cannot reserve a color here
        assertTrue(serverCore.nickname(new NicknameRequest("Dave")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("wait-b", "Dave")).success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                serverCore.chooseColor(new ChooseColorRequest("wait-a", "Dave", Color.BLUE)).errorCode());

        // a reserved nickname whose session is disconnected cannot reserve a color either
        assertTrue(serverCore.nickname(new NicknameRequest("Eve")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("Eve")).success());
        expireHeartbeat(serverCore, "Eve");
        checkDisconnectedClients(serverCore);
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                serverCore.chooseColor(new ChooseColorRequest("wait-a", "Eve", Color.YELLOW)).errorCode());
    }

    @Test
    void rejoinReturnsGameNotFoundWhenPreviousGameDisappeared() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("ghost-game", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("ghost-game", "Frank")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("ghost-game", "Frank", true)).success());
        expireHeartbeat(serverCore, "Frank");
        checkDisconnectedClients(serverCore);

        removeSession(serverCore, "ghost-game");

        GameOperationResult rejoin = serverCore.joinGame(new JoinGameRequest(null, "Frank", true));
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, rejoin.errorCode());
    }

    @SuppressWarnings("unchecked")
    private static void finishGameNaturally(MesosServerCore serverCore, String gameName) throws Exception {
        Field sessions = MesosServerCore.class.getDeclaredField("sessions");
        sessions.setAccessible(true);
        Object session = ((Map<String, Object>) sessions.get(serverCore)).get(gameName);
        Field controllerField = session.getClass().getDeclaredField("controller");
        controllerField.setAccessible(true);
        Object controller = controllerField.get(session);
        Field currentState = controller.getClass().getDeclaredField("current_state");
        currentState.setAccessible(true);
        currentState.set(controller, null);
    }

    @SuppressWarnings("unchecked")
    private static void removeSession(MesosServerCore serverCore, String gameName) throws Exception {
        Field sessions = MesosServerCore.class.getDeclaredField("sessions");
        sessions.setAccessible(true);
        ((Map<String, Object>) sessions.get(serverCore)).remove(gameName);
    }

    @SuppressWarnings("unchecked")
    private static void forceSingleConnectedTimeout(MesosServerCore serverCore, String gameName) throws Exception {
        Field sessions = MesosServerCore.class.getDeclaredField("sessions");
        sessions.setAccessible(true);
        Object session = ((Map<String, Object>) sessions.get(serverCore)).get(gameName);
        Field since = session.getClass().getDeclaredField("singleConnectedSince");
        since.setAccessible(true);
        since.setLong(session, System.currentTimeMillis()
                - NetworkDefaults.SINGLE_PLAYER_WIN_TIMEOUT_MILLIS - 1);
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

    @SuppressWarnings("unchecked")
    private static GameContext contextForGame(MesosServerCore serverCore, String gameName) throws Exception {
        Field sessions = MesosServerCore.class.getDeclaredField("sessions");
        sessions.setAccessible(true);
        Object session = ((Map<String, Object>) sessions.get(serverCore)).get(gameName);
        Field context = session.getClass().getDeclaredField("context");
        context.setAccessible(true);
        return (GameContext) context.get(session);
    }

    private static void giveAfterEveryoneBuilding(MesosServerCore serverCore, String gameName, String nickname)
            throws Exception {
        GameContext context = contextForGame(serverCore, gameName);
        Player player = context.getPlayers().stream()
                .filter(candidate -> nickname.equals(candidate.getNickname()))
                .findFirst()
                .orElseThrow();
        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.FIRST, 2, 0, 0);
        building.setOwner(player);
        player.getTribe().addCard(building);
    }

    private static void giveFood(MesosServerCore serverCore, String gameName, int amount) throws Exception {
        GameContext context = contextForGame(serverCore, gameName);
        context.getPlayers().forEach(player -> player.gainFood(amount));
    }

    private static MesosServerCore finishedTwoPlayerGame(String gameName) throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest(gameName, 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest(gameName, "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest(gameName, "Bob")).success());
        finishGameNaturally(serverCore, gameName);
        return serverCore;
    }

    private static void setPlayerScore(MesosServerCore serverCore, String gameName, String nickname,
            int prestige, int food) throws Exception {
        GameContext context = contextForGame(serverCore, gameName);
        Player player = context.getPlayers().stream()
                .filter(candidate -> nickname.equals(candidate.getNickname()))
                .findFirst()
                .orElseThrow();
        player.changePrestigePoints(prestige - player.getPrestigePoints());
        int foodDelta = food - player.getFood();
        if (foodDelta >= 0) {
            player.gainFood(foodDelta);
        } else {
            player.loseFood(-foodDelta);
        }
    }

    private static List<String> winnerNicknames(MesosServerCore serverCore, String gameName) {
        GameOperationResult result = serverCore.getSnapshot(new SnapshotRequest(gameName));
        assertTrue(result.success());
        return result.snapshot().players().stream()
                .filter(PlayerSummary::winner)
                .map(PlayerSummary::nickname)
                .toList();
    }

    private static int firstPickableTopCardIndex(GameSnapshot snapshot) {
        return snapshot.topRow().stream()
                .filter(card -> !card.event())
                .findFirst()
                .orElseThrow()
                .selectionIndex();
    }

    private static CardLoader.CardData singleTopPickCardData(int playerCount) throws IOException {
        CardLoader.CardData baseData = CardLoader.load();
        ArrayList<OfferTile> customTiles = new ArrayList<>();
        for (int index = 0; index < playerCount + 2; index++) {
            customTiles.add(new OfferTile((char) ('A' + index), 1, 0, 0, 2));
        }
        return new CardLoader.CardData(baseData.tribe_cards(), baseData.building_cards(), customTiles);
    }
}
