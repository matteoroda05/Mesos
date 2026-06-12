package it.polimi.ingsw.mesos.Launcher;

import it.polimi.ingsw.mesos.Network.MesosServerLauncher;

/**
 * Launches the Mesos server in a terminal or Swing console.
 */
public final class MesosServerLauncherApp {
    /**
     * Prevents instantiation.
     */
    private MesosServerLauncherApp() {
    }

    /**
     * Starts the server directly when a console exists, otherwise opens Swing.
     *
     * @param args server launch arguments
     * @throws Exception if the server startup fails
     */
    public static void main(String[] args) throws Exception {
        MesosServerLauncher.main(args);
    }
}
