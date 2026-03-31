package it.polimi.ingsw.mesos.Network.RMI;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import it.polimi.ingsw.mesos.Network.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.SnapshotRequest;

public class RmiGameClient implements GameClient {
    private final RemoteMesosServer remoteServer;

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
    public void close() {
    }
}
