package it.polimi.ingsw.mesos.Network.Socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import it.polimi.ingsw.mesos.Network.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameRequest;
import it.polimi.ingsw.mesos.Network.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.SnapshotRequest;

public class SocketGameClient implements GameClient {
    private final Socket socket;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;

    public SocketGameClient(String host, int port) throws IOException {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        this.socket = new Socket(host, port);
        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.output.flush();
        this.input = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public GameOperationResult createGame(CreateGameRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult joinGame(JoinGameRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult getSnapshot(SnapshotRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult chooseOffer(ChooseOfferRequest request) throws IOException {
        return exchange(request);
    }

    @Override
    public GameOperationResult chooseCard(ChooseCardRequest request) throws IOException {
        return exchange(request);
    }

    private synchronized GameOperationResult exchange(GameRequest request) throws IOException {
        try {
            output.writeObject(request);
            output.flush();
            output.reset();

            Object response = input.readObject();
            if (response instanceof GameOperationResult result) {
                return result;
            }
            throw new IOException("Unexpected response type: " + response);
        } catch (ClassNotFoundException exception) {
            throw new IOException("Could not deserialize server response", exception);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
