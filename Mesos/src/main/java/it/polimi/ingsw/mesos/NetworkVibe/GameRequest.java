package it.polimi.ingsw.mesos.NetworkVibe;

import java.io.Serializable;

public sealed interface GameRequest extends Serializable permits CreateGameRequest, JoinGameRequest, SnapshotRequest,
        ChooseOfferRequest, ChooseCardRequest, SubscribeGameRequest, UnsubscribeGameRequest {
}
