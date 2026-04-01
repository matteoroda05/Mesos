package it.polimi.ingsw.mesos.NetworkVibe;

public record JoinGameRequest(
        String gameName,
        String nickname
) implements GameRequest {

    public JoinGameRequest {
        validateGameName(gameName);
        validateNickname(nickname);
    }

    static void validateGameName(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
    }

    static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }
}
