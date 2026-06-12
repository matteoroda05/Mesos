package it.polimi.ingsw.mesos.Network;

@FunctionalInterface
public interface GameUpdateListener {
    void onUpdate(GameServerMessage update);
}
