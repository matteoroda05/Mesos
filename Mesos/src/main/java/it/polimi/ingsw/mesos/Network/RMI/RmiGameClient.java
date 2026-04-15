package it.polimi.ingsw.mesos.Network.RMI;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameUpdateListener;
import it.polimi.ingsw.mesos.Network.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;

public class RmiGameClient implements GameClient {
    private final RemoteMesosServer remoteServer;
    private SubscribeGameRequest activeSubscription;
    private RemoteGameUpdateListener remoteListener;

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
        } catch (NotBoundException exception) {
            throw new IOException("RMI service '" + serviceName + "' is not bound", exception);
        }
    }

    @Override
    public GameOperationResult createGame(CreateGameRequest request) throws IOException {
        return remoteServer.createGame(request);
    }

    @Override
    public GameOperationResult joinGame(JoinGameRequest request) throws IOException {
        return remoteServer.joinGame(request);
    }

    @Override
    public GameOperationResult getSnapshot(SnapshotRequest request) throws IOException {
        return remoteServer.getSnapshot(request);
    }

    @Override
    public GameOperationResult chooseOffer(ChooseOfferRequest request) throws IOException {
        return remoteServer.chooseOffer(request);
    }

    @Override
    public GameOperationResult chooseCard(ChooseCardRequest request) throws IOException {
        return remoteServer.chooseCard(request);
    }

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
    }

    private static final class RemoteGameUpdateListenerAdapter extends UnicastRemoteObject implements RemoteGameUpdateListener {
        private final GameUpdateListener delegate;

        private RemoteGameUpdateListenerAdapter(GameUpdateListener delegate) throws RemoteException {
            super();
            this.delegate = delegate;
        }

        @Override
        public void onUpdate(GameViewUpdate update) {
            delegate.onUpdate(update);
        }
    }
}
