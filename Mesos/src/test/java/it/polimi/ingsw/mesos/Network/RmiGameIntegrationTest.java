package it.polimi.ingsw.mesos.Network;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Network.RMI.RmiGameServer;

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
}
