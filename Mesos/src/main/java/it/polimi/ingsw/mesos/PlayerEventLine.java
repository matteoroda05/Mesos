package it.polimi.ingsw.mesos;

import it.polimi.ingsw.mesos.Model.Enums.Color;

import java.io.Serializable;

import static it.polimi.ingsw.mesos.EventMessageFormatter.joinChanges;

/**
 * A single per-player line within an event summary, describing how much food
 * and prestige the player actually gained or lost.
 *
 * @param color player affected by the event
 * @param foodDelta signed food change applied to the player
 * @param prestigeDelta signed prestige change applied to the player
 */
public record PlayerEventLine(Color color, int foodDelta, int prestigeDelta) implements Serializable {
    public String describeLine() {
        String change = joinChanges(this.foodDelta, this.prestigeDelta);
        return "Player " + this.color + " " + change;
    }
}
