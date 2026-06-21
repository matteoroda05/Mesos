package it.polimi.ingsw.mesos.Network;

import java.io.IOException;

import it.polimi.ingsw.mesos.Network.RMI.RmiGameClient;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameClient;

/**
 * Factory for concrete client implementations.
 */
public final class GameClientFactory {
    /**
     * Prevents instantiation of the utility factory.
     */
    private GameClientFactory() {
    }

    /**
     * Connects to a server using the default port for the selected transport.
     *
     * @param connectionType transport to use
     * @param host remote host
     * @return connected game client
     * @throws IOException if the concrete client cannot connect
     * @throws IllegalArgumentException if the generated endpoint is invalid
     */
    public static GameClient connect(ConnectionType connectionType, String host) throws IOException {
        return connect(ServerEndpoint.defaultEndpoint(connectionType, host));
    }

    /**
     * Connects to a server using the default RMI service name.
     *
     * @param connectionType transport to use
     * @param host remote host
     * @param port remote port
     * @return connected game client
     * @throws IOException if the concrete client cannot connect
     * @throws IllegalArgumentException if the endpoint data is invalid
     */
    public static GameClient connect(ConnectionType connectionType, String host, int port) throws IOException {
        return connect(new ServerEndpoint(connectionType, host, port, NetworkDefaults.DEFAULT_RMI_SERVICE_NAME));
    }

    /**
     * Connects to a server using an explicit RMI service name.
     *
     * @param connectionType transport to use
     * @param host remote host
     * @param port remote port
     * @param serviceName RMI service name; blank values are normalized by
     *                    {@link ServerEndpoint}
     * @return connected game client
     * @throws IOException if the concrete client cannot connect
     * @throws IllegalArgumentException if the endpoint data is invalid
     */
    public static GameClient connect(ConnectionType connectionType, String host, int port, String serviceName)
            throws IOException {
        return connect(new ServerEndpoint(connectionType, host, port, serviceName));
    }

    /**
     * Connects using a fully specified endpoint.
     *
     * @param endpoint endpoint describing transport, host, port and service
     * @return connected game client
     * @throws IOException if the concrete client cannot connect
     * @throws IllegalArgumentException if endpoint is null
     */
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
