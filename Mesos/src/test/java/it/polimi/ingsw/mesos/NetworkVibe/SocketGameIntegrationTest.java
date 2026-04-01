package it.polimi.ingsw.mesos.NetworkVibe;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.NetworkVibe.Socket.SocketGameServer;

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
}
