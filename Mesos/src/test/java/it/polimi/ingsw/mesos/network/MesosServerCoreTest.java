package it.polimi.ingsw.mesos.network;

import it.polimi.ingsw.mesos.database.DatabaseManager;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.controller.enums.GameStatus;
import it.polimi.ingsw.mesos.model.CardLoader;
import it.polimi.ingsw.mesos.model.cards.buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.GameContext;
import it.polimi.ingsw.mesos.model.Player;
import it.polimi.ingsw.mesos.model.tiles.OfferTile;

class MesosServerCoreTest {
    @BeforeAll
    static void setDBTestMode() {
        DatabaseManager.useTestMode();
    }

    @Test
    void constructorRejectsNullCardData() {
        assertThrows(IllegalArgumentException.class, () -> new MesosServerCore(null));
    }

    @Test
    void createJoinAndSnapshotStartTheGame() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        GameOperationResult createGame = serverCore.createGame(new CreateGameRequest("local-core", 2));
        assertTrue(createGame.success());
        assertEquals(GameStatus.LOGIN, GameOperationResult.snapshotOf(createGame).status());

        GameOperationResult firstJoin = serverCore.joinGame(new JoinGameRequest("local-core", "Alice"));
        assertTrue(firstJoin.success());
        assertEquals(1, GameOperationResult.snapshotOf(firstJoin).joinedPlayers());

        GameOperationResult secondJoin = serverCore.joinGame(new JoinGameRequest("local-core", "Bob"));
        assertTrue(secondJoin.success());
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(secondJoin).status());
        assertEquals(1, GameOperationResult.snapshotOf(secondJoin).currentRound());
        assertNotNull(GameOperationResult.snapshotOf(secondJoin).currentPlayerNickname());
        assertNotNull(GameOperationResult.snapshotOf(secondJoin).players().stream()
                .filter(player -> "Bob".equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color());

        GameOperationResult snapshot = serverCore.getSnapshot(new SnapshotRequest("local-core"));
        assertTrue(snapshot.success());
        assertEquals(2, GameOperationResult.snapshotOf(snapshot).players().size());
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
                serverCore.chooseOffer(new ChooseOfferRequest("room-a", GameOperationResult.snapshotOf(roomAStarted).currentPlayerNickname(), 0));
        assertTrue(roomAOffer.success());

        GameOperationResult roomBSnapshot = serverCore.getSnapshot(new SnapshotRequest("room-b"));

        assertTrue(roomBSnapshot.success());
        assertTrue(GameOperationResult.snapshotOf(roomBSnapshot).offerTiles().stream().allMatch(OfferTileSummary::available));
        assertTrue(GameOperationResult.snapshotOf(roomBSnapshot).offerTiles().stream().allMatch(tile -> tile.occupantColor() == null));
        assertTrue(GameOperationResult.snapshotOf(roomBSnapshot).offerTiles().stream().allMatch(tile -> tile.occupantNickname() == null));
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
        assertEquals(GameStatus.LOGIN, GameOperationResult.snapshotOf(reserveColor).status());
        assertEquals(1, GameOperationResult.snapshotOf(reserveColor).joinedPlayers());
        assertEquals(java.util.List.of(Color.BLACK), GameOperationResult.snapshotOf(reserveColor).reservedColors());

        GameOperationResult finalJoin = serverCore.joinGame(new JoinGameRequest("color-start-room", "Bob"));

        assertTrue(finalJoin.success());
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(finalJoin).status());
        assertEquals(Color.BLACK, GameOperationResult.snapshotOf(finalJoin).players().stream()
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
        assertEquals(1, GameOperationResult.lobbyOf(lobby).openGames().size());
        assertEquals("open-room", GameOperationResult.lobbyOf(lobby).openGames().getFirst().gameName());

        GameOperationResult fullJoin = serverCore.joinGame(new JoinGameRequest("full-room", "Charlie"));
        assertFalse(fullJoin.success());
        assertEquals(OperationErrorCode.GAME_FULL, GameOperationResult.errorCodeOf(fullJoin));
        assertNull(GameOperationResult.snapshotOf(fullJoin));
    }

    @Test
    void duplicateGameAndNicknamesReturnStructuredErrors() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("shared-name", 2)).success());

        GameOperationResult duplicateGame = serverCore.createGame(new CreateGameRequest("shared-name", 2));
        assertFalse(duplicateGame.success());
        assertEquals(OperationErrorCode.GAME_NAME_TAKEN, GameOperationResult.errorCodeOf(duplicateGame));

        GameOperationResult aliceNickname = serverCore.nickname(new NicknameRequest("Alice"));
        assertTrue(aliceNickname.success());
        assertFalse(((GameOperationResult.Nickname) aliceNickname).rejoinAvailable());

        assertTrue(serverCore.joinGame(new JoinGameRequest("shared-name", "Alice")).success());
        assertTrue(serverCore.createGame(new CreateGameRequest("another-room", 2)).success());

        GameOperationResult duplicatePlayer = serverCore.nickname(new NicknameRequest("Alice"));
        assertFalse(duplicatePlayer.success());
        assertEquals(OperationErrorCode.PLAYER_NAME_TAKEN, GameOperationResult.errorCodeOf(duplicatePlayer));
        assertNull(GameOperationResult.snapshotOf(duplicatePlayer));
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
        assertEquals(OperationErrorCode.GAME_FULL, GameOperationResult.errorCodeOf(repeatedJoin));

        GameOperationResult earlyRejoin = serverCore.joinGame(new JoinGameRequest(null, "Alice", true));
        assertFalse(earlyRejoin.success());
        assertEquals(OperationErrorCode.NICKNAME_DISCONNECTED, GameOperationResult.errorCodeOf(earlyRejoin));

        assertTrue(serverCore.heartbeat(new HeartbeatRequest("rejoin-room", "Alice", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("rejoin-room", "Bob", true)).success());
        expireHeartbeat(serverCore, "Alice");
        checkDisconnectedClients(serverCore);

        GameOperationResult nickname = serverCore.nickname(new NicknameRequest("Alice"));
        assertTrue(nickname.success());
        assertTrue(((GameOperationResult.Nickname) nickname).rejoinAvailable());

        GameOperationResult rejoin = serverCore.joinGame(new JoinGameRequest(null, "Alice", true));

        assertTrue(rejoin.success());
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(rejoin).status());
        assertEquals(2, GameOperationResult.snapshotOf(rejoin).joinedPlayers());
    }

    @Test
    void heartbeatValidatesLobbyAndJoinedPlayerState() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        GameOperationResult unknownLobbyHeartbeat = serverCore.heartbeat(new HeartbeatRequest("Alice"));
        assertFalse(unknownLobbyHeartbeat.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, GameOperationResult.errorCodeOf(unknownLobbyHeartbeat));

        assertTrue(serverCore.nickname(new NicknameRequest("Alice")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("Alice")).success());

        assertTrue(serverCore.createGame(new CreateGameRequest("heartbeat-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("heartbeat-room", "Alice")).success());

        GameOperationResult joinedAsLobbyHeartbeat = serverCore.heartbeat(new HeartbeatRequest("Alice"));
        assertFalse(joinedAsLobbyHeartbeat.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, GameOperationResult.errorCodeOf(joinedAsLobbyHeartbeat));

        GameOperationResult missingGameHeartbeat = serverCore.heartbeat(new HeartbeatRequest("missing-room", "Alice", true));
        assertFalse(missingGameHeartbeat.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, GameOperationResult.errorCodeOf(missingGameHeartbeat));

        GameOperationResult wrongPlayerHeartbeat = serverCore.heartbeat(new HeartbeatRequest("heartbeat-room", "Bob", true));
        assertFalse(wrongPlayerHeartbeat.success());
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, GameOperationResult.errorCodeOf(wrongPlayerHeartbeat));
    }

    @Test
    void passReturnsGameNotFoundForMissingSession() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        GameOperationResult result = serverCore.pass(new PassRequest("missing-pass-room", "Alice"));

        assertFalse(result.success());
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, GameOperationResult.errorCodeOf(result));
        assertNull(GameOperationResult.snapshotOf(result));
    }

    @Test
    void passRejectsPassOutsideOptionalAfterEveryoneState() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("pass-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("pass-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("pass-room", "Bob")).success());

        GameOperationResult result = serverCore.pass(new PassRequest("pass-room", "Alice"));

        assertFalse(result.success());
        assertNotNull(GameOperationResult.snapshotOf(result));
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(result).status());
        assertTrue(result.message().toLowerCase().contains("pass"));
    }

    @Test
    void passSucceedsDuringOptionalAfterEveryoneChoice() throws Exception {
        MesosServerCore serverCore = new MesosServerCore(singleTopPickCardData(2));

        assertTrue(serverCore.createGame(new CreateGameRequest("pass-extra-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("pass-extra-room", "Alice")).success());
        GameOperationResult secondJoin = serverCore.joinGame(new JoinGameRequest("pass-extra-room", "Bob"));
        assertTrue(secondJoin.success());

        String firstPlayer = GameOperationResult.snapshotOf(secondJoin).currentPlayerNickname();
        String secondPlayer = "Alice".equals(firstPlayer) ? "Bob" : "Alice";
        giveAfterEveryoneBuilding(serverCore, "pass-extra-room", firstPlayer);
        giveFood(serverCore, "pass-extra-room", 20);

        GameOperationResult firstOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("pass-extra-room", firstPlayer, 0));
        assertTrue(firstOffer.success());

        GameOperationResult secondOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("pass-extra-room", secondPlayer, 1));
        assertTrue(secondOffer.success());
        assertNotNull(GameOperationResult.snapshotOf(secondOffer));
        assertEquals(GameStatus.ROUND_ACTIONS, GameOperationResult.snapshotOf(secondOffer).status());
        assertEquals(1, GameOperationResult.snapshotOf(secondOffer).currentRound());
        assertFalse(GameOperationResult.snapshotOf(secondOffer).passAvailable());

        GameOperationResult firstAction =
                serverCore.chooseCard(new ChooseCardRequest(
                        "pass-extra-room",
                        firstPlayer,
                        firstPickableTopCardIndex(GameOperationResult.snapshotOf(secondOffer)),
                        true));
        assertTrue(firstAction.success());
        assertNotNull(GameOperationResult.snapshotOf(firstAction));
        assertEquals(secondPlayer, GameOperationResult.snapshotOf(firstAction).currentPlayerNickname());
        assertFalse(GameOperationResult.snapshotOf(firstAction).passAvailable());

        GameOperationResult secondAction =
                serverCore.chooseCard(new ChooseCardRequest(
                        "pass-extra-room",
                        secondPlayer,
                        firstPickableTopCardIndex(GameOperationResult.snapshotOf(firstAction)),
                        true));
        assertTrue(secondAction.success());
        assertNotNull(GameOperationResult.snapshotOf(secondAction));
        assertEquals(GameStatus.ROUND_ACTIONS, GameOperationResult.snapshotOf(secondAction).status());
        assertEquals(firstPlayer, GameOperationResult.snapshotOf(secondAction).currentPlayerNickname());
        assertEquals(1, GameOperationResult.snapshotOf(secondAction).currentRound());
        assertTrue(GameOperationResult.snapshotOf(secondAction).passAvailable());
        String extraPlayer = GameOperationResult.snapshotOf(secondAction).currentPlayerNickname();
        String otherPlayer = "Alice".equals(extraPlayer) ? "Bob" : "Alice";

        GameOperationResult wrongPlayerPass =
                serverCore.pass(new PassRequest("pass-extra-room", otherPlayer));
        assertFalse(wrongPlayerPass.success());

        GameOperationResult passResult =
                serverCore.pass(new PassRequest("pass-extra-room", extraPlayer));

        assertTrue(passResult.success());
        assertNotNull(GameOperationResult.snapshotOf(passResult));
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(passResult).status());
        assertEquals(2, GameOperationResult.snapshotOf(passResult).currentRound());
        assertFalse(GameOperationResult.snapshotOf(passResult).passAvailable());
    }

    @Test
    void optionalAfterEveryoneChoiceAllowsOnlyOneExtraTopCard() throws Exception {
        MesosServerCore serverCore = new MesosServerCore(singleTopPickCardData(2));

        assertTrue(serverCore.createGame(new CreateGameRequest("one-extra-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("one-extra-room", "Alice")).success());
        GameOperationResult secondJoin = serverCore.joinGame(new JoinGameRequest("one-extra-room", "Bob"));
        assertTrue(secondJoin.success());

        String firstPlayer = GameOperationResult.snapshotOf(secondJoin).currentPlayerNickname();
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
                firstPickableTopCardIndex(GameOperationResult.snapshotOf(secondOffer)),
                true
        )).success());

        GameOperationResult secondAction =
                serverCore.chooseCard(new ChooseCardRequest(
                        "one-extra-room",
                        secondPlayer,
                        firstPickableTopCardIndex(GameOperationResult.snapshotOf(serverCore.getSnapshot(new SnapshotRequest("one-extra-room")))),
                        true
                ));
        assertTrue(secondAction.success());
        assertTrue(GameOperationResult.snapshotOf(secondAction).passAvailable());
        assertEquals(firstPlayer, GameOperationResult.snapshotOf(secondAction).currentPlayerNickname());

        GameOperationResult extraPick =
                serverCore.chooseCard(new ChooseCardRequest(
                        "one-extra-room",
                        firstPlayer,
                        firstPickableTopCardIndex(GameOperationResult.snapshotOf(secondAction)),
                        true
                ));
        assertTrue(extraPick.success());
        assertNotNull(GameOperationResult.snapshotOf(extraPick));
        assertEquals(GameStatus.ROUND_OFFER, GameOperationResult.snapshotOf(extraPick).status());
        assertEquals(2, GameOperationResult.snapshotOf(extraPick).currentRound());
        assertFalse(GameOperationResult.snapshotOf(extraPick).passAvailable());

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

        String firstPlayer = GameOperationResult.snapshotOf(thirdJoin).currentPlayerNickname();

        assertTrue(serverCore.heartbeat(new HeartbeatRequest("disconnect-actions-room", "Alice", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("disconnect-actions-room", "Bob", true)).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("disconnect-actions-room", "Carol", true)).success());

        GameOperationResult firstOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("disconnect-actions-room", firstPlayer, 0));
        assertTrue(firstOffer.success());
        String secondPlayer = GameOperationResult.snapshotOf(firstOffer).currentPlayerNickname();

        GameOperationResult secondOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("disconnect-actions-room", secondPlayer, 1));
        assertTrue(secondOffer.success());
        String thirdPlayer = GameOperationResult.snapshotOf(secondOffer).currentPlayerNickname();

        GameOperationResult thirdOffer =
                serverCore.chooseOffer(new ChooseOfferRequest("disconnect-actions-room", thirdPlayer, 2));
        assertTrue(thirdOffer.success());
        assertEquals(GameStatus.ROUND_ACTIONS, GameOperationResult.snapshotOf(thirdOffer).status());
        assertEquals(firstPlayer, GameOperationResult.snapshotOf(thirdOffer).currentPlayerNickname());

        expireHeartbeat(serverCore, firstPlayer);
        checkDisconnectedClients(serverCore);

        GameOperationResult snapshot =
                serverCore.getSnapshot(new SnapshotRequest("disconnect-actions-room"));
        assertTrue(snapshot.success());
        assertNotNull(GameOperationResult.snapshotOf(snapshot));
        assertEquals(GameStatus.ROUND_ACTIONS, GameOperationResult.snapshotOf(snapshot).status());
        assertEquals(secondPlayer, GameOperationResult.snapshotOf(snapshot).currentPlayerNickname());
        assertFalse(GameOperationResult.snapshotOf(snapshot).passAvailable());
    }

    @Test
    void chooseColorReservesWaitingRoomColorRejectsDuplicatesAndAppearsInPreStartSnapshot() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("color-room", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("color-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("color-room", "Bob")).success());

        GameOperationResult chooseColor = serverCore.chooseColor(new ChooseColorRequest("color-room", "Alice", Color.BLACK));

        assertTrue(chooseColor.success());
        assertEquals(Color.BLACK, GameOperationResult.snapshotOf(chooseColor).players().stream()
                .filter(player -> "Alice".equals(player.nickname()))
                .findFirst()
                .orElseThrow()
                .color());
        assertEquals(java.util.List.of(Color.BLACK), GameOperationResult.snapshotOf(chooseColor).reservedColors());

        GameOperationResult duplicateColor = serverCore.chooseColor(new ChooseColorRequest("color-room", "Bob", Color.BLACK));

        assertFalse(duplicateColor.success());
        assertEquals(OperationErrorCode.COLOR_ALREADY_TAKEN, GameOperationResult.errorCodeOf(duplicateColor));
        assertEquals(java.util.List.of(Color.BLACK), GameOperationResult.snapshotOf(duplicateColor).reservedColors());
    }

    @Test
    void chooseColorRejectsSelectionAfterGameStart() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertTrue(serverCore.createGame(new CreateGameRequest("started-color-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("started-color-room", "Alice")).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("started-color-room", "Bob")).success());

        GameOperationResult lateChoice = serverCore.chooseColor(new ChooseColorRequest("started-color-room", "Alice", Color.YELLOW));

        assertFalse(lateChoice.success());
        assertEquals(OperationErrorCode.COLOR_SELECTION_CLOSED, GameOperationResult.errorCodeOf(lateChoice));
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
        assertTrue(((GameOperationResult.Nickname) rejoinAvailable).rejoinAvailable());

        GameOperationResult chooseColor =
                serverCore.chooseColor(new ChooseColorRequest("liveness-room", "Alice", Color.YELLOW));

        assertTrue(chooseColor.success());

        GameOperationResult takenAgain = serverCore.nickname(new NicknameRequest("Alice"));
        assertFalse(takenAgain.success());
        assertEquals(OperationErrorCode.PLAYER_NAME_TAKEN, GameOperationResult.errorCodeOf(takenAgain));
    }

    @Test
    void operationsOnMissingGameReturnGameNotFound() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                GameOperationResult.errorCodeOf(serverCore.getSnapshot(new SnapshotRequest("nope"))));
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                GameOperationResult.errorCodeOf(serverCore.chooseOffer(new ChooseOfferRequest("nope", "Alice", 0))));
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                GameOperationResult.errorCodeOf(serverCore.chooseCard(new ChooseCardRequest("nope", "Alice", 0, true))));
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                GameOperationResult.errorCodeOf(serverCore.chooseColor(new ChooseColorRequest("nope", "Alice", Color.BLUE))));
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                GameOperationResult.errorCodeOf(serverCore.subscribe(new SubscribeGameRequest("nope", "Alice"), update -> { })));
        assertEquals(OperationErrorCode.GAME_NOT_FOUND,
                GameOperationResult.errorCodeOf(serverCore.unsubscribe(new UnsubscribeGameRequest("nope", "Alice"), update -> { })));
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
                GameOperationResult.errorCodeOf(serverCore.chooseOffer(new ChooseOfferRequest("started-room", "Mallory", 0))));
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                GameOperationResult.errorCodeOf(serverCore.chooseCard(new ChooseCardRequest("started-room", "Mallory", 0, true))));
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                GameOperationResult.errorCodeOf(serverCore.pass(new PassRequest("started-room", "Mallory"))));
    }

    @Test
    void chooseColorBeforeStartRejectsNonMemberNickname() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("color-wait", 3)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("color-wait", "Alice")).success());

        GameOperationResult result =
                serverCore.chooseColor(new ChooseColorRequest("color-wait", "Stranger", Color.BLUE));

        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, GameOperationResult.errorCodeOf(result));
    }

    @Test
    void subscribeRejectsNullSinkAndNonMembersThenUnsubscribes() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        assertTrue(serverCore.createGame(new CreateGameRequest("subs-room", 2)).success());
        assertTrue(serverCore.joinGame(new JoinGameRequest("subs-room", "Alice")).success());

        assertEquals(OperationErrorCode.INVALID_LISTENER,
                GameOperationResult.errorCodeOf(serverCore.subscribe(new SubscribeGameRequest("subs-room", "Alice"), null)));
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                GameOperationResult.errorCodeOf(serverCore.subscribe(new SubscribeGameRequest("subs-room", "Ghost"), update -> { })));

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
        assertTrue(GameOperationResult.snapshotOf(snapshot).finished());
        assertEquals(GameStatus.END_GAME, GameOperationResult.snapshotOf(snapshot).status());
        List<String> forcedWinners = GameOperationResult.snapshotOf(snapshot).players().stream()
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
                GameOperationResult.errorCodeOf(serverCore.joinGame(new JoinGameRequest("ghost-room", "Alice"))));

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
                GameOperationResult.errorCodeOf(serverCore.joinGame(new JoinGameRequest("room-b", "Cara"))));
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
        assertTrue(GameOperationResult.snapshotOf(snapshot).finished());
        assertEquals(GameStatus.END_GAME, GameOperationResult.snapshotOf(snapshot).status());
        assertTrue(GameOperationResult.snapshotOf(snapshot).players().stream().anyMatch(PlayerSummary::winner));

        // disconnecting a player in a finished game must not touch model connection state
        expireHeartbeat(serverCore, "Alice");
        checkDisconnectedClients(serverCore);
        assertTrue(GameOperationResult.snapshotOf(serverCore.getSnapshot(new SnapshotRequest("finished-room"))).finished());
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

        String activePlayer = GameOperationResult.snapshotOf(started).currentPlayerNickname();
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
                GameOperationResult.errorCodeOf(serverCore.chooseColor(new ChooseColorRequest("wait-a", "Dave", Color.BLUE))));

        // a reserved nickname whose session is disconnected cannot reserve a color either
        assertTrue(serverCore.nickname(new NicknameRequest("Eve")).success());
        assertTrue(serverCore.heartbeat(new HeartbeatRequest("Eve")).success());
        expireHeartbeat(serverCore, "Eve");
        checkDisconnectedClients(serverCore);
        assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME,
                GameOperationResult.errorCodeOf(serverCore.chooseColor(new ChooseColorRequest("wait-a", "Eve", Color.YELLOW))));
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
        assertEquals(OperationErrorCode.GAME_NOT_FOUND, GameOperationResult.errorCodeOf(rejoin));
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
        return GameOperationResult.snapshotOf(result).players().stream()
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
