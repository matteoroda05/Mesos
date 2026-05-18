package it.polimi.ingsw.mesos.Network.RMI;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameUpdateListener;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;

public class RmiGameClient implements GameClient {
    private final RemoteMesosServer remoteServer;
    private final ScheduledExecutorService heartbeatExecutor;
    private RemoteGameUpdateListener remoteListener;
    private ScheduledFuture<?> heartbeatTask;
    private SubscribeGameRequest activeSubscription;
    private volatile boolean sendHeartbeat;
    private volatile String heartbeatNickname;
    private volatile String heartbeatGameName;

    /**
     * Creates an RMI client by looking up the remote Mesos service in the RMI
     * registry.
     *
     * @param host registry host
     * @param registryPort registry port
     * @param serviceName bound service name
     * @throws IllegalArgumentException if host or service name is null/blank,
     *                                  or the port is outside 1 to 65535
     * @throws IOException if the remote service cannot be found or the lookup
     *                     fails
     */
    public RmiGameClient(String host, int registryPort, String serviceName) throws IOException {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }
        if (registryPort <= 0 || registryPort > 65_535) {
            throw new IllegalArgumentException("registryPort must be between 1 and 65535");
        }
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName cannot be null or blank");
        }

        try {
            Registry registry = LocateRegistry.getRegistry(host, registryPort);
            this.remoteServer = (RemoteMesosServer) registry.lookup(serviceName);
            this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "mesos-rmi-client-heartbeat");
                thread.setDaemon(true);
                return thread;
            });
        } catch (NotBoundException exception) {
            throw new IOException("RMI service '" + serviceName + "' is not bound", exception);
        }
    }

    /**
     * Retrieves the current lobby through RMI.
     *
     * @return operation result containing lobby data on success
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult getLobby() throws IOException {
        return remoteServer.getLobby();
    }

    /**
     * Registers or validates a nickname and starts lobby heartbeat when the
     * nickname is accepted without an immediate rejoin path.
     *
     * @param request nickname request
     * @return operation result from the server
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult nickname(NicknameRequest request) throws IOException {
        GameOperationResult result = remoteServer.nickname(request);
        if (result.success() && !result.rejoinAvailable()) {
            heartbeatNickname = request.nickname();
            heartbeatGameName = null;
            sendHeartbeat = true;
            startHeartbeat();
        }
        return result;
    }

    /**
     * Creates a game through RMI.
     *
     * @param request creation request
     * @return operation result from the server
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult createGame(CreateGameRequest request) throws IOException {
        return remoteServer.createGame(request);
    }

    /**
     * Joins a game and starts heartbeat for the joined game when successful.
     *
     * @param request join request
     * @return operation result from the server
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult joinGame(JoinGameRequest request) throws IOException {
        GameOperationResult result = remoteServer.joinGame(request);
        if (result.success()) {
            heartbeatNickname = request.nickname();
            heartbeatGameName = result.snapshot() == null ? null : result.snapshot().gameName();
            sendHeartbeat = true;
            startHeartbeat();
        }
        return result;
    }

    /**
     * Retrieves a game snapshot through RMI.
     *
     * @param request snapshot request
     * @return operation result from the server
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult getSnapshot(SnapshotRequest request) throws IOException {
        return remoteServer.getSnapshot(request);
    }

    /**
     * Sends an offer choice through RMI.
     *
     * @param request offer-choice request
     * @return operation result from the server
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult chooseOffer(ChooseOfferRequest request) throws IOException {
        return remoteServer.chooseOffer(request);
    }

    /**
     * Sends a card choice through RMI.
     *
     * @param request card-choice request
     * @return operation result from the server
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult chooseCard(ChooseCardRequest request) throws IOException {
        return remoteServer.chooseCard(request);
    }

    /**
     * Sends a pass request through RMI.
     *
     * @param request pass request
     * @return operation result from the server
     * @throws IOException if the remote call fails
     */
    @Override
    public GameOperationResult pass(PassRequest request) throws IOException {
        return remoteServer.pass(request);
    }

    /**
     * Subscribes to game updates by exporting a local RMI listener.
     * Any existing subscription is removed before the new listener is
     * registered. If server subscription fails, the exported listener is
     * unexported before returning.
     *
     * @param request subscription request
     * @param listener local listener delegate
     * @return operation result from the server
     * @throws IllegalArgumentException if the listener is null
     * @throws IOException if the remote call or listener export/unexport fails
     */
    @Override
    public GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateListener listener) throws IOException {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (activeSubscription != null && remoteListener != null) {
            unsubscribe(new UnsubscribeGameRequest(activeSubscription.gameName(), activeSubscription.nickname()));
        }

        RemoteGameUpdateListener exportedListener = new RemoteGameUpdateListenerAdapter(listener);
        try {
            GameOperationResult result = remoteServer.subscribe(request, exportedListener);
            if (result.success()) {
                activeSubscription = request;
                remoteListener = exportedListener;
                return result;
            }

            UnicastRemoteObject.unexportObject(exportedListener, true);
            return result;
        } catch (IOException exception) {
            try {
                UnicastRemoteObject.unexportObject(exportedListener, true);
            } catch (Exception ignored) {
            }
            throw exception;
        }
    }

    /**
     * Unsubscribes the currently exported remote listener.
     * When no listener is active, this returns a successful no-op result.
     *
     * @param request unsubscription request
     * @return operation result from the server, or a successful no-op result
     * @throws IOException if the remote unsubscription fails
     */
    @Override
    public GameOperationResult unsubscribe(UnsubscribeGameRequest request) throws IOException {
        if (remoteListener == null) {
            return GameOperationResult.success("Nessuna sottoscrizione attiva", null);
        }

        try {
            GameOperationResult result = remoteServer.unsubscribe(request, remoteListener);
            if (result.success()) {
                activeSubscription = null;
                RemoteGameUpdateListener listenerToClose = remoteListener;
                remoteListener = null;
                UnicastRemoteObject.unexportObject(listenerToClose, true);
            }
            return result;
        } catch (RemoteException exception) {
            throw new IOException("Failed to unsubscribe RMI listener", exception);
        }
    }

    /**
     * Closes the client, removing any active subscription, stopping heartbeat,
     * and shutting down the heartbeat executor.
     *
     * @throws IOException if active unsubscription fails before shutdown
     */
    @Override
    public void close() throws IOException {
        if (activeSubscription != null) {
            try {
                unsubscribe(new UnsubscribeGameRequest(activeSubscription.gameName(), activeSubscription.nickname()));
            } catch (IOException ignored) {
            }
        } else if (remoteListener != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteListener, true);
            } catch (Exception ignored) {
            }
            remoteListener = null;
        }
        sendHeartbeat = false;
        heartbeatNickname = null;
        heartbeatGameName = null;
        stopHeartbeat();
        heartbeatExecutor.shutdownNow();
    }

    /**
     * Starts periodic heartbeat after cancelling any previous heartbeat task.
     */
    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                this::sendHeartbeat,
                NetworkDefaults.HEARTBEAT_INTERVAL_MILLIS,
                NetworkDefaults.HEARTBEAT_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cancels the current heartbeat task, if one exists.
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
    }

    /**
     * Sends one heartbeat if heartbeat is enabled and a nickname is known.
     * Remote failures are ignored so that the scheduled task keeps running.
     */
    private void sendHeartbeat() {
        String nickname = heartbeatNickname;
        String gameName = heartbeatGameName;
        if (!sendHeartbeat || nickname == null) {
            return;
        }

        try {
            if (gameName == null) {
                remoteServer.heartbeat(new HeartbeatRequest(nickname));
            } else {
                remoteServer.heartbeat(new HeartbeatRequest(gameName, nickname, true));
            }
        } catch (RemoteException ignored) {
        }
    }

    private static final class RemoteGameUpdateListenerAdapter extends UnicastRemoteObject implements RemoteGameUpdateListener {
        private final GameUpdateListener delegate;

        /**
         * Exports an RMI callback that forwards updates to a local listener.
         *
         * @param delegate local listener receiving forwarded updates
         * @throws RemoteException if the callback cannot be exported
         */
        private RemoteGameUpdateListenerAdapter(GameUpdateListener delegate) throws RemoteException {
            super();
            this.delegate = delegate;
        }

        /**
         * Forwards a remote update to the local listener.
         *
         * @param update update payload
         */
        @Override
        public void onUpdate(GameViewUpdate update) {
            delegate.onUpdate(update);
        }
    }
}
