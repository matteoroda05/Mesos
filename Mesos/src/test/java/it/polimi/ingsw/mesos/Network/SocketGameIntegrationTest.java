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
}
