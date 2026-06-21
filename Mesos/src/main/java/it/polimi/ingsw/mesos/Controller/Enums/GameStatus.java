package it.polimi.ingsw.mesos.Controller.Enums;

/**
 * Enumerates the client-visible phases that a game can occupy from setup
 * through its final screen.
 */
public enum GameStatus{
    LOADING,
    LOGIN,
    SETUP,
    ROUND_OFFER,
    ROUND_ACTIONS,
    ROUND_EVENTS,
    ROUND_END,
    FINAL_EVENTS,
    END_GAME,
    END_SCREEN
}
