package it.polimi.ingsw.mesos.Network;

import java.util.concurrent.CountDownLatch;

import it.polimi.ingsw.mesos.Network.RMI.RmiGameServer;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public final class MesosServerLauncher {
    private MesosServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        int socketPort = args.length > 0 ? Integer.parseInt(args[0]) : NetworkDefaults.DEFAULT_SOCKET_PORT;
        int rmiPort = args.length > 1 ? Integer.parseInt(args[1]) : NetworkDefaults.DEFAULT_RMI_REGISTRY_PORT;
        String serviceName = args.length > 2 ? args[2] : NetworkDefaults.DEFAULT_RMI_SERVICE_NAME;
        String rmiHost = args.length > 3 ? args[3] : NetworkDefaults.DEFAULT_RMI_HOST;

        MesosServerCore serverCore = new MesosServerCore();

        try (SocketGameServer socketServer = new SocketGameServer(serverCore, socketPort);
             RmiGameServer rmiServer = new RmiGameServer(serverCore, rmiHost, rmiPort, serviceName)) {
            System.out.println("Socket server listening on port " + socketServer.getPort());
            System.out.println("RMI registry listening on port " + rmiServer.getPort()
                    + " with service '" + serviceName + "' on host " + rmiHost);
            new CountDownLatch(1).await();
        }
    }
}
