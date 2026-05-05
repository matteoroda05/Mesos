package it.polimi.ingsw.mesos.Network;

import java.io.Serializable;

public record ReconnectProposal(
        String gameName,
        String nickname
) implements Serializable {

    public ReconnectProposal {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname cannot be null or blank");
        }
    }
}
