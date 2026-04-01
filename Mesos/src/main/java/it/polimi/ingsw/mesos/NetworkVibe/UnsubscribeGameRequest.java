package it.polimi.ingsw.mesos.NetworkVibe;

public record UnsubscribeGameRequest(
        String gameName,
        String nickname
) implements GameRequest {

    public UnsubscribeGameRequest {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }
}
