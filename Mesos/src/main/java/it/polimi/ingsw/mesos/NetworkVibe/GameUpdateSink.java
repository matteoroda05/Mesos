package it.polimi.ingsw.mesos.Network;

import java.io.IOException;

@FunctionalInterface
public interface GameUpdateSink {
    void accept(GameViewUpdate update) throws IOException;
}
