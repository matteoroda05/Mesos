package it.polimi.ingsw.mesos.Launcher;

import it.polimi.ingsw.mesos.CLI.MesosCliClient;

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
     * Starts the CLI directly when a console exists, otherwise opens Swing.
     *
     * @param args CLI arguments
     * @throws Exception if the client startup fails
     */
    public static void main(String[] args) throws Exception {
        if (System.console() != null) {
            MesosCliClient.main(args);
            return;
        }

        SwingConsoleWindow.open("Mesos Client", () -> MesosCliClient.main(args));
    }
}
