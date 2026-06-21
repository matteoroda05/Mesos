package it.polimi.ingsw.mesos.Network;

/**
 * Receives server-pushed game messages on behalf of a client.
 */
@FunctionalInterface
public interface GameUpdateListener {
    /**
     * Receives a server-pushed game update.
     *
     * @param update message delivered by the server
     */
    void onUpdate(GameServerMessage update);
}
