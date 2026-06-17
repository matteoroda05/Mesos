package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Network.ConnectionType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesosGuiServiceTest {

    @Test
    void connect_nullConnectionType_throws() {
        try (MesosGuiService service = new MesosGuiService()) {
            assertThrows(NullPointerException.class, () -> service.connect(null, "127.0.0.1"));
        }
    }

    @Test
    void connect_blankHost_throws() {
        try (MesosGuiService service = new MesosGuiService()) {
            assertThrows(IllegalArgumentException.class, () -> service.connect(ConnectionType.SOCKET, "  "));
        }
    }

    @Test
    void hasJoinedGame_beforeJoining_isFalse() {
        try (MesosGuiService service = new MesosGuiService()) {
            assertFalse(service.hasJoinedGame());
            assertNull(service.currentGameName());
            assertNull(service.currentNickname());
        }
    }

    @Test
    void operationBeforeConnecting_failsWithIllegalState() {
        try (MesosGuiService service = new MesosGuiService()) {
            CompletionException error = assertThrows(CompletionException.class, () -> service.fetchLobby().join());
            assertInstanceOf(IllegalStateException.class, rootCause(error));
        }
    }

    @Test
    void gameCommandWithoutActiveSession_failsWithIllegalState() {
        try (MesosGuiService service = new MesosGuiService()) {
            CompletionException error = assertThrows(CompletionException.class, () -> service.chooseOffer(0).join());
            assertInstanceOf(IllegalStateException.class, rootCause(error));
        }
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
