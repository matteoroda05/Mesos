package it.polimi.ingsw.mesos.NetworkVibe;

public record ChooseOfferRequest(
        String gameName,
        String nickname,
        int offerTileIndex
) implements GameRequest {

    public ChooseOfferRequest {
        JoinGameRequest.validateGameName(gameName);
        JoinGameRequest.validateNickname(nickname);
        if (offerTileIndex < 0) {
            throw new IllegalArgumentException("offerTileIndex cannot be negative");
        }
    }
}
