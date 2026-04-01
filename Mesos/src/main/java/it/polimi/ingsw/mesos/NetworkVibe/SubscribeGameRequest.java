package it.polimi.ingsw.mesos.Network;

public record SubscribeGameRequest(
        String gameName,
        String nickname
) implements GameRequest {

    public SubscribeGameRequest {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }
}
