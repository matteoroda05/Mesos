package it.polimi.ingsw.mesos.Network;

public interface MesosServerService {
    GameOperationResult createGame(CreateGameRequest request);

    GameOperationResult joinGame(JoinGameRequest request);

    GameOperationResult getSnapshot(SnapshotRequest request);

    GameOperationResult chooseOffer(ChooseOfferRequest request);

    GameOperationResult chooseCard(ChooseCardRequest request);

    GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink);

    GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink);
}
