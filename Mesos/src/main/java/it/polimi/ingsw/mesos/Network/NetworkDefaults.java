package it.polimi.ingsw.mesos.Network;

public final class NetworkDefaults {
    public static final String DEFAULT_RMI_HOST = "127.0.0.1";
    public static final int DEFAULT_SOCKET_PORT = 12_345;
    public static final int DEFAULT_RMI_REGISTRY_PORT = 1_099;
    public static final String DEFAULT_RMI_SERVICE_NAME = "MesosGameService";
    public static final long HEARTBEAT_INTERVAL_MILLIS = 2_500L;
    public static final long HEARTBEAT_TIMEOUT_MILLIS = 10_000L;
    public static final long HEARTBEAT_CHECK_INTERVAL_MILLIS = 1_000L;
    public static final long DISCONNECTION_WIN_TIMEOUT_MILLIS = 60_000L;

    private NetworkDefaults() {
    }
}
