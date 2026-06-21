package it.polimi.ingsw.mesos.network.config;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

/**
 * Shared network defaults and host-detection helpers.
 */
public final class NetworkDefaults {
    public static final String DEFAULT_RMI_HOST = "127.0.0.1";
    public static final int DEFAULT_SOCKET_PORT = 12_345;
    public static final int DEFAULT_RMI_REGISTRY_PORT = 1_099;
    public static final int DEFAULT_RMI_CALLBACK_PORT = 0;
    public static final String DEFAULT_RMI_SERVICE_NAME = "MesosGameService";
    public static final String RMI_CALLBACK_PORT_PROPERTY = "mesos.rmi.callback.port";
    public static final long HEARTBEAT_INTERVAL_MILLIS = 2_000;
    public static final long HEARTBEAT_TIMEOUT_MILLIS = 8_000;
    public static final long SINGLE_PLAYER_WIN_TIMEOUT_MILLIS = 60_000;

    /**
     * Prevents instantiation of the constants utility.
     */
    private NetworkDefaults() {
    }

    /**
     * Resolves the host advertised by the RMI server.
     * A non-blank configured value wins after trimming; otherwise the first
     * usable local IPv4 address is used, with loopback as a fallback.
     *
     * @param configuredHost optional configured host
     * @return host to advertise to RMI clients
     */
    public static String resolveRmiServerHost(String configuredHost) {
        if (configuredHost != null && !configuredHost.isBlank()) {
            return configuredHost.trim();
        }

        return detectLocalNetworkHost();
    }

    /**
     * Sets the JVM RMI hostname property using the resolved server host.
     *
     * @param configuredHost optional configured host
     */
    public static void configureRmiServerHostname(String configuredHost) {
        System.setProperty("java.rmi.server.hostname", resolveRmiServerHost(configuredHost));
    }

    /**
     * Resolves the host advertised by an RMI client callback.
     * Loopback server connections keep the callback on loopback; all other
     * connections advertise the first usable local IPv4 address.
     *
     * @param serverHost host used to connect to the RMI server
     * @return callback host to advertise back to the server
     */
    public static String resolveRmiClientHost(String serverHost) {
        return resolveRmiClientHost(serverHost, null);
    }

    /**
     * Resolves the host advertised by an RMI client callback.
     * Loopback server connections keep the callback on loopback; otherwise the
     * address actually used to reach the server is preferred, falling back to
     * the first usable local IPv4 address.
     *
     * @param serverHost host used to connect to the RMI server
     * @param routedLocalHost local address chosen by the OS route to the server
     * @return callback host to advertise back to the server
     */
    public static String resolveRmiClientHost(String serverHost, String routedLocalHost) {
        if (serverHost != null && isLoopbackHost(serverHost)) {
            return DEFAULT_RMI_HOST;
        }
        if (routedLocalHost != null && !routedLocalHost.isBlank()) {
            return routedLocalHost.trim();
        }
        return detectLocalNetworkHost();
    }

    /**
     * Sets the JVM RMI hostname property for client-side callback exports.
     *
     * @param serverHost host used to connect to the RMI server
     */
    public static void configureRmiClientHostname(String serverHost) {
        System.setProperty("java.rmi.server.hostname", resolveRmiClientHost(serverHost));
    }

    /**
     * Resolves the optional RMI client callback port.
     * Port 0 keeps the default anonymous-port RMI behavior.
     *
     * @return configured callback port, or 0 when absent/invalid
     */
    public static int rmiCallbackPort() {
        int port = Integer.getInteger(RMI_CALLBACK_PORT_PROPERTY, DEFAULT_RMI_CALLBACK_PORT);
        if (port < 0 || port > 65_535) {
            return DEFAULT_RMI_CALLBACK_PORT;
        }
        return port;
    }

    /**
     * Detects a usable non-loopback IPv4 address for LAN clients.
     * Virtual, loopback and down interfaces are skipped. If interface
     * enumeration fails or no suitable address exists, loopback is returned.
     *
     * @return detected host address, or {@link #DEFAULT_RMI_HOST}
     */
    public static String detectLocalNetworkHost() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (isUsableIpv4Address(address)) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        return DEFAULT_RMI_HOST;
    }

    /**
     * Checks whether an address is a usable advertised IPv4 address.
     *
     * @param address address to inspect
     * @return true when the address is IPv4 and not any-local, loopback or
     *         link-local
     */
    private static boolean isUsableIpv4Address(InetAddress address) {
        // IPv4 raw addresses are 4 bytes long; IPv6 addresses are 16 bytes.
        return address.getAddress().length == 4
                && !address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress();
    }

    /**
     * Checks whether a host string refers to the local loopback interface.
     *
     * @param host host string to inspect
     * @return true when the host is localhost or a loopback literal
     */
    private static boolean isLoopbackHost(String host) {
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
                || normalized.startsWith("127.")
                || normalized.equals("::1")
                || normalized.equals("0:0:0:0:0:0:0:1");
    }
}
