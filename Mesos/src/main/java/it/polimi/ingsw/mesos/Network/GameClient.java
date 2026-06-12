package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;

import java.io.Closeable;
import java.io.IOException;

public interface GameClient extends Closeable {
    GameOperationResult getLobby() throws IOException;

    GameOperationResult createGame(CreateGameRequest request) throws IOException;

    GameOperationResult joinGame(JoinGameRequest request) throws IOException;

    GameOperationResult getSnapshot(SnapshotRequest request) throws IOException;

    GameOperationResult chooseOffer(ChooseOfferRequest request) throws IOException;

    GameOperationResult chooseCard(ChooseCardRequest request) throws IOException;

    GameOperationResult chooseColor(ChooseColorRequest request) throws IOException;

    GameOperationResult pass(PassRequest request) throws IOException;

    GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateListener listener) throws IOException;

    GameOperationResult unsubscribe(UnsubscribeGameRequest request) throws IOException;

    GameOperationResult nickname(NicknameRequest request) throws IOException;

    @Override
    void close() throws IOException;
}
