package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

@FunctionalInterface
public interface GameUpdateListener {
    void onUpdate(GameViewUpdate update);
}
