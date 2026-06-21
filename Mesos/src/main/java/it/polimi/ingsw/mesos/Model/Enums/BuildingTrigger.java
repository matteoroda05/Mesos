package it.polimi.ingsw.mesos.Model.Enums;

/**
 * Enumerates the game moments and events that can activate a building effect.
 */
public enum BuildingTrigger{
    ON_END_TURN,  // after each player
    ON_AFTER_EVERYONE,  // after all players
    ON_END_GAME,
    ON_EVENT_SUSTENANCE,
    ON_EVENT_HUNT,
    ON_EVENT_SHAMANIC_RITUAL,
    ON_EVENT_CAVE_PAINTINGS,
    ON_ACQUIRE_CHARACTER
}
