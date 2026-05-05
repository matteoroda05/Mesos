package it.polimi.ingsw.mesos.Launcher;

import it.polimi.ingsw.mesos.Network.MesosServerLauncher;

public final class MesosServerLauncherApp {
    private MesosServerLauncherApp() {
    }

    public static void main(String[] args) throws Exception {
        if (System.console() != null) {
            MesosServerLauncher.main(args);
            return;
        }

        SwingConsoleWindow.open("Mesos Server", () -> MesosServerLauncher.main(args));
    }
}
