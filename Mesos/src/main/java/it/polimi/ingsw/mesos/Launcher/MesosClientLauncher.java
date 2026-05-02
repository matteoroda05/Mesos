package it.polimi.ingsw.mesos.Launcher;

import it.polimi.ingsw.mesos.CLI.MesosCliClient;

public final class MesosClientLauncher {
    private MesosClientLauncher() {
    }

    public static void main(String[] args) throws Exception {
        if (System.console() != null) {
            MesosCliClient.main(args);
            return;
        }

        SwingConsoleWindow.open("Mesos Client", () -> MesosCliClient.main(args));
    }
}
