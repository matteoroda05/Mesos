package it.polimi.ingsw.mesos.Network;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.PassRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.RMI.RmiGameServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RmiGameIntegrationTest {
    @Test
    void rmiClientsCanCreateJoinAndPlayATurn() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosTestService");
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestService"));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestService"))) {
            NetworkTestSupport.assertBasicRemoteTurn(aliceClient, bobClient, "rmi-room");
        }
    }

    @Test
    void rmiServerExposesAdvertisedHost() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosHostService")) {
            assertNotNull(rmiServer.getAdvertisedHost());
            assertFalse(rmiServer.getAdvertisedHost().isBlank());
        }
    }

    @Test
    void rmiClientFactoryConnectsViaConnectionTypeOverload() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosFactoryService");
             GameClient client = GameClientFactory.connect(
                     ConnectionType.RMI, "127.0.0.1", rmiServer.getPort(), "MesosFactoryService")) {
            assertTrue(client.createGame(new CreateGameRequest("rmi-factory-room", 2)).success());
        }
    }

    @Test
    void rmiClientsReceivePushUpdates() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosTestServicePush");
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServicePush"));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServicePush"))) {
            NetworkTestSupport.assertPushUpdates(aliceClient, bobClient, "rmi-push-room");
        }
    }

    @Test
    void rmiClientsSeeOnlyOpenGamesInLobby() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosTestServiceLobby");
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServiceLobby"));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServiceLobby"));
             GameClient observerClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServiceLobby"))) {
            NetworkTestSupport.assertLobbyVisibilityAndFullGameRejection(
                    aliceClient,
                    bobClient,
                    observerClient,
                    "rmi-open-room",
                    "rmi-full-room"
            );
        }
    }

    @Test
    void rmiClientsHonorWaitingRoomColorSelectionBeforeGameStart() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosTestServiceColor");
             GameClient aliceClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServiceColor"));
             GameClient bobClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServiceColor"));
             GameClient carolClient = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServiceColor"))) {
            NetworkTestSupport.assertWaitingRoomColorSelection(
                    aliceClient,
                    bobClient,
                    carolClient,
                    "rmi-color-room"
            );
        }
    }

    @Test
    void rmiClientHandlesSubscriptionGuardsAndFailures() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosTestServiceGuards");
             GameClient client = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServiceGuards"))) {
            assertThrows(IllegalArgumentException.class,
                    () -> client.subscribe(new SubscribeGameRequest("rmi-guard-room", "Alice"), null));

            assertTrue(client.unsubscribe(new UnsubscribeGameRequest("rmi-guard-room", "Alice")).success());
            assertTrue(client.createGame(new CreateGameRequest("rmi-guard-room", 2)).success());

            var failedSubscription = client.subscribe(
                    new SubscribeGameRequest("rmi-guard-room", "Alice"),
                    update -> { });
            assertFalse(failedSubscription.success());
            assertEquals(OperationErrorCode.PLAYER_NOT_IN_GAME, failedSubscription.errorCode());
        }
    }

    @Test
    void rmiClientForwardsPassRequestsThroughServerAdapter() throws IOException {
        MesosServerCore serverCore = new MesosServerCore();
        int rmiPort = NetworkTestSupport.findFreePort();

        try (RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiPort, "MesosTestServicePass");
             GameClient client = GameClientFactory.connect(ServerEndpoint.rmi("127.0.0.1", rmiServer.getPort(), "MesosTestServicePass"))) {
            GameOperationResult result = client.pass(new PassRequest("missing-pass-room", "Alice"));

            assertFalse(result.success());
            assertEquals(OperationErrorCode.GAME_NOT_FOUND, result.errorCode());
        }
    }
}
