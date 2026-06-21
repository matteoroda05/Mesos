package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.controller.enums.GameStatus;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.dto.game.GameSnapshot;
import it.polimi.ingsw.mesos.network.protocol.messages.GameOperationResult;
import it.polimi.ingsw.mesos.network.protocol.messages.GameViewUpdate;
import it.polimi.ingsw.mesos.network.protocol.OperationErrorCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesosUserMessageMapperTest {

    @Test
    void describeResult_nullResult_returnsGenericCompletion() {
        assertEquals("Operation completed.", MesosUserMessageMapper.describeResult(null, "ignored"));
    }

    @Test
    void describeResult_successResult_returnsSuccessFallback() {
        GameOperationResult success = GameOperationResult.success("server message", null);

        assertEquals("Joined the game.", MesosUserMessageMapper.describeResult(success, "Joined the game."));
    }

    @Test
    void describeResult_failureWithKnownErrorCode_returnsMappedErrorText() {
        GameOperationResult failure = GameOperationResult.failure("full", OperationErrorCode.GAME_FULL, null);

        assertEquals("The selected game is already full.",
                MesosUserMessageMapper.describeResult(failure, "ignored"));
    }

    @Test
    void describeResult_distinctErrorCodes_mapToDistinctMessages() {
        assertEquals("This nickname is already in use.", MesosUserMessageMapper.describeResult(
                GameOperationResult.failure("x", OperationErrorCode.PLAYER_NAME_TAKEN, null), "ignored"));
        assertEquals("That color has already been chosen by another player.", MesosUserMessageMapper.describeResult(
                GameOperationResult.failure("x", OperationErrorCode.COLOR_ALREADY_TAKEN, null), "ignored"));
    }

    @Test
    void describeResult_failureWithoutErrorCode_translatesKnownFreeformMessage() {
        GameOperationResult failure = GameOperationResult.failure("Wrong turn", OperationErrorCode.NONE, null);

        assertEquals("It is not your turn.", MesosUserMessageMapper.describeResult(failure, "ignored"));
    }

    @Test
    void describeResult_unknownFreeformMessage_returnsLowercasedMessage() {
        GameOperationResult failure = GameOperationResult.failure("Totally Unknown Thing", OperationErrorCode.NONE, null);

        assertEquals("totally unknown thing", MesosUserMessageMapper.describeResult(failure, "ignored"));
    }

    @Test
    void describeUpdate_nullUpdate_returnsGenericBoardMessage() {
        assertEquals("Board updated.", MesosUserMessageMapper.describeUpdate(null));
    }

    @Test
    void describeUpdate_knownMessage_isTranslated() {
        assertEquals("Card selected.", MesosUserMessageMapper.describeUpdate(update("Carta scelta")));
    }

    @Test
    void describeException_connectException_returnsServerUnreachableHint() {
        assertTrue(MesosUserMessageMapper.describeException(new ConnectException("refused"))
                .contains("Could not reach the MESOS server"));
    }

    @Test
    void describeException_rmiConnectException_returnsServerUnreachableHint() {
        assertTrue(MesosUserMessageMapper.describeException(new java.rmi.ConnectException("refused"))
                .contains("Could not reach the MESOS server"));
    }

    @Test
    void describeException_ioExceptionWithMessage_returnsNetworkError() {
        assertEquals("Network error: socket closed",
                MesosUserMessageMapper.describeException(new IOException("socket closed")));
    }

    @Test
    void describeException_ioExceptionWithoutMessage_returnsGenericNetworkError() {
        assertTrue(MesosUserMessageMapper.describeException(new IOException())
                .contains("network error interrupted"));
    }

    @Test
    void describeException_runtimeWrappingCause_unwrapsToRootMessage() {
        Throwable wrapped = new RuntimeException(new IllegalStateException("wrong game state"));

        assertEquals("This action is not available in the current phase.",
                MesosUserMessageMapper.describeException(wrapped));
    }

    @Test
    void describeException_genericWithoutMessage_returnsSimpleName() {
        assertEquals("IllegalStateException",
                MesosUserMessageMapper.describeException(new IllegalStateException()));
    }

    private static GameViewUpdate update(String message) {
        GameSnapshot snapshot = new GameSnapshot("game", 2, 1, false, false, GameStatus.LOGIN, 0,
                Era.FIRST, null, null, 0, 0, null, null, null, null, null, null);
        return new GameViewUpdate("game", message, snapshot);
    }
}
