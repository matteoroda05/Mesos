package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class NetworkValueObjectsTest {
    @Test
    void cardSummaryValidatesSelectionAndTypeAndDefaultsDetails() {
        assertThrows(IllegalArgumentException.class,
                () -> new CardSummary(-1, "Hunter", Era.FIRST, false, "", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CardSummary(0, null, Era.FIRST, false, "", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CardSummary(0, " ", Era.FIRST, false, "", null, null));

        assertEquals("", new CardSummary(0, "Hunter", Era.FIRST, false, null, null, null).details());
    }

    @Test
    void gameSnapshotValidatesBoundsAndCopiesNullableLists() {
        assertThrows(IllegalArgumentException.class, () -> snapshot(null, 2, 0, GameStatus.LOGIN, 0));
        assertThrows(IllegalArgumentException.class, () -> snapshot(" ", 2, 0, GameStatus.LOGIN, 0));
        assertThrows(IllegalArgumentException.class, () -> snapshot("game", 0, 0, GameStatus.LOGIN, 0));
        assertThrows(IllegalArgumentException.class, () -> snapshot("game", 2, -1, GameStatus.LOGIN, 0));
        assertThrows(IllegalArgumentException.class, () -> snapshot("game", 2, 3, GameStatus.LOGIN, 0));
        assertThrows(IllegalArgumentException.class, () -> snapshot("game", 2, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> snapshot("game", 2, 0, GameStatus.LOGIN, -1));

        GameSnapshot snapshot = new GameSnapshot("game", 2, 1, false, false, GameStatus.LOGIN, 0,
                Era.FIRST, null, null, 0, 0, null, null, null, null, null, null);

        assertTrue(snapshot.reservedColors().isEmpty());
        assertTrue(snapshot.players().isEmpty());
        assertTrue(snapshot.offerTiles().isEmpty());
        assertTrue(snapshot.turnOrder().isEmpty());
        assertTrue(snapshot.topRow().isEmpty());
        assertTrue(snapshot.bottomRow().isEmpty());
        assertFalse(snapshot.passAvailable());
    }

    @Test
    void gameSnapshotListFieldsAreDefensiveCopies() {
        List<PlayerSummary> players = new ArrayList<>();
        players.add(new PlayerSummary("Alice", Color.BLUE, 1, 2, 3, 4));

        GameSnapshot snapshot = new GameSnapshot("game", 2, 1, false, false, GameStatus.LOGIN, 0,
                Era.FIRST, null, null, 0, 0, List.of(Color.BLUE), players, List.of(), List.of(), List.of(), List.of());

        players.add(new PlayerSummary("Bob", Color.YELLOW, 0, 0, 0, 0));
        assertEquals(List.of(Color.BLUE), snapshot.reservedColors());
        assertEquals(1, snapshot.players().size());
        assertFalse(snapshot.passAvailable());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.players().add(new PlayerSummary("Carol", Color.ORANGE, 0, 0, 0, 0)));
    }

    @Test
    void lobbySummaryValidatesSeatCounts() {
        assertThrows(IllegalArgumentException.class, () -> new LobbyGameSummary(null, 2, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> new LobbyGameSummary(" ", 2, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> new LobbyGameSummary("game", 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new LobbyGameSummary("game", 2, -1, 3));
        assertThrows(IllegalArgumentException.class, () -> new LobbyGameSummary("game", 2, 3, -1));
        assertThrows(IllegalArgumentException.class, () -> new LobbyGameSummary("game", 2, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new LobbyGameSummary("game", 2, 1, 2));

        assertEquals(1, new LobbyGameSummary("game", 2, 1, 1).availableSeats());
    }

    @Test
    void lobbySnapshotDefaultsAndSortsOpenGames() {
        assertTrue(new LobbySnapshot(null).openGames().isEmpty());

        LobbySnapshot snapshot = new LobbySnapshot(List.of(
                new LobbyGameSummary("three", 3, 1, 2),
                new LobbyGameSummary("beta", 2, 1, 1),
                new LobbyGameSummary("alpha", 2, 0, 2)));

        assertEquals(List.of("alpha", "beta", "three"),
                snapshot.openGames().stream().map(LobbyGameSummary::gameName).toList());
    }

    @Test
    void smallSummariesValidateTheirRequiredFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new OfferTileSummary(-1, 'A', 1, 0, 0, true, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new TurnOrderSpaceSummary(-1, 0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerSummary(null, Color.BLUE, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerSummary(" ", Color.BLUE, 0, 0, 0, 0));

        assertEquals('A', new OfferTileSummary(0, 'A', 1, 0, 0, true, Color.BLUE, "Alice").id());
        assertEquals(0, new TurnOrderSpaceSummary(0, -1, Color.BLUE, "Alice").index());
    }

    @Test
    void serverEndpointValidatesFieldsAndAppliesDefaults() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerEndpoint(null, "127.0.0.1", 1, "service"));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerEndpoint(ConnectionType.SOCKET, null, 1, "service"));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerEndpoint(ConnectionType.SOCKET, " ", 1, "service"));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerEndpoint(ConnectionType.SOCKET, "127.0.0.1", 0, "service"));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerEndpoint(ConnectionType.SOCKET, "127.0.0.1", 65_536, "service"));

        assertEquals(NetworkDefaults.DEFAULT_RMI_SERVICE_NAME,
                new ServerEndpoint(ConnectionType.RMI, "localhost", 1, " ").serviceName());
        assertEquals(NetworkDefaults.DEFAULT_RMI_SERVICE_NAME,
                new ServerEndpoint(ConnectionType.RMI, "localhost", 1, null).serviceName());
        assertEquals(NetworkDefaults.DEFAULT_SOCKET_PORT, ServerEndpoint.socket("localhost").port());
        assertEquals(NetworkDefaults.DEFAULT_RMI_REGISTRY_PORT, ServerEndpoint.rmi("localhost").port());
        assertEquals(ConnectionType.SOCKET, ServerEndpoint.defaultEndpoint(ConnectionType.SOCKET, "localhost").connectionType());
        assertEquals(ConnectionType.RMI, ServerEndpoint.defaultEndpoint(ConnectionType.RMI, "localhost").connectionType());
    }

    @Test
    void networkDefaultsResolveRmiClientHostForLoopbackAndLanServers() throws Exception {
        Method method = NetworkDefaults.class.getDeclaredMethod("resolveRmiClientHost", String.class, String.class);
        method.setAccessible(true);

        assertEquals("127.0.0.1", method.invoke(null, "localhost", "10.0.0.8"));
        assertEquals("127.0.0.1", method.invoke(null, "127.0.0.1", "10.0.0.8"));
        assertEquals("10.0.0.8", method.invoke(null, "192.168.1.20", "10.0.0.8"));
        assertEquals(NetworkDefaults.detectLocalNetworkHost(), method.invoke(null, "192.168.1.20", null));
    }

    @Test
    void operationResultsAndViewUpdatesExecuteAgainstSocketContext() {
        GameOperationResult success = new GameOperationResult(true, null, null, null, null, false);
        assertEquals("Operation completed", success.message());
        assertEquals(OperationErrorCode.NONE, success.errorCode());

        GameOperationResult failure = new GameOperationResult(false, " ", null, null, null, false);
        assertEquals("Operation failed", failure.message());
        assertEquals(OperationErrorCode.NONE, failure.errorCode());

        GameOperationResult nickname = GameOperationResult.successNickname("ok", true);
        assertTrue(nickname.rejoinAvailable());
        assertEquals(OperationErrorCode.NONE, nickname.errorCode());

        LinkedBlockingQueue<GameOperationResult> responses = new LinkedBlockingQueue<>();
        success.messageExecute(new SocketGameClient.SocketClientContext(responses, null));
        assertEquals(success, responses.poll());

        GameSnapshot snapshot = snapshot("game", 2, 0, GameStatus.LOGIN, 0);
        assertThrows(IllegalArgumentException.class, () -> new GameViewUpdate(null, "message", snapshot));
        assertThrows(IllegalArgumentException.class, () -> new GameViewUpdate(" ", "message", snapshot));
        assertThrows(IllegalArgumentException.class, () -> new GameViewUpdate("game", "message", null));
        assertEquals("Aggiornamento tavolo", new GameViewUpdate("game", null, snapshot).message());
        assertEquals("Aggiornamento tavolo", new GameViewUpdate("game", " ", snapshot).message());

        List<GameServerMessage> received = new ArrayList<>();
        GameViewUpdate update = new GameViewUpdate("game", "updated", snapshot);
        update.messageExecute(new SocketGameClient.SocketClientContext(new LinkedBlockingQueue<>(), received::add));
        update.messageExecute(new SocketGameClient.SocketClientContext(new LinkedBlockingQueue<>(), null));
        assertEquals(List.of(update), received);
    }

    @Test
    void unsupportedServerMessagesAndNullClientFactoryFailFast() {
        GameServerMessage unsupported = new GameServerMessage() {
            @Override
            public void accept(GameServerMessageVisitor visitor) { }
        };

        IOException exception = assertThrows(IOException.class, () -> unsupported.messageExecute(null));
        assertTrue(exception.getMessage().contains("Unsupported server message"));
        assertThrows(IllegalArgumentException.class, () -> GameClientFactory.connect(null));
    }

    private static GameSnapshot snapshot(String gameName, int playerCount, int joinedPlayers, GameStatus status, int round) {
        return new GameSnapshot(gameName, playerCount, joinedPlayers, false, false, status, round, Era.FIRST,
                null, null, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
