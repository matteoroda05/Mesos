package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;

public interface MesosServerService {
    GameOperationResult getLobby();

    GameOperationResult createGame(CreateGameRequest request);

    GameOperationResult joinGame(JoinGameRequest request);

    default GameOperationResult joinGame(JoinGameRequest request, String clientAddress) {
        return joinGame(request);
    }

    GameOperationResult getSnapshot(SnapshotRequest request);

    GameOperationResult chooseOffer(ChooseOfferRequest request);

    GameOperationResult chooseCard(ChooseCardRequest request);

    GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink);

    GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink);

    default GameOperationResult heartbeat(HeartbeatRequest request, String clientAddress) {
        return GameOperationResult.failure("Heartbeat non supportato", null);
    }

    default GameOperationResult getReconnectProposal(ReconnectProbeRequest request, String clientAddress) {
        return GameOperationResult.successReconnect("Nessuna partita da riconnettere", null);
    }

    default GameOperationResult reconnectGame(ReconnectGameRequest request, String clientAddress) {
        return GameOperationResult.failure(
                "Nessuna riconnessione disponibile",
                OperationErrorCode.RECONNECT_NOT_AVAILABLE,
                null
        );
    }

    default GameOperationResult declineReconnect(DeclineReconnectRequest request, String clientAddress) {
        return GameOperationResult.failure(
                "Nessuna riconnessione disponibile",
                OperationErrorCode.RECONNECT_NOT_AVAILABLE,
                null
        );
    }
}
