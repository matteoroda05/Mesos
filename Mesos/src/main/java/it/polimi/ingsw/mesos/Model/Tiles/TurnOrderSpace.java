package it.polimi.ingsw.mesos.Model.Tiles;

import it.polimi.ingsw.mesos.Model.Enums.Color;

/**
 * Models one position on the turn-order tile, including its food bonus or
 * penalty and current occupant.
 */
public class TurnOrderSpace {

    private final int foodBonus;
    private Color occupant;   // null se lo spazio è libero

    /**
     * Creates a turn-order space with the configured food bonus or penalty.
     *
     * @param foodBonus food bonus, or {@code -1} for the penalty space
     * @throws IllegalArgumentException if the bonus is outside the range -1 to 3
     */
    public TurnOrderSpace(int foodBonus) {
        if (foodBonus < -1 || foodBonus > 3) {
            throw new IllegalArgumentException("foodBonus must be between 0 and 3");
        }
        this.foodBonus = foodBonus;
        this.occupant = null;
    }

    /**
     * Returns the food bonus associated with this space.
     *
     * @return food bonus, or {@code -1} for the penalty space
     */
    public int getBonus() {
        return foodBonus;
    }

    /**
     * Returns the occupying player color, if any.
     *
     * @return occupant color, or null when free
     */
    public Color getOccupant() {
        return occupant;
    }

    /**
     * Sets the occupying player color.
     * Passing null clears the space.
     *
     * @param playerColor color to assign, or null to mark the space free
     */
    public void setOccupant(Color playerColor) {
        this.occupant = playerColor;
    }

    /**
     * Checks whether no player occupies this space.
     *
     * @return true when the occupant is null
     */
    public boolean isFree() {
        return occupant == null;
    }

    /**
     * Clears the occupying player, if any.
     */
    public void clear() {
        occupant = null;
    }
}
