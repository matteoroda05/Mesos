package it.polimi.ingsw.mesos.dto.event;

import java.io.Serializable;

/**
 * Food and prestige change actually applied to a player after an outcome was
 * resolved against the model. Unlike {@link Result}, these values reflect the
 * real mutation (e.g. partial food payment converted into a prestige penalty),
 * so they can be shown verbatim to the user.
 *
 * @param foodDelta signed food change applied to the player
 * @param prestigeDelta signed prestige change applied to the player
 */
public record AppliedDelta(int foodDelta, int prestigeDelta) implements Serializable {
}
