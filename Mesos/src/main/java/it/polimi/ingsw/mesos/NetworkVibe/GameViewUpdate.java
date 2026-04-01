package it.polimi.ingsw.mesos.Network;

public record GameViewUpdate(
        String gameName,
        String message,
        GameSnapshot snapshot
) implements GameServerMessage {

    public GameViewUpdate {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (message == null || message.isBlank()) {
            message = "Aggiornamento tavolo";
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }
    }
}
