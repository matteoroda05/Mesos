package it.polimi.ingsw.mesos.Network;

import java.io.IOException;

import it.polimi.ingsw.mesos.Network.RMI.RmiGameClient;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

public final class GameClientFactory {
    private GameClientFactory() {
    }

    public static GameClient connect(ConnectionType connectionType, String host) throws IOException {
        return connect(ServerEndpoint.defaultEndpoint(connectionType, host));
    }

    public static GameClient connect(ConnectionType connectionType, String host, int port) throws IOException {
        return connect(new ServerEndpoint(connectionType, host, port, NetworkDefaults.DEFAULT_RMI_SERVICE_NAME));
    }

    public static GameClient connect(ConnectionType connectionType, String host, int port, String serviceName)
            throws IOException {
        return connect(new ServerEndpoint(connectionType, host, port, serviceName));
    }

    public static GameClient connect(ServerEndpoint endpoint) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint cannot be null");
        }

        return switch (endpoint.connectionType()) {
            case SOCKET -> new SocketGameClient(endpoint.host(), endpoint.port());
            case RMI -> new RmiGameClient(endpoint.host(), endpoint.port(), endpoint.serviceName());
        };
    }
}
