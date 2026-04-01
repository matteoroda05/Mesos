package it.polimi.ingsw.mesos.NetworkVibe;

public record SnapshotRequest(String gameName) implements GameRequest {
    public SnapshotRequest {
        JoinGameRequest.validateGameName(gameName);
    }
}
