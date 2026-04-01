package it.polimi.ingsw.mesos.NetworkVibe;

import java.io.IOException;

@FunctionalInterface
public interface GameUpdateSink {
    void accept(GameViewUpdate update) throws IOException;
}
