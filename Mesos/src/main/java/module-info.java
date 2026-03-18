module it.polimi.ingsw.mesos {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    opens it.polimi.ingsw.mesos.Model        to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Cards  to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Enums  to com.fasterxml.jackson.databind;

    opens it.polimi.ingsw.mesos to javafx.fxml;
    exports it.polimi.ingsw.mesos;
    opens it.polimi.ingsw.mesos.Model.Cards.Events to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Cards.Characters to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Cards.Buildings to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Tiles to com.fasterxml.jackson.databind;
}