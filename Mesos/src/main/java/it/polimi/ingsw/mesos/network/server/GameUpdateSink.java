package it.polimi.ingsw.mesos.network.server;

import java.io.IOException;

import it.polimi.ingsw.mesos.network.protocol.messages.GameServerMessage;

/**
 * Delivers a server-pushed game message to a subscribed transport endpoint.
 */
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
