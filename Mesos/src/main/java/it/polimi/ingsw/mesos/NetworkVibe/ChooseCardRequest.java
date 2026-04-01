package it.polimi.ingsw.mesos.Network;

public record ChooseCardRequest(
        String gameName,
        String nickname,
        int cardIndex,
        boolean topRow
) implements GameRequest {

    public ChooseCardRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
        if (cardIndex < 0) {
            throw new IllegalArgumentException("cardIndex cannot be negative");
        }
    }
}
