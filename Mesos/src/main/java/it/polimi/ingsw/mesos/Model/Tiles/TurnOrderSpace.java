package it.polimi.ingsw.mesos.Model.Tiles;

import it.polimi.ingsw.mesos.Model.Enums.Color;

public class TurnOrderSpace {

    private final int foodBonus;
    private Color occupant;   // null se lo spazio è libero

    public TurnOrderSpace(int foodBonus) {
        if (foodBonus < -1 || foodBonus > 3) {
            throw new IllegalArgumentException("foodBonus must be between 0 and 3");
        }
        this.foodBonus = foodBonus;
        this.occupant = null;
    }

    public int getBonus() {
        return foodBonus;
    }

    public Color getOccupant() {
        return occupant;
    }

    public void setOccupant(Color playerColor) {
        this.occupant = playerColor;
    }

    public boolean isFree() {
        return occupant == null;
    }

    public void clear() {
        occupant = null;
    }
}