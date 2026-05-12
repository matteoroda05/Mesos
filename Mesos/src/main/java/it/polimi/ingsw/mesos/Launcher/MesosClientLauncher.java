package it.polimi.ingsw.mesos.Launcher;

import javafx.application.Application;

public final class MesosClientLauncher {
    private MesosClientLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(MesosClientApplication.class, args);
    }
}
