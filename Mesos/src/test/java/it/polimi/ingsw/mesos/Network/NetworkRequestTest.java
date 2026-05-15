package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.HeartbeatRequest;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.LobbyRequest;
import it.polimi.ingsw.mesos.Network.Requests.NicknameRequest;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkRequestTest {
    @Test
    void requestRecordsValidateConstructorInputs() {
        assertThrows(IllegalArgumentException.class, () -> new CreateGameRequest(null, 2));
        assertThrows(IllegalArgumentException.class, () -> new CreateGameRequest(" ", 2));
        assertThrows(IllegalArgumentException.class, () -> new CreateGameRequest("game", 1));
        assertThrows(IllegalArgumentException.class, () -> new CreateGameRequest("game", 6));

        assertThrows(IllegalArgumentException.class, () -> new JoinGameRequest(null, "Alice"));
        assertThrows(IllegalArgumentException.class, () -> new JoinGameRequest(" ", "Alice"));
        assertThrows(IllegalArgumentException.class, () -> new JoinGameRequest("game", null));
        assertThrows(IllegalArgumentException.class, () -> new JoinGameRequest("game", " "));
        assertThrows(IllegalArgumentException.class, () -> new JoinGameRequest(null, " ", true));

        assertThrows(IllegalArgumentException.class, () -> new ChooseOfferRequest(null, "Alice", 0));
        assertThrows(IllegalArgumentException.class, () -> new ChooseOfferRequest("game", null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChooseOfferRequest("game", "Alice", -1));

        assertThrows(IllegalArgumentException.class, () -> new ChooseCardRequest(null, "Alice", 0, true));
        assertThrows(IllegalArgumentException.class, () -> new ChooseCardRequest("game", null, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new ChooseCardRequest("game", "Alice", -1, true));

        assertThrows(IllegalArgumentException.class, () -> new SnapshotRequest(null));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotRequest(" "));
        assertThrows(IllegalArgumentException.class, () -> new SubscribeGameRequest(null, "Alice"));
        assertThrows(IllegalArgumentException.class, () -> new SubscribeGameRequest("game", null));
        assertThrows(IllegalArgumentException.class, () -> new UnsubscribeGameRequest(null, "Alice"));
        assertThrows(IllegalArgumentException.class, () -> new UnsubscribeGameRequest("game", null));

        assertThrows(IllegalArgumentException.class, () -> new NicknameRequest(null));
        assertThrows(IllegalArgumentException.class, () -> new NicknameRequest(" "));
        assertThrows(IllegalArgumentException.class, () -> new HeartbeatRequest(null));
        assertThrows(IllegalArgumentException.class, () -> new HeartbeatRequest(" "));
        assertThrows(IllegalArgumentException.class, () -> new HeartbeatRequest(null, "Alice", true));
        assertThrows(IllegalArgumentException.class, () -> new HeartbeatRequest(" ", "Alice", true));
        assertThrows(IllegalArgumentException.class, () -> new HeartbeatRequest("game", null, true));
        assertThrows(IllegalArgumentException.class, () -> new HeartbeatRequest("game", "Alice", false));
    }

    @Test
    void requestsDispatchToMatchingServiceMethods() {
        RecordingService service = new RecordingService();

        assertEquals("nickname", new NicknameRequest("Alice").requestToService(service, null).message());
        assertEquals("lobby", new LobbyRequest().requestToService(service, null).message());
        assertEquals("create", new CreateGameRequest("game", 2).requestToService(service, null).message());
        assertEquals("join", new JoinGameRequest("game", "Alice").requestToService(service, null).message());
        assertEquals("rejoin", new JoinGameRequest(null, "Alice", true).requestToService(service, null).message());
        assertEquals("snapshot", new SnapshotRequest("game").requestToService(service, null).message());
        assertEquals("offer", new ChooseOfferRequest("game", "Alice", 0).requestToService(service, null).message());
        assertEquals("card", new ChooseCardRequest("game", "Alice", 0, true).requestToService(service, null).message());
        assertEquals("heartbeat", new HeartbeatRequest("Alice").requestToService(service, null).message());
        assertEquals("heartbeat", new HeartbeatRequest("game", "Alice", true).requestToService(service, null).message());

        GameOperationResult subscribe = new SubscribeGameRequest("game", "Alice").requestToService(service, null);
        assertFalse(subscribe.success());
        assertEquals("subscribe", subscribe.message());
    }

    private static final class RecordingService implements MesosServerService {
        @Override public GameOperationResult nickname(NicknameRequest request) { return GameOperationResult.successNickname("nickname", false); }
        @Override public GameOperationResult getLobby() { return GameOperationResult.successLobby("lobby", null); }
        @Override public GameOperationResult createGame(CreateGameRequest request) { return GameOperationResult.success("create", null); }
        @Override public GameOperationResult joinGame(JoinGameRequest request) { return GameOperationResult.success(request.rejoin() ? "rejoin" : "join", null); }
        @Override public GameOperationResult getSnapshot(SnapshotRequest request) { return GameOperationResult.success("snapshot", null); }
        @Override public GameOperationResult chooseOffer(ChooseOfferRequest request) { return GameOperationResult.success("offer", null); }
        @Override public GameOperationResult chooseCard(ChooseCardRequest request) { return GameOperationResult.success("card", null); }
        @Override public GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink) { return GameOperationResult.failure("subscribe", null); }
        @Override public GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink) { return GameOperationResult.failure("unsubscribe", null); }
        @Override public GameOperationResult heartbeat(HeartbeatRequest request) { return GameOperationResult.success("heartbeat", null); }
    }
}
