module it.polimi.ingsw.mesos {
    requires java.desktop;
    requires java.rmi;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires java.sql;

    opens it.polimi.ingsw.mesos.Model        to com.fasterxml.jackson.databind, org.junit.platform.commons;
    opens it.polimi.ingsw.mesos.Model.Cards  to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Enums  to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Controller   to org.junit.platform.commons;

    exports it.polimi.ingsw.mesos;
    exports it.polimi.ingsw.mesos.Controller;
    exports it.polimi.ingsw.mesos.Controller.Enums;

    exports it.polimi.ingsw.mesos.Model;
    exports it.polimi.ingsw.mesos.Model.Enums;
    exports it.polimi.ingsw.mesos.Model.Tiles;
    exports it.polimi.ingsw.mesos.Model.Cards;

    exports it.polimi.ingsw.mesos.Model.Cards.Characters;
    exports it.polimi.ingsw.mesos.Model.Cards.Events;
    exports it.polimi.ingsw.mesos.Model.Cards.Buildings;

    opens it.polimi.ingsw.mesos.Model.Cards.Events to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Cards.Characters to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Cards.Buildings to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.Model.Tiles to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.mesos.Controller.States;
    exports it.polimi.ingsw.mesos.CLI;
    exports it.polimi.ingsw.mesos.Network;
    exports it.polimi.ingsw.mesos.Network.RMI;
    exports it.polimi.ingsw.mesos.Network.Socket;
    opens it.polimi.ingsw.mesos.Controller.States to org.junit.platform.commons;
    exports it.polimi.ingsw.mesos.Network.Requests;
    exports it.polimi.ingsw.mesos.Network.Messages;
    exports it.polimi.ingsw.mesos.Launcher;
}
