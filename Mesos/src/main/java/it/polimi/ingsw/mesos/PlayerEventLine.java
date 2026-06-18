package it.polimi.ingsw.mesos;

import it.polimi.ingsw.mesos.Model.Enums.Color;

import java.io.Serializable;

import static it.polimi.ingsw.mesos.EventMessageFormatter.joinChanges;
import static it.polimi.ingsw.mesos.EventMessageFormatter.playerLabel;

/**
 * A single per-player line within an event summary, describing how much food
 * and prestige the player actually gained or lost.
 *
 * @param nickname nickname of the player affected by the event
 * @param color player affected by the event
 * @param foodDelta signed food change applied to the player
 * @param prestigeDelta signed prestige change applied to the player
 */
public record PlayerEventLine(String nickname, Color color, int foodDelta, int prestigeDelta) implements Serializable {
    public String describeLine() {
        String change = joinChanges(this.foodDelta, this.prestigeDelta);
        return "Player " + playerLabel(this.nickname, this.color) + " " + change;
    }
}
