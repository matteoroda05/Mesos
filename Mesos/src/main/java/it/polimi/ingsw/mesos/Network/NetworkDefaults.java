package it.polimi.ingsw.mesos.Network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class NetworkDefaults {
    public static final String DEFAULT_RMI_HOST = "127.0.0.1";
    public static final int DEFAULT_SOCKET_PORT = 12_345;
    public static final int DEFAULT_RMI_REGISTRY_PORT = 1_099;
    public static final String DEFAULT_RMI_SERVICE_NAME = "MesosGameService";
    public static final long HEARTBEAT_INTERVAL_MILLIS = 2_000;
    public static final long HEARTBEAT_TIMEOUT_MILLIS = 8_000;
    public static final long SINGLE_PLAYER_WIN_TIMEOUT_MILLIS = 60_000;

    private NetworkDefaults() {
    }

    public static String resolveRmiServerHost(String configuredHost) {
        if (configuredHost != null && !configuredHost.isBlank()) {
            return configuredHost.trim();
        }

        return detectLocalNetworkHost();
    }

    public static void configureRmiServerHostname(String configuredHost) {
        System.setProperty("java.rmi.server.hostname", resolveRmiServerHost(configuredHost));
    }

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

    private static boolean isUsableIpv4Address(InetAddress address) {
        return address instanceof Inet4Address
                && !address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress();
    }
}
