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

    /**
     * Starts the JavaFX Mesos client application.
     *
     * @param args command-line arguments forwarded to JavaFX
     */
    public static void main(String[] args) {
        Application.launch(MesosClientApplication.class, args);
    }
}
