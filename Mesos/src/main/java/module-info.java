module it.polimi.ingsw.mesos {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens it.polimi.ingsw.mesos to javafx.fxml;
    exports it.polimi.ingsw.mesos;
}