package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

/**
 * Serializable network endpoint used to create clients.
 *
 * @param connectionType transport type
 * @param host remote host
 * @param port remote port
 * @param serviceName RMI service name, normalized to the default when
 *                    null/blank
 */
public record ServerEndpoint(
        ConnectionType connectionType,
        String host,
        int port,
        String serviceName
) implements Serializable {

    /**
     * Validates endpoint data and normalizes blank RMI service names.
     *
     * @throws IllegalArgumentException if connection type is null, host is
     *                                  null/blank, or port is outside
     *                                  {@code 1..65535}
     */
    public ServerEndpoint {
        if (connectionType == null) {
            throw new IllegalArgumentException("connectionType cannot be null");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (serviceName == null || serviceName.isBlank()) {
            serviceName = NetworkDefaults.DEFAULT_RMI_SERVICE_NAME;
        }
    }

    /**
     * Creates a socket endpoint with an explicit port.
     *
     * @param host remote host
     * @param port socket port
     * @return socket endpoint
     */
    public static ServerEndpoint socket(String host, int port) {
        return new ServerEndpoint(ConnectionType.SOCKET, host, port, NetworkDefaults.DEFAULT_RMI_SERVICE_NAME);
    }

    /**
     * Creates a socket endpoint with the default socket port.
     *
     * @param host remote host
     * @return socket endpoint
     */
    public static ServerEndpoint socket(String host) {
        return socket(host, NetworkDefaults.DEFAULT_SOCKET_PORT);
    }

    /**
     * Creates an RMI endpoint with an explicit registry port and default service
     * name.
     *
     * @param host remote host
     * @param port RMI registry port
     * @return RMI endpoint
     */
    public static ServerEndpoint rmi(String host, int port) {
        return new ServerEndpoint(ConnectionType.RMI, host, port, NetworkDefaults.DEFAULT_RMI_SERVICE_NAME);
    }

    /**
     * Creates an RMI endpoint with default registry port and service name.
     *
     * @param host remote host
     * @return RMI endpoint
     */
    public static ServerEndpoint rmi(String host) {
        return rmi(host, NetworkDefaults.DEFAULT_RMI_REGISTRY_PORT, NetworkDefaults.DEFAULT_RMI_SERVICE_NAME);
    }

    /**
     * Creates an RMI endpoint with explicit registry port and service name.
     *
     * @param host remote host
     * @param port RMI registry port
     * @param serviceName service name; null/blank values become the default
     * @return RMI endpoint
     */
    public static ServerEndpoint rmi(String host, int port, String serviceName) {
        return new ServerEndpoint(ConnectionType.RMI, host, port, serviceName);
    }

    /**
     * Creates a default endpoint for the selected transport.
     *
     * @param connectionType transport type
     * @param host remote host
     * @return socket or RMI endpoint with transport defaults
     * @throws NullPointerException if connectionType is null
     */
    public static ServerEndpoint defaultEndpoint(ConnectionType connectionType, String host) {
        return switch (connectionType) {
            case SOCKET -> socket(host);
            case RMI -> rmi(host);
        };
    }
}
