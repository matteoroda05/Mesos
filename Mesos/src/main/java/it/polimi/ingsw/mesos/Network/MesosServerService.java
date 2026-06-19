package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Socket.SocketGameServer;

public interface MesosServerService {
    GameOperationResult getLobby();

    GameOperationResult createGame(CreateGameRequest request);

    GameOperationResult joinGame(JoinGameRequest request);

    GameOperationResult getSnapshot(SnapshotRequest request);

    GameOperationResult chooseOffer(ChooseOfferRequest request);

    GameOperationResult chooseCard(ChooseCardRequest request);

    GameOperationResult chooseColor(ChooseColorRequest request);

    GameOperationResult pass(PassRequest request);

    GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink);

    GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink);

    GameOperationResult heartbeat(HeartbeatRequest request);

    GameOperationResult nickname(NicknameRequest request);

    GameOperationResult getRankings(RankingsRequest request);
}