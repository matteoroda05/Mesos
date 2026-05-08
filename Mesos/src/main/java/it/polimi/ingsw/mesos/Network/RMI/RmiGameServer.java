package it.polimi.ingsw.mesos.Network.RMI;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameUpdateSink;
import it.polimi.ingsw.mesos.Network.Requests.HeartbeatRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;

public class RmiGameServer implements AutoCloseable {
    private final Registry registry;
    private final RemoteMesosServer remoteServer;
    private final String serviceName;
    private final String advertisedHost;
    private final int port;

    public RmiGameServer(MesosServerService serverService, int port, String serviceName) throws RemoteException {
        this(serverService, NetworkDefaults.DEFAULT_RMI_HOST, port, serviceName);
    }

    public RmiGameServer(MesosServerService serverService, String host, int port, String serviceName)
            throws RemoteException {
        if (serverService == null) {
            throw new IllegalArgumentException("serverService cannot be null");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName cannot be null or blank");
        }

        NetworkDefaults.configureRmiServerHostname(host);

        this.port = port;
        this.serviceName = serviceName;
        this.advertisedHost = System.getProperty("java.rmi.server.hostname");
        this.registry = LocateRegistry.createRegistry(port);
        this.remoteServer = new RmiServerAdapter(serverService);
        registry.rebind(serviceName, remoteServer);
    }

    public int getPort() {
        return port;
    }

    public String getAdvertisedHost() {
        return advertisedHost;
    }

    @Override
    public void close() throws IOException {
        try {
            registry.unbind(serviceName);
        } catch (Exception ignored) {
        }

        try {
            UnicastRemoteObject.unexportObject(remoteServer, true);
        } catch (NoSuchObjectException ignored) {
        }
        try {
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (NoSuchObjectException ignored) {
        }
    }

    private static final class RmiServerAdapter extends UnicastRemoteObject implements RemoteMesosServer {
        private final MesosServerService serverService;
        private final Map<RemoteGameUpdateListener, GameUpdateSink> sinksByListener;

        private RmiServerAdapter(MesosServerService serverService) throws RemoteException {
            super();
            this.serverService = serverService;
            this.sinksByListener = new ConcurrentHashMap<>();
        }

        @Override
        public GameOperationResult getLobby() {
            return serverService.getLobby();
        }

        @Override
        public GameOperationResult createGame(CreateGameRequest request) {
            return serverService.createGame(request);
        }

        @Override
        public GameOperationResult joinGame(JoinGameRequest request) {
            return serverService.joinGame(request);
        }

        @Override
        public GameOperationResult getSnapshot(SnapshotRequest request) {
            return serverService.getSnapshot(request);
        }

        @Override
        public GameOperationResult chooseOffer(ChooseOfferRequest request) {
            return serverService.chooseOffer(request);
        }

        @Override
        public GameOperationResult chooseCard(ChooseCardRequest request) {
            return serverService.chooseCard(request);
        }

        @Override
        public GameOperationResult subscribe(SubscribeGameRequest request, RemoteGameUpdateListener listener) {
            if (listener == null) {
                return GameOperationResult.failure("Listener RMI non valido", null);
            }
            GameUpdateSink sink = sinksByListener.computeIfAbsent(listener, this::createSink);
            return serverService.subscribe(request, sink);
        }

        @Override
        public GameOperationResult unsubscribe(UnsubscribeGameRequest request, RemoteGameUpdateListener listener) {
            GameUpdateSink sink = listener == null ? null : sinksByListener.remove(listener);
            return serverService.unsubscribe(request, sink);
        }

        @Override
        public GameOperationResult heartbeat(HeartbeatRequest request) {
            return serverService.heartbeat(request);
        }

        private GameUpdateSink createSink(RemoteGameUpdateListener listener) {
            return update -> listener.onUpdate(update);
        }
    }
}
