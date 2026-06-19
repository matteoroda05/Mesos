package it.polimi.ingsw.mesos.Network;

import java.io.IOException;

@FunctionalInterface
public interface GameUpdateSink {
    /**
     * Delivers a server-pushed game update to this sink.
     *
     * @param update message to deliver
     * @throws IOException if the recipient cannot receive the update
     */
    void accept(GameServerMessage update) throws IOException;
}
