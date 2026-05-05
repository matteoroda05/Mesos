package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;

public interface MesosServerService {
    GameOperationResult getLobby();

    GameOperationResult createGame(CreateGameRequest request);

    GameOperationResult joinGame(JoinGameRequest request);

    GameOperationResult getSnapshot(SnapshotRequest request);

    GameOperationResult chooseOffer(ChooseOfferRequest request);

    GameOperationResult chooseCard(ChooseCardRequest request);

    GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink);

    GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink);
}
