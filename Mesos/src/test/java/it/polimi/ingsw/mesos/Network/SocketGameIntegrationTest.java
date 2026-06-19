package it.polimi.ingsw.mesos.Network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseColorRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.HeartbeatRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.LobbyRequest;
import it.polimi.ingsw.mesos.Network.Requests.NicknameRequest;
import it.polimi.ingsw.mesos.Network.Requests.PassRequest;
import it.polimi.ingsw.mesos.Network.Requests.RankingsRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

import static org.junit.jupiter.api.Assertions.*;

class SocketGameIntegrationTest {
    @Test
    void socketClientsCanCreateJoinAndPlayATurn() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        try (SocketGameServer socketServer = new SocketGameServer(serverCore, 0);
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            NetworkTestSupport.assertBasicRemoteTurn(aliceClient, bobClient, "socket-room");
        }
    }

    @Test
    void socketClientFactoryConnectsViaConnectionTypeOverload() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        try (SocketGameServer socketServer = new SocketGameServer(serverCore, 0);
             GameClient client = GameClientFactory.connect(ConnectionType.SOCKET, "127.0.0.1", socketServer.getPort())) {
            assertTrue(client.createGame(new CreateGameRequest("socket-factory-room", 2)).success());
        }
    }

    @Test
    void socketClientsReceivePushUpdates() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        try (SocketGameServer socketServer = new SocketGameServer(serverCore, 0);
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            NetworkTestSupport.assertPushUpdates(aliceClient, bobClient, "socket-push-room");
        }
    }

    @Test
    void socketClientsSeeOnlyOpenGamesInLobby() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        try (SocketGameServer socketServer = new SocketGameServer(serverCore, 0);
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()));
             GameClient observerClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            NetworkTestSupport.assertLobbyVisibilityAndFullGameRejection(
                    aliceClient,
                    bobClient,
                    observerClient,
                    "socket-open-room",
                    "socket-full-room"
            );
        }
    }

    @Test
    void socketClientsHonorWaitingRoomColorSelectionBeforeGameStart() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        try (SocketGameServer socketServer = new SocketGameServer(serverCore, 0);
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()));
             GameClient carolClient = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            NetworkTestSupport.assertWaitingRoomColorSelection(
                    aliceClient,
                    bobClient,
                    carolClient,
                    "socket-color-room"
            );
        }
    }

    @Test
    void socketClientHandlesSubscriptionGuardsAndFailures() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();

        try (SocketGameServer socketServer = new SocketGameServer(serverCore, 0);
             GameClient client = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            assertThrows(IllegalArgumentException.class,
                    () -> client.subscribe(new SubscribeGameRequest("socket-guard-room", "Alice"), null));

            assertTrue(client.unsubscribe(new UnsubscribeGameRequest("socket-guard-room", "Alice")).success());
            assertTrue(client.createGame(new CreateGameRequest("socket-guard-room", 2)).success());

            var failedSubscription = client.subscribe(
                    new SubscribeGameRequest("socket-guard-room", "Alice"),
                    update -> { });
            assertFalse(failedSubscription.success());
            assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, failedSubscription.errorCode());
        }
    }

    @Test
    void socketClientReplacesSubscriptionsAndCloseUnsubscribesActiveOne() throws IOException {
        RecordingServerService service = new RecordingServerService();

        try (SocketGameServer socketServer = new SocketGameServer(service, 0);
             GameClient client = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            assertTrue(client.subscribe(new SubscribeGameRequest("room-a", "Alice"), update -> { }).success());
            assertTrue(client.subscribe(new SubscribeGameRequest("room-b", "Alice"), update -> { }).success());
            assertEquals(2, service.subscribeCalls);
            assertEquals(1, service.unsubscribeCalls);
            assertEquals("room-a", service.lastUnsubscribe.gameName());
        }

        assertEquals(2, service.unsubscribeCalls);
        assertEquals("room-b", service.lastUnsubscribe.gameName());
    }

    @Test
    void socketClientKeepsActiveSubscriptionWhenUnsubscribeFails() throws IOException {
        RecordingServerService service = new RecordingServerService();
        service.unsubscribeSuccess = false;

        try (SocketGameServer socketServer = new SocketGameServer(service, 0);
             GameClient client = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            assertTrue(client.subscribe(new SubscribeGameRequest("sticky-room", "Alice"), update -> { }).success());

            GameOperationResult failedUnsubscribe =
                    client.unsubscribe(new UnsubscribeGameRequest("sticky-room", "Alice"));

            assertFalse(failedUnsubscribe.success());
            assertEquals(1, service.unsubscribeCalls);
        }

        assertEquals(2, service.unsubscribeCalls);
    }

    @Test
    void socketClientSendsPassRequests() throws IOException {
        RecordingServerService service = new RecordingServerService();

        try (SocketGameServer socketServer = new SocketGameServer(service, 0);
             GameClient client = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", socketServer.getPort()))) {
            GameOperationResult result = client.pass(new PassRequest("pass-room", "Alice"));

            assertTrue(result.success());
            assertEquals("pass", result.message());
            assertEquals(1, service.passCalls);
            assertEquals("pass-room", service.lastPass.gameName());
            assertEquals("Alice", service.lastPass.nickname());
        }
    }

    @Test
    void socketServerReturnsFailureForUnsupportedPayload() throws Exception {
        try (SocketGameServer socketServer = new SocketGameServer(new RecordingServerService(), 0);
             Socket socket = new Socket("127.0.0.1", socketServer.getPort());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            socket.setSoTimeout(2_000);
            output.flush();

            output.writeObject("not a game request");
            output.flush();

            Object response = input.readObject();
            assertInstanceOf(GameOperationResult.class, response);
            GameOperationResult result = (GameOperationResult) response;
            assertFalse(result.success());
            assertEquals("Unsupported request payload", result.message());
        }
    }

    @Test
    void socketServerConnectionReplacesSubscriptionAndCleansUpActiveSubscription() throws Exception {
        RecordingServerService service = new RecordingServerService();

        try (SocketGameServer socketServer = new SocketGameServer(service, 0);
             Socket socket = new Socket("127.0.0.1", socketServer.getPort());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            socket.setSoTimeout(2_000);
            output.flush();

            writeRequest(output, input, new SubscribeGameRequest("room-a", "Alice"));
            writeRequest(output, input, new SubscribeGameRequest("room-b", "Alice"));
            assertEquals(2, service.subscribeCalls);
            assertEquals(1, service.unsubscribeCalls);
            assertEquals("room-a", service.lastUnsubscribe.gameName());

            writeRequest(output, input, new UnsubscribeGameRequest("room-c", "Alice"));
            assertEquals(2, service.unsubscribeCalls);
        }

        waitForUnsubscribeCalls(service, 3);
        assertEquals("room-b", service.lastUnsubscribe.gameName());
    }

    @Test
    void socketServerHandleClientCoversClosedSocketAndStoppedServerBranches() throws Exception {
        try (SocketGameServer socketServer = new SocketGameServer(new RecordingServerService(), 0)) {
            Socket closedSocket = new Socket();
            closedSocket.close();
            invokeHandleClient(socketServer, closedSocket);

            setServerRunning(socketServer, false);
            try (ServerSocket bridge = new ServerSocket(0);
                 Socket clientSocket = new Socket("127.0.0.1", bridge.getLocalPort());
                 Socket serverSideSocket = bridge.accept();
                 ObjectOutputStream clientOutput = new ObjectOutputStream(clientSocket.getOutputStream())) {
                clientOutput.flush();
                invokeHandleClient(socketServer, serverSideSocket);
            }
        }
    }

    @Test
    void socketServerAcceptLoopCoversStoppedAndAcceptFailureBranches() throws Exception {
        try (SocketGameServer socketServer = new SocketGameServer(new RecordingServerService(), 0)) {
            setServerRunning(socketServer, false);
            getServerSocket(socketServer).close();
            invokeAcceptLoop(socketServer);

            setServerSocket(socketServer, new ThrowingServerSocket(new SocketException("closed"), null));
            setServerRunning(socketServer, true);
            assertThrows(IllegalStateException.class, () -> invokeAcceptLoop(socketServer));

            setServerSocket(socketServer, new ThrowingServerSocket(
                    new SocketException("closed after stop"),
                    () -> setServerRunningQuietly(socketServer, false)
            ));
            setServerRunning(socketServer, true);
            invokeAcceptLoop(socketServer);

            setServerSocket(socketServer, new ThrowingServerSocket(new IOException("accept failed"), null));
            setServerRunning(socketServer, true);
            assertThrows(IllegalStateException.class, () -> invokeAcceptLoop(socketServer));

            setServerSocket(socketServer, new ThrowingServerSocket(
                    new IOException("accept failed after stop"),
                    () -> setServerRunningQuietly(socketServer, false)
            ));
            setServerRunning(socketServer, true);
            invokeAcceptLoop(socketServer);
        }
    }

    @Test
    void socketServerHandleClientCoversBrokenStreamBranches() throws Exception {
        try (SocketGameServer socketServer = new SocketGameServer(new RecordingServerService(), 0)) {
            ExecutionException runningFailure = assertThrows(ExecutionException.class,
                    () -> invokeHandleClientWithPrimitivePayload(socketServer, true));
            assertInstanceOf(IllegalStateException.class, runningFailure.getCause());

            invokeHandleClientWithPrimitivePayload(socketServer, false);
        }
    }

    @Test
    void clientConnectionSubscriptionBranchesCanBeExercisedDirectly() throws Exception {
        RecordingServerService service = new RecordingServerService();

        try (SocketGameServer socketServer = new SocketGameServer(service, 0)) {
            Object connection = newClientConnection(socketServer);
            Method subscribe = connection.getClass().getMethod("subscribe", SubscribeGameRequest.class);
            Method unsubscribe = connection.getClass().getMethod("unsubscribe", UnsubscribeGameRequest.class);
            Method cleanup = connection.getClass().getDeclaredMethod("cleanup");
            cleanup.setAccessible(true);

            unsubscribe.invoke(connection, new UnsubscribeGameRequest("room-a", "Alice"));
            cleanup.invoke(connection);
            assertEquals(0, service.unsubscribeCalls);

            subscribe.invoke(connection, new SubscribeGameRequest("room-a", "Alice"));
            subscribe.invoke(connection, new SubscribeGameRequest("room-a", "Alice"));
            assertEquals(0, service.unsubscribeCalls);

            subscribe.invoke(connection, new SubscribeGameRequest("room-a", "Bob"));
            assertEquals(1, service.unsubscribeCalls);
            assertEquals("Alice", service.lastUnsubscribe.nickname());

            unsubscribe.invoke(connection, new UnsubscribeGameRequest("room-a", "Charlie"));
            assertEquals(1, service.unsubscribeCalls);

            unsubscribe.invoke(connection, new UnsubscribeGameRequest("room-a", "Bob"));
            cleanup.invoke(connection);
            assertEquals(1, service.unsubscribeCalls);
        }
    }

    @Test
    void socketClientReportsFailureWhenServerSendsUnexpectedMessageType() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Future<?> server = executor.submit(() -> {
                try (Socket socket = serverSocket.accept();
                     ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                     ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
                    output.flush();
                    input.readObject();
                    output.writeObject("not a server message");
                    output.flush();
                } catch (IOException | ClassNotFoundException exception) {
                    throw new AssertionError(exception);
                }
            });

            try (GameClient client = GameClientFactory.connect(ServerEndpoint.socket("127.0.0.1", serverSocket.getLocalPort()))) {
                GameOperationResult result = client.getLobby();

                assertFalse(result.success());
                assertTrue(result.message().contains("Unexpected response type"));
            }

            server.get();
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class RecordingServerService implements MesosServerService {
        private volatile int subscribeCalls;
        private volatile int unsubscribeCalls;
        private volatile int passCalls;
        private volatile boolean unsubscribeSuccess = true;
        private volatile UnsubscribeGameRequest lastUnsubscribe;
        private volatile PassRequest lastPass;

        @Override
        public GameOperationResult getLobby() {
            return GameOperationResult.successLobby("lobby", new LobbySnapshot(java.util.List.of()));
        }

        @Override
        public GameOperationResult nickname(NicknameRequest request) {
            return GameOperationResult.successNickname("nickname", false);
        }

        @Override
        public GameOperationResult createGame(CreateGameRequest request) {
            return GameOperationResult.success("created", null);
        }

        @Override
        public GameOperationResult joinGame(JoinGameRequest request) {
            return GameOperationResult.success("joined", null);
        }

        @Override
        public GameOperationResult getSnapshot(SnapshotRequest request) {
            return GameOperationResult.success("snapshot", null);
        }

        @Override
        public GameOperationResult chooseOffer(ChooseOfferRequest request) {
            return GameOperationResult.success("offer", null);
        }

        @Override
        public GameOperationResult chooseCard(ChooseCardRequest request) {
            return GameOperationResult.success("card", null);
        }

        @Override
        public GameOperationResult chooseColor(ChooseColorRequest request) {
            return GameOperationResult.success("color", null);
        }

        @Override
        public GameOperationResult pass(PassRequest request) {
            passCalls++;
            lastPass = request;
            return GameOperationResult.success("pass", null);
        }

        @Override
        public GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink) {
            subscribeCalls++;
            return GameOperationResult.success("subscribed", null);
        }

        @Override
        public GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink) {
            unsubscribeCalls++;
            lastUnsubscribe = request;
            return unsubscribeSuccess
                    ? GameOperationResult.success("unsubscribed", null)
                    : GameOperationResult.failure("unsubscribe failed", null);
        }

        @Override
        public GameOperationResult heartbeat(HeartbeatRequest request) {
            return GameOperationResult.success("heartbeat", null);
        }

        @Override
        public GameOperationResult getRankings(RankingsRequest request) {
            return GameOperationResult.successRankings("rankings", null);
        }
    }

    private static void writeRequest(
            ObjectOutputStream output,
            ObjectInputStream input,
            GameRequest request
    ) throws Exception {
        output.writeObject(request);
        output.flush();
        Object response = input.readObject();
        assertInstanceOf(GameOperationResult.class, response);
        assertTrue(((GameOperationResult) response).success());
    }

    private static void waitForUnsubscribeCalls(RecordingServerService service, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (service.unsubscribeCalls < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, service.unsubscribeCalls);
    }

    private static Object newClientConnection(SocketGameServer socketServer) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        Constructor<?> constructor = SocketGameServer.ClientConnection.class
                .getDeclaredConstructor(SocketGameServer.class, ObjectOutputStream.class);
        constructor.setAccessible(true);
        return constructor.newInstance(socketServer, output);
    }

    private static void invokeHandleClient(SocketGameServer socketServer, Socket socket) throws Exception {
        Method handleClient = SocketGameServer.class.getDeclaredMethod("handleClient", Socket.class);
        handleClient.setAccessible(true);
        invokeUnwrapped(handleClient, socketServer, socket);
    }

    private static void invokeAcceptLoop(SocketGameServer socketServer) throws Exception {
        Method acceptLoop = SocketGameServer.class.getDeclaredMethod("acceptLoop");
        acceptLoop.setAccessible(true);
        invokeUnwrapped(acceptLoop, socketServer);
    }

    private static void setServerRunning(SocketGameServer socketServer, boolean running) throws Exception {
        java.lang.reflect.Field runningField = SocketGameServer.class.getDeclaredField("running");
        runningField.setAccessible(true);
        ((AtomicBoolean) runningField.get(socketServer)).set(running);
    }

    private static void setServerRunningQuietly(SocketGameServer socketServer, boolean running) {
        try {
            setServerRunning(socketServer, running);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static ServerSocket getServerSocket(SocketGameServer socketServer) throws Exception {
        java.lang.reflect.Field serverSocketField = SocketGameServer.class.getDeclaredField("serverSocket");
        serverSocketField.setAccessible(true);
        return (ServerSocket) serverSocketField.get(socketServer);
    }

    private static void setServerSocket(SocketGameServer socketServer, ServerSocket replacement) throws Exception {
        java.lang.reflect.Field serverSocketField = SocketGameServer.class.getDeclaredField("serverSocket");
        serverSocketField.setAccessible(true);
        serverSocketField.set(socketServer, replacement);
    }

    private static void invokeUnwrapped(Method method, Object target, Object... args) throws Exception {
        try {
            method.invoke(target, args);
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

    private static void invokeHandleClientWithPrimitivePayload(
            SocketGameServer socketServer,
            boolean runningDuringFailure
    ) throws Exception {
        try (ServerSocket bridge = new ServerSocket(0);
             Socket clientSocket = new Socket("127.0.0.1", bridge.getLocalPort());
             Socket serverSideSocket = bridge.accept();
             ObjectOutputStream clientOutput = new ObjectOutputStream(clientSocket.getOutputStream())) {
            clientOutput.flush();
            setServerRunning(socketServer, true);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> server = executor.submit(() -> {
                    invokeHandleClient(socketServer, serverSideSocket);
                    return null;
                });
                if (!runningDuringFailure) {
                    waitForServerInputToBeReady();
                    setServerRunning(socketServer, false);
                }
                clientOutput.writeInt(42);
                clientOutput.flush();
                server.get();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static void waitForServerInputToBeReady() throws InterruptedException {
        Thread.sleep(50);
    }

    private static final class ThrowingServerSocket extends ServerSocket {
        private final IOException exception;
        private final Runnable beforeThrow;

        private ThrowingServerSocket(IOException exception, Runnable beforeThrow) throws IOException {
            this.exception = exception;
            this.beforeThrow = beforeThrow;
        }

        @Override
        public Socket accept() throws IOException {
            if (beforeThrow != null) {
                beforeThrow.run();
            }
            throw exception;
        }
    }
}
