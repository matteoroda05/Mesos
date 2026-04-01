package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

public record ServerEndpoint(
        ConnectionType connectionType,
        String host,
        int port,
        String serviceName
) implements Serializable {

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

    public static ServerEndpoint socket(String host, int port) {
        return new ServerEndpoint(ConnectionType.SOCKET, host, port, NetworkDefaults.DEFAULT_RMI_SERVICE_NAME);
    }

    public static ServerEndpoint rmi(String host, int port) {
        return new ServerEndpoint(ConnectionType.RMI, host, port, NetworkDefaults.DEFAULT_RMI_SERVICE_NAME);
    }

    public static ServerEndpoint rmi(String host, int port, String serviceName) {
        return new ServerEndpoint(ConnectionType.RMI, host, port, serviceName);
    }
}
