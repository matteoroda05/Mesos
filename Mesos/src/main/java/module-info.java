module it.polimi.ingsw.mesos {
    requires java.rmi;
    requires javafx.controls;
    requires javafx.graphics;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires java.sql;

    // --- Model (domain) ---
    exports it.polimi.ingsw.mesos.model;
    exports it.polimi.ingsw.mesos.model.enums;
    exports it.polimi.ingsw.mesos.model.tiles;
    exports it.polimi.ingsw.mesos.model.cards;
    exports it.polimi.ingsw.mesos.model.cards.characters;
    exports it.polimi.ingsw.mesos.model.cards.events;
    exports it.polimi.ingsw.mesos.model.cards.buildings;

    opens it.polimi.ingsw.mesos.model                 to com.fasterxml.jackson.databind, org.junit.platform.commons;
    opens it.polimi.ingsw.mesos.model.cards           to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.model.enums           to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.model.cards.events    to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.model.cards.characters to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.model.cards.buildings to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.mesos.model.tiles           to com.fasterxml.jackson.databind;

    // --- Controller ---
    exports it.polimi.ingsw.mesos.controller;
    exports it.polimi.ingsw.mesos.controller.enums;
    exports it.polimi.ingsw.mesos.controller.states;
    opens it.polimi.ingsw.mesos.controller        to org.junit.platform.commons;
    opens it.polimi.ingsw.mesos.controller.states to org.junit.platform.commons;

    // --- Shared data-transfer contracts ---
    exports it.polimi.ingsw.mesos.dto.game;
    exports it.polimi.ingsw.mesos.dto.lobby;
    exports it.polimi.ingsw.mesos.dto.event;

    // --- Cross-layer common ---
    exports it.polimi.ingsw.mesos.common;

    // --- Views ---
    exports it.polimi.ingsw.mesos.cli;
    exports it.polimi.ingsw.mesos.gui;
    opens it.polimi.ingsw.mesos.gui to javafx.graphics;

    // --- Launcher (application entry points) ---
    exports it.polimi.ingsw.mesos.launcher;
    opens it.polimi.ingsw.mesos.launcher to javafx.graphics, org.junit.platform.commons;

    // --- Network: protocol contracts ---
    exports it.polimi.ingsw.mesos.network.protocol;
    exports it.polimi.ingsw.mesos.network.protocol.requests;
    exports it.polimi.ingsw.mesos.network.protocol.messages;
    exports it.polimi.ingsw.mesos.network.protocol.rmi;

    // --- Network: client side ---
    exports it.polimi.ingsw.mesos.network.client;
    exports it.polimi.ingsw.mesos.network.client.rmi;
    exports it.polimi.ingsw.mesos.network.client.socket;

    // --- Network: server side ---
    exports it.polimi.ingsw.mesos.network.server;
    exports it.polimi.ingsw.mesos.network.server.rmi;
    exports it.polimi.ingsw.mesos.network.server.socket;

    // --- Network: configuration ---
    exports it.polimi.ingsw.mesos.network.config;
}
