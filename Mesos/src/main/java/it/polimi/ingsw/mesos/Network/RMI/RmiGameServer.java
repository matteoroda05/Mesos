package it.polimi.ingsw.mesos.Network.RMI;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameUpdateSink;
import it.polimi.ingsw.mesos.Network.MesosServerService;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;

public class RmiGameServer implements AutoCloseable {
    private final Registry registry;
    private final RemoteMesosServer remoteServer;
    private final String serviceName;
    private final String advertisedHost;
    private final int port;

    /**
     * Creates an RMI server using the default advertised host.
     *
     * @param serverService service that handles game operations
     * @param port registry port
     * @param serviceName name used to bind the remote service
     * @throws IllegalArgumentException if required arguments are invalid
     * @throws RemoteException if the registry or remote adapter cannot be
     *                         created
     */
    public RmiGameServer(MesosServerService serverService, int port, String serviceName) throws RemoteException {
        this(serverService, NetworkDefaults.DEFAULT_RMI_HOST, port, serviceName);
    }

    /**
     * Creates an RMI registry, exports the server adapter, and binds it under
     * the requested service name.
     *
     * @param serverService service that handles game operations
     * @param host advertised RMI host
     * @param port registry port
     * @param serviceName name used to bind the remote service
     * @throws IllegalArgumentException if the service is null, host or service
     *                                  name is null/blank, or the port is
     *                                  outside 1 to 65535
     * @throws RemoteException if registry creation, adapter export, or binding
     *                         fails
     */
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

    /**
     * Returns the configured registry port.
     *
     * @return RMI registry port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the host advertised to RMI clients.
     *
     * @return advertised host from {@code java.rmi.server.hostname}
     */
    public String getAdvertisedHost() {
        return advertisedHost;
    }

    /**
     * Unbinds and unexports the remote service and registry.
     * Missing bindings or already-unexported objects are ignored.
     *
     * @throws IOException declared for {@link AutoCloseable} compatibility;
     *                     current cleanup paths swallow close-time failures
     */
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

        /**
         * Exports an RMI adapter over the game server service.
         *
         * @param serverService service that handles requests
         * @throws RemoteException if the adapter cannot be exported
         */
        private RmiServerAdapter(MesosServerService serverService) throws RemoteException {
            super();
            this.serverService = serverService;
            this.sinksByListener = new ConcurrentHashMap<>();
        }

        /**
         * Delegates lobby retrieval to the server service.
         *
         * @return operation result from the service
         */
        @Override
        public GameOperationResult getLobby() {
            return serverService.getLobby();
        }

        /**
         * Delegates nickname registration to the server service.
         *
         * @param request nickname request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult nickname(NicknameRequest request){
            return serverService.nickname(request);
        }

        /**
         * Delegates game creation to the server service.
         *
         * @param request creation request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult createGame(CreateGameRequest request) {
            return serverService.createGame(request);
        }

        /**
         * Delegates game joining to the server service.
         *
         * @param request join request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult joinGame(JoinGameRequest request) {
            return serverService.joinGame(request);
        }

        /**
         * Delegates snapshot retrieval to the server service.
         *
         * @param request snapshot request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult getSnapshot(SnapshotRequest request) {
            return serverService.getSnapshot(request);
        }

        /**
         * Delegates offer choice to the server service.
         *
         * @param request offer-choice request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult chooseOffer(ChooseOfferRequest request) {
            return serverService.chooseOffer(request);
        }

        /**
         * Delegates card choice to the server service.
         *
         * @param request card-choice request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult chooseCard(ChooseCardRequest request) {
            return serverService.chooseCard(request);
        }

        /**
         * Delegates color choice to the server service.
         *
         * @param request color-choice request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult chooseColor(ChooseColorRequest request) {
            return serverService.chooseColor(request);
        }

        /**
         * Delegates pass requests to the server service.
         *
         * @param request pass request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult pass(PassRequest request) {
            return serverService.pass(request);
        }

        /**
         * Subscribes an RMI listener by wrapping it in a local update sink.
         * Reusing the same listener reuses the same sink instance.
         *
         * @param request subscription request
         * @param listener remote update listener
         * @return failure result when listener is null, otherwise service result
         */
        @Override
        public GameOperationResult subscribe(SubscribeGameRequest request, RemoteGameUpdateListener listener) {
            if (listener == null) {
                return GameOperationResult.failure("Listener RMI non valido", null);
            }
            GameUpdateSink sink = sinksByListener.computeIfAbsent(listener, this::createSink);
            return serverService.subscribe(request, sink);
        }

        /**
         * Unsubscribes the sink associated with a remote listener.
         * A null listener produces a null sink and is delegated to the service.
         *
         * @param request unsubscription request
         * @param listener remote listener previously subscribed
         * @return operation result from the service
         */
        @Override
        public GameOperationResult unsubscribe(UnsubscribeGameRequest request, RemoteGameUpdateListener listener) {
            GameUpdateSink sink = listener == null ? null : sinksByListener.remove(listener);
            return serverService.unsubscribe(request, sink);
        }

        /**
         * Delegates heartbeat handling to the server service.
         *
         * @param request heartbeat request
         * @return operation result from the service
         */
        @Override
        public GameOperationResult heartbeat(HeartbeatRequest request) {
            return serverService.heartbeat(request);
        }

        @Override
        public GameOperationResult getRankings(RankingsRequest request) throws RemoteException {
            return serverService.getRankings(request);
        }

        /**
         * Creates a sink that forwards updates through a remote listener.
         *
         * @param listener remote listener to call
         * @return update sink backed by the listener
         */
        private GameUpdateSink createSink(RemoteGameUpdateListener listener) {
            return listener::onUpdate;
        }
    }
}
