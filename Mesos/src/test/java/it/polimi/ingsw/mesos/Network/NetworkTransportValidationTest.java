package it.polimi.ingsw.mesos.network;

import it.polimi.ingsw.mesos.dto.game.*;
import it.polimi.ingsw.mesos.dto.lobby.*;
import it.polimi.ingsw.mesos.dto.event.*;
import it.polimi.ingsw.mesos.network.config.*;
import it.polimi.ingsw.mesos.network.protocol.*;
import it.polimi.ingsw.mesos.network.protocol.messages.*;
import it.polimi.ingsw.mesos.network.protocol.requests.*;
import it.polimi.ingsw.mesos.network.protocol.rmi.*;
import it.polimi.ingsw.mesos.network.client.*;
import it.polimi.ingsw.mesos.network.server.*;

import it.polimi.ingsw.mesos.network.client.rmi.RmiGameClient;
import it.polimi.ingsw.mesos.network.server.rmi.RmiGameServer;
import it.polimi.ingsw.mesos.network.client.socket.SocketGameClient;
import it.polimi.ingsw.mesos.network.server.socket.SocketGameServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkTransportValidationTest {
    @Test
    void socketServerRejectsInvalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SocketGameServer(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new SocketGameServer(new MesosServerCore(), -1));
        assertThrows(IllegalArgumentException.class, () -> new SocketGameServer(new MesosServerCore(), 65_536));
    }

    @Test
    void socketClientRejectsInvalidArgumentsBeforeConnecting() {
        assertThrows(IllegalArgumentException.class, () -> new SocketGameClient(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SocketGameClient(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new SocketGameClient("127.0.0.1", 0));
        assertThrows(IllegalArgumentException.class, () -> new SocketGameClient("127.0.0.1", 65_536));
    }

    @Test
    void rmiServerRejectsInvalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new RmiGameServer(null, "127.0.0.1", 1, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameServer(new MesosServerCore(), null, 1, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameServer(new MesosServerCore(), " ", 1, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameServer(new MesosServerCore(), "127.0.0.1", 0, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameServer(new MesosServerCore(), "127.0.0.1", 65_536, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameServer(new MesosServerCore(), "127.0.0.1", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameServer(new MesosServerCore(), "127.0.0.1", 1, " "));
    }

    @Test
    void rmiClientRejectsInvalidArgumentsBeforeLookup() {
        assertThrows(IllegalArgumentException.class, () -> new RmiGameClient(null, 1, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameClient(" ", 1, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameClient("127.0.0.1", 0, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameClient("127.0.0.1", 65_536, "service"));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameClient("127.0.0.1", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new RmiGameClient("127.0.0.1", 1, " "));
    }
}
