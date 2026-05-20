package it.polimi.ingsw.mesos.Network;

/**
 * Structured error codes returned by network operations.
 */
public enum OperationErrorCode {
    NONE,
    GAME_NAME_TAKEN,
    GAME_NOT_FOUND,
    GAME_FULL,
    PLAYER_NAME_TAKEN,
    PLAYER_NOT_IN_GAME,
    COLOR_ALREADY_TAKEN,
    COLOR_SELECTION_CLOSED,
    INVALID_LISTENER,
    NICKNAME_DISCONNECTED
}
