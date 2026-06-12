package it.polimi.ingsw.mesos.Launcher;

import javafx.application.Application;

/**
 * Launches the Mesos client in a terminal or Swing console.
 */
public final class MesosClientLauncher {
    /**
     * Prevents instantiation.
     */
    private MesosClientLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(MesosClientApplication.class, args);
    }
}
