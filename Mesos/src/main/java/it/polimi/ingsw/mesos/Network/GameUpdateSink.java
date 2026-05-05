package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

import java.io.IOException;

@FunctionalInterface
public interface GameUpdateSink {
    void accept(GameViewUpdate update) throws IOException;
}
