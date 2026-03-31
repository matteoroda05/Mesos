package it.polimi.ingsw.mesos.Network;

public record SnapshotRequest(String gameName) implements GameRequest {
    public SnapshotRequest {
        JoinGameRequest.validateGameName(gameName);
    }
}
