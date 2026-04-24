package it.polimi.ingsw.mesos.Network;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

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
}
