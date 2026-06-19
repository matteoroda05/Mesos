package it.polimi.ingsw.mesos.Network;

@FunctionalInterface
public interface GameUpdateListener {
    /**
     * Receives a server-pushed game update.
     *
     * @param update message delivered by the server
     */
    void onUpdate(GameServerMessage update);
}
