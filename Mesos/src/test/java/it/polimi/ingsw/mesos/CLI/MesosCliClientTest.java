package it.polimi.ingsw.mesos.CLI;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.CardSummary;
import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.GameUpdateListener;
import it.polimi.ingsw.mesos.Network.LobbyGameSummary;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.OfferTileSummary;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseColorRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.NicknameRequest;
import it.polimi.ingsw.mesos.Network.Requests.PassRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.TurnOrderSpaceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesosCliClientTest {
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;

    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    void promptHelpersRepromptUntilValidValues() throws Exception {
        MesosCliClient cli = newCli("bad\nR\n \nvalue\nbad\n3\n1\n5\n \nmaybe\ny\nn\n");
        ByteArrayOutputStream output = captureOutput();

        assertEquals(ConnectionType.RMI, invoke(cli, "promptConnectionType"));
        assertEquals("value", invoke(cli, "promptRequired", String.class, "Campo"));
        assertEquals(3, invoke(cli, "promptInt", String.class, "Numero"));
        assertEquals(5, invoke(cli, "promptPlayerCount"));
        assertEquals(true, invoke(cli, "promptYesNo", String.class, "Confermi"));
        assertEquals(false, invoke(cli, "promptYesNo", String.class, "Confermi"));

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Scegli 'socket' oppure 'rmi'."));
        assertTrue(text.contains("Campo non puo' essere vuoto."));
        assertTrue(text.contains("Numero deve essere un numero valido."));
        assertTrue(text.contains("Numero giocatori deve essere compreso"));
        assertTrue(text.contains("Rispondi con yes/no"));
    }

    @Test
    void parsingAndFormattingHelpersCoverSuccessAndFailureBranches() throws Exception {
        MesosCliClient cli = newCli("");

        assertEquals(true, invoke(cli, "parseRow", String.class, "top"));
        assertEquals(true, invoke(cli, "parseRow", String.class, "alto"));
        assertEquals(false, invoke(cli, "parseRow", String.class, "bottom"));
        assertEquals(false, invoke(cli, "parseRow", String.class, "basso"));
        assertThrows(IllegalArgumentException.class, () -> invokeUnchecked(cli, "parseRow", new Class<?>[] {String.class}, "middle"));

        assertEquals(42, invoke(cli, "parseInt", new Class<?>[] {String.class, String.class}, "42", "valore"));
        assertThrows(IllegalArgumentException.class, () -> invokeUnchecked(
                cli,
                "parseInt",
                new Class<?>[] {String.class, String.class},
                "x",
                "valore"));

        assertEquals("room-7", invoke(cli, "appendNumericSuffix", new Class<?>[] {String.class, int.class}, "room", 7));
        assertEquals("IOException", invoke(cli, "describeConnectionError", IOException.class, new IOException()));
        assertEquals("boom", invoke(cli, "describeConnectionError", IOException.class, new IOException("boom")));
        assertEquals("IOException", invoke(cli, "describeConnectionError", IOException.class, new IOException(" ")));
    }

    @Test
    void handleResultAndRemoteUpdateManageSnapshotAndOutput() throws Exception {
        MesosCliClient cli = newCli("");
        ByteArrayOutputStream output = captureOutput();
        GameSnapshot snapshot = snapshot(GameStatus.ROUND_OFFER);

        invoke(cli, "handleResult", GameOperationResult.class, null);
        assertNull(field(cli, "snapshot"));

        invoke(cli, "handleResult", new Class<?>[] {GameOperationResult.class, boolean.class},
                GameOperationResult.success("ok", snapshot), false);
        assertEquals(snapshot, field(cli, "snapshot"));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("[ok] ok"));

        invoke(cli, "handleResult", new Class<?>[] {GameOperationResult.class, boolean.class},
                GameOperationResult.failure("fail", snapshot), false);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[error] fail"));

        setField(cli, "nickname", "Alice");
        invoke(cli, "handleRemoteUpdate", GameViewUpdate.class, null);
        invoke(cli, "handleRemoteUpdate", GameViewUpdate.class, new GameViewUpdate("room", "updated", snapshot));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[update] updated"));
    }

    @Test
    void fetchLobbyHandlesFailureNullAndSuccessfulLobby() throws Exception {
        MesosCliClient cli = newCli("");
        ByteArrayOutputStream output = captureOutput();
        FakeClient client = new FakeClient();
        client.lobbyResults.add(GameOperationResult.failure("no lobby", null));
        client.lobbyResults.add(GameOperationResult.successLobby("empty", null));
        client.lobbyResults.add(GameOperationResult.successLobby("ok", new LobbySnapshot(List.of(
                new LobbyGameSummary("open", 2, 1, 1)))));

        assertTrue(((LobbySnapshot) invoke(cli, "fetchLobby", GameClient.class, client)).openGames().isEmpty());
        assertTrue(((LobbySnapshot) invoke(cli, "fetchLobby", GameClient.class, client)).openGames().isEmpty());
        assertEquals("open", ((LobbySnapshot) invoke(cli, "fetchLobby", GameClient.class, client)).openGames().getFirst().gameName());

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("[error] no lobby"));
        assertTrue(text.contains("open"));
    }

    @Test
    void createGameWithUniqueNameCoversRenameAutoAppendAndFailure() throws Exception {
        MesosCliClient renameCli = newCli("no\nRenamed\n");
        FakeClient renameClient = new FakeClient();
        renameClient.createResults.add(GameOperationResult.failure("taken", OperationErrorCode.GAME_NAME_TAKEN, null));
        renameClient.createResults.add(GameOperationResult.success("created", snapshot(GameStatus.LOGIN)));

        assertEquals("Renamed", invoke(renameCli, "createGameWithUniqueName",
                new Class<?>[] {GameClient.class, String.class, int.class}, renameClient, "Room", 2));
        assertEquals(List.of("Room", "Renamed"), renameClient.createdGameNames);

        MesosCliClient autoCli = newCli("yes\nyes\n");
        FakeClient autoClient = new FakeClient();
        autoClient.createResults.add(GameOperationResult.failure("taken", OperationErrorCode.GAME_NAME_TAKEN, null));
        autoClient.createResults.add(GameOperationResult.failure("taken again", OperationErrorCode.GAME_NAME_TAKEN, null));
        autoClient.createResults.add(GameOperationResult.success("created", snapshot(GameStatus.LOGIN)));

        assertEquals("Room-1-1", invoke(autoCli, "createGameWithUniqueName",
                new Class<?>[] {GameClient.class, String.class, int.class}, autoClient, "Room", 2));
        assertEquals(List.of("Room", "Room-1", "Room-1-1"), autoClient.createdGameNames);

        MesosCliClient failCli = newCli("");
        FakeClient failClient = new FakeClient();
        failClient.createResults.add(GameOperationResult.failure("bad", OperationErrorCode.GAME_FULL, null));

        assertNull(invoke(failCli, "createGameWithUniqueName",
                new Class<?>[] {GameClient.class, String.class, int.class}, failClient, "Room", 2));
    }

    @Test
    void joinSelectedGameCoversCreateFallbacksAndErrors() throws Exception {
        MesosCliClient foundCli = newCli("yes\n2\nquit\n");
        FakeClient foundClient = new FakeClient();
        foundClient.joinResults.add(GameOperationResult.failure("missing", OperationErrorCode.GAME_NOT_FOUND, null));
        foundClient.joinResults.add(GameOperationResult.success("joined", snapshot(GameStatus.LOGIN)));
        foundClient.createResults.add(GameOperationResult.success("created", snapshot(GameStatus.LOGIN)));
        foundClient.subscribeResults.add(GameOperationResult.success("subscribed", snapshot(GameStatus.LOGIN)));
        setField(foundCli, "nickname", "Alice");

        assertEquals(true, invoke(foundCli, "joinSelectedGame",
                new Class<?>[] {GameClient.class, String.class, boolean.class},
                foundClient, "Room", true));
        assertEquals(List.of("Room"), foundClient.createdGameNames);
        assertEquals(List.of("Alice", "Alice"), foundClient.joinNicknames);

        MesosCliClient noFallbackCli = newCli("");
        FakeClient noFallbackClient = new FakeClient();
        noFallbackClient.joinResults.add(GameOperationResult.failure("missing", OperationErrorCode.GAME_NOT_FOUND, null));
        setField(noFallbackCli, "nickname", "Alice");
        assertEquals(false, invoke(noFallbackCli, "joinSelectedGame",
                new Class<?>[] {GameClient.class, String.class, boolean.class},
                noFallbackClient, "Room", false));

        MesosCliClient fullCli = newCli("");
        FakeClient fullClient = new FakeClient();
        fullClient.joinResults.add(GameOperationResult.failure("full", OperationErrorCode.GAME_FULL, null));
        setField(fullCli, "nickname", "Alice");
        assertEquals(false, invoke(fullCli, "joinSelectedGame",
                new Class<?>[] {GameClient.class, String.class, boolean.class},
                fullClient, "Room", false));
    }

    @Test
    void handleCreateAndJoinFlowsDelegateToExpectedPaths() throws Exception {
        MesosCliClient createCli = newCli("2\nAlice\nquit\n");
        FakeClient createClient = new FakeClient();
        createClient.createResults.add(GameOperationResult.success("created", snapshot(GameStatus.LOGIN)));
        createClient.joinResults.add(GameOperationResult.success("joined", snapshot(GameStatus.LOGIN)));
        createClient.subscribeResults.add(GameOperationResult.success("subscribed", snapshot(GameStatus.LOGIN)));
        setField(createCli, "nickname", "Alice");

        assertEquals(true, invoke(createCli, "handleCreateFlow",
                new Class<?>[] {GameClient.class, String.class}, createClient, "Room"));

        MesosCliClient joinCli = newCli("Bob\nquit\n");
        FakeClient joinClient = new FakeClient();
        joinClient.joinResults.add(GameOperationResult.success("joined", snapshot(GameStatus.LOGIN)));
        joinClient.subscribeResults.add(GameOperationResult.success("subscribed", snapshot(GameStatus.LOGIN)));
        LobbySnapshot lobby = new LobbySnapshot(List.of(new LobbyGameSummary("Other", 2, 1, 1)));
        setField(joinCli, "nickname", "Bob");

        assertEquals(true, invoke(joinCli, "handleJoinFlow",
                new Class<?>[] {GameClient.class, LobbySnapshot.class, String.class}, joinClient, lobby, "Room"));
    }

    @Test
    void chooseNicknameCoversRegistrationRetriesAndRejoinPaths() throws Exception {
        MesosCliClient plainCli = newCli("Alice\n");
        FakeClient plainClient = new FakeClient();
        plainClient.nicknameResults.add(GameOperationResult.successNickname("registered", false));

        assertEquals(false, invoke(plainCli, "chooseNickname", GameClient.class, plainClient));
        assertEquals("Alice", field(plainCli, "nickname"));
        assertEquals(List.of("Alice"), plainClient.nicknames);

        MesosCliClient retryCli = newCli("Alice\nno\nAlice2\n");
        FakeClient retryClient = new FakeClient();
        retryClient.nicknameResults.add(GameOperationResult.failure("taken", OperationErrorCode.PLAYER_NAME_TAKEN, null));
        retryClient.nicknameResults.add(GameOperationResult.successNickname("registered", false));

        assertEquals(false, invoke(retryCli, "chooseNickname", GameClient.class, retryClient));
        assertEquals("Alice2", field(retryCli, "nickname"));
        assertEquals(List.of("Alice", "Alice2"), retryClient.nicknames);

        MesosCliClient declineCli = newCli("Alice\nno\nBob\n");
        FakeClient declineClient = new FakeClient();
        declineClient.nicknameResults.add(GameOperationResult.successNickname("rejoin", true));
        declineClient.nicknameResults.add(GameOperationResult.successNickname("registered", false));

        assertEquals(false, invoke(declineCli, "chooseNickname", GameClient.class, declineClient));
        assertEquals("Bob", field(declineCli, "nickname"));
        assertEquals(List.of("Alice", "Bob"), declineClient.nicknames);

        MesosCliClient rejoinCli = newCli("Alice\nyes\nquit\n");
        FakeClient rejoinClient = new FakeClient();
        rejoinClient.nicknameResults.add(GameOperationResult.successNickname("rejoin", true));
        rejoinClient.joinResults.add(GameOperationResult.success("rejoined", snapshot(GameStatus.LOGIN)));
        rejoinClient.subscribeResults.add(GameOperationResult.success("subscribed", snapshot(GameStatus.LOGIN)));

        assertEquals(true, invoke(rejoinCli, "chooseNickname", GameClient.class, rejoinClient));
        assertEquals("Alice", field(rejoinCli, "nickname"));
        assertEquals("Room", field(rejoinCli, "gameName"));
        assertEquals(List.of(true), rejoinClient.joinRejoinFlags);
    }

    @Test
    void promptLobbyActionAcceptsAliasesAfterInvalidInput() throws Exception {
        MesosCliClient createCli = newCli("bad\ncrea\n");
        ByteArrayOutputStream output = captureOutput();

        assertEquals(true, invoke(createCli, "promptLobbyAction"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Scegli 'join' oppure 'create'."));

        MesosCliClient joinCli = newCli("entra\n");

        assertEquals(false, invoke(joinCli, "promptLobbyAction"));
    }

    @Test
    void mainLoopTreatsEmptyCommandAsRefreshAndCoversInvalidCommands() throws Exception {
        MesosCliClient cli = newCli("\nhelp\nshow\nwait 0\nrefresh\npoll\noffer 1\ncard top 0\ncarta basso 0\nunknown\ncard middle 1\nexit\n");
        FakeClient client = new FakeClient();
        client.snapshotResults.add(GameOperationResult.success("snapshot", snapshot(GameStatus.ROUND_OFFER)));
        client.snapshotResults.add(GameOperationResult.success("snapshot", snapshot(GameStatus.ROUND_OFFER)));
        client.snapshotResults.add(GameOperationResult.success("snapshot", snapshot(GameStatus.ROUND_ACTIONS)));
        client.offerResults.add(GameOperationResult.success("offer", snapshot(GameStatus.ROUND_OFFER)));
        client.cardResults.add(GameOperationResult.success("card", snapshot(GameStatus.ROUND_ACTIONS)));
        client.cardResults.add(GameOperationResult.success("carta", snapshot(GameStatus.ROUND_ACTIONS)));
        setField(cli, "gameName", "Room");
        setField(cli, "nickname", "Alice");
        setField(cli, "snapshot", snapshot(GameStatus.ROUND_OFFER));
        ByteArrayOutputStream output = captureOutput();

        invoke(cli, "mainLoop", GameClient.class, client);

        assertEquals(List.of(1), client.offerIndexes);
        assertEquals(List.of(true, false), client.cardRows);
        assertEquals(3, client.snapshotRequests.size());
        String text = output.toString(StandardCharsets.UTF_8);
        assertFalse(text.contains("Comando non puo' essere vuoto."));
        assertTrue(text.contains("Comando non valido"));
        assertTrue(text.contains("Chiusura del client CLI."));
    }

    @Test
    void handleGameCommandForwardsPassAndValidatesArgumentCounts() throws Exception {
        MesosCliClient cli = newCli("");
        FakeClient client = new FakeClient();
        client.offerResults.add(GameOperationResult.success("offer", snapshot(GameStatus.ROUND_OFFER)));
        client.cardResults.add(GameOperationResult.success("card", snapshot(GameStatus.ROUND_ACTIONS)));
        client.passResults.add(GameOperationResult.success("pass", snapshot(GameStatus.ROUND_ACTIONS)));
        setField(cli, "gameName", "Room");
        setField(cli, "nickname", "Alice");

        invoke(cli, "handleGameCommand", new Class<?>[] {GameClient.class, String.class}, client, "offerta 2");
        invoke(cli, "handleGameCommand", new Class<?>[] {GameClient.class, String.class}, client, "carta alto 3");
        invoke(cli, "handleGameCommand", new Class<?>[] {GameClient.class, String.class}, client, "pass");

        assertEquals(List.of(2), client.offerIndexes);
        assertEquals(List.of(3), client.cardIndexes);
        assertEquals(List.of("Room"), client.passGameNames);
        assertEquals(List.of("Alice"), client.passNicknames);
        assertThrows(IllegalArgumentException.class, () -> invokeUnchecked(
                cli, "handleGameCommand", new Class<?>[] {GameClient.class, String.class}, client, "offer"));
        assertThrows(IllegalArgumentException.class, () -> invokeUnchecked(
                cli, "handleGameCommand", new Class<?>[] {GameClient.class, String.class}, client, "card top"));
        assertThrows(IllegalArgumentException.class, () -> invokeUnchecked(
                cli, "handleGameCommand", new Class<?>[] {GameClient.class, String.class}, client, "passa"));
        assertThrows(IllegalArgumentException.class, () -> invokeUnchecked(
                cli, "handleGameCommand", new Class<?>[] {GameClient.class, String.class}, client, "nope"));
    }

    @Test
    void waitRejectsNegativeValuesAndHonorsInterrupts() throws Exception {
        MesosCliClient cli = newCli("");

        assertThrows(IllegalArgumentException.class,
                () -> invokeUnchecked(cli, "handleWait", new Class<?>[] {String.class}, "wait -1"));
        Thread.currentThread().interrupt();
        invoke(cli, "handleWait", String.class, "wait 1");
        assertTrue(Thread.interrupted());
    }

    @Test
    void promptRawThrowsWhenInputStreamCloses() {
        MesosCliClient cli = newCli("");

        IOException exception = assertThrows(IOException.class,
                () -> invokeUnchecked(cli, "promptRaw", new Class<?>[] {String.class}, "prompt"));
        assertTrue(exception.getMessage().contains("Input stream closed"));
    }

    private MesosCliClient newCli(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        return new MesosCliClient();
    }

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        return output;
    }

    private static GameSnapshot snapshot(GameStatus status) {
        return new GameSnapshot(
                "Room",
                2,
                2,
                true,
                false,
                status,
                1,
                Era.FIRST,
                "Alice",
                Color.BLUE,
                1,
                1,
                List.of(Color.BLUE),
                List.of(new PlayerSummary("Alice", Color.BLUE, 3, 4, 2, 1)),
                List.of(new OfferTileSummary(0, 'A', 1, 0, 0, true, null, null)),
                List.of(new TurnOrderSpaceSummary(0, 0, Color.BLUE, "Alice")),
                List.of(new CardSummary(0, "Hunter", Era.FIRST, false, "hunter", null, null)),
                List.of(new CardSummary(0, "Sustenance Event", Era.FIRST, true, "event", null, null))
        );
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object invoke(Object target, String name) throws Exception {
        return invoke(target, name, new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?> parameterType, Object argument) throws Exception {
        return invoke(target, name, new Class<?>[] {parameterType}, argument);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        return invokeUnchecked(target, name, parameterTypes, arguments);
    }

    private static Object invokeUnchecked(Object target, String name, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    private static final class FakeClient implements GameClient {
        private final Queue<GameOperationResult> lobbyResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> createResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> joinResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> snapshotResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> offerResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> cardResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> colorResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> passResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> subscribeResults = new ArrayDeque<>();
        private final Queue<GameOperationResult> nicknameResults = new ArrayDeque<>();
        private final List<String> createdGameNames = new ArrayList<>();
        private final List<String> joinNicknames = new ArrayList<>();
        private final List<Boolean> joinRejoinFlags = new ArrayList<>();
        private final List<String> nicknames = new ArrayList<>();
        private final List<String> snapshotRequests = new ArrayList<>();
        private final List<Integer> offerIndexes = new ArrayList<>();
        private final List<Integer> cardIndexes = new ArrayList<>();
        private final List<Boolean> cardRows = new ArrayList<>();
        private final List<String> passGameNames = new ArrayList<>();
        private final List<String> passNicknames = new ArrayList<>();

        @Override
        public GameOperationResult getLobby() {
            return poll(lobbyResults, GameOperationResult.successLobby("lobby", new LobbySnapshot(null)));
        }

        @Override
        public GameOperationResult createGame(CreateGameRequest request) {
            createdGameNames.add(request.gameName());
            return poll(createResults, GameOperationResult.success("created", snapshot(GameStatus.LOGIN)));
        }

        @Override
        public GameOperationResult joinGame(JoinGameRequest request) {
            joinNicknames.add(request.nickname());
            joinRejoinFlags.add(request.rejoin());
            return poll(joinResults, GameOperationResult.success("joined", snapshot(GameStatus.LOGIN)));
        }

        @Override
        public GameOperationResult getSnapshot(SnapshotRequest request) {
            snapshotRequests.add(request.gameName());
            return poll(snapshotResults, GameOperationResult.success("snapshot", snapshot(GameStatus.ROUND_OFFER)));
        }

        @Override
        public GameOperationResult chooseOffer(ChooseOfferRequest request) {
            offerIndexes.add(request.offerTileIndex());
            return poll(offerResults, GameOperationResult.success("offer", snapshot(GameStatus.ROUND_OFFER)));
        }

        @Override
        public GameOperationResult chooseCard(ChooseCardRequest request) {
            cardIndexes.add(request.cardIndex());
            cardRows.add(request.topRow());
            return poll(cardResults, GameOperationResult.success("card", snapshot(GameStatus.ROUND_ACTIONS)));
        }

        @Override
        public GameOperationResult chooseColor(ChooseColorRequest request) {
            return poll(colorResults, GameOperationResult.success("color", snapshot(GameStatus.LOGIN)));
        }

        @Override
        public GameOperationResult pass(PassRequest request) {
            passGameNames.add(request.gameName());
            passNicknames.add(request.nickname());
            return poll(passResults, GameOperationResult.success("pass", snapshot(GameStatus.ROUND_ACTIONS)));
        }

        @Override
        public GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateListener listener) {
            return poll(subscribeResults, GameOperationResult.success("subscribed", snapshot(GameStatus.LOGIN)));
        }

        @Override
        public GameOperationResult unsubscribe(UnsubscribeGameRequest request) {
            return GameOperationResult.success("unsubscribed", snapshot(GameStatus.LOGIN));
        }

        @Override
        public GameOperationResult nickname(NicknameRequest request) {
            nicknames.add(request.nickname());
            return poll(nicknameResults, GameOperationResult.successNickname("registered", false));
        }

        @Override
        public void close() {
        }

        private static GameOperationResult poll(Queue<GameOperationResult> queue, GameOperationResult fallback) {
            GameOperationResult result = queue.poll();
            return result == null ? fallback : result;
        }
    }
}
