package it.polimi.ingsw.mesos.Model.Tiles;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Result;

public record TurnOrderTile(List<TurnOrderSpace> spaces) {

    public TurnOrderTile(List<TurnOrderSpace> spaces) {
        if (spaces == null) {
            throw new IllegalArgumentException("spaces cannot be null");
        }

        this.spaces = new ArrayList<>(spaces);
    }

    /**
     * Places a player's totem in the first free turn-order space during game
     * setup.
     *
     * @param player player whose totem is placed
     */
    public void setupTotem(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        for (TurnOrderSpace space : spaces) {
            if (space.isFree()) {
                space.setOccupant(player.getColor());
                return;
            }
        }

        throw new IllegalStateException("No free spaces in TurnOrderTile");
    }

    /**
     * Places a returning player's totem in the first free space and returns the
     * food-penalty effect associated with that space.
     *
     * @param player color of the returning player
     * @return result containing the turn-order space bonus or penalty
     */
    public Result placeTotem(Color player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        for (TurnOrderSpace space : spaces) {
            if (space.isFree()) {
                space.setOccupant(player);

                int bonus = space.getBonus();
                if (bonus > 0) {
                    return new Result(bonus, 0, 0);
                } else if (bonus < 0) {
                    return new Result(bonus, 0, 2);
                }
            }
        }

        throw new IllegalStateException("No free spaces in TurnOrderTile");
    }


    /**
     * Reads the current turn order by scanning occupied spaces from top to
     * bottom.
     *
     * @return player colors in current turn order
     */
    public List<Color> currentOrderTopToBottom() {
        List<Color> order = new ArrayList<>();

        for (TurnOrderSpace space : spaces) {
            if (space.getOccupant() != null) {
                order.add(space.getOccupant());
            }
        }

        return order;
    }

    /**
     * Clears the first occupied turn-order space, preserving the order of the
     * remaining occupants.
     */
    public void clearFirst() {
        for (TurnOrderSpace space : spaces) {
            if (!space.isFree()) {
                space.clear();
                return;
            }
        }

        throw new IllegalStateException("No occupied spaces in TurnOrderTile");
    }

    /**
     * Clears every turn-order space.
     */
    public void clear() {
        for (TurnOrderSpace space : spaces) {
            space.clear();
        }
    }

    /**
     * Checks whether every turn-order space is occupied.
     *
     * @return true when no free spaces remain
     */
    public boolean isFull() {
        for (TurnOrderSpace space : spaces) {
            if (space.isFree()) {
                return false;
            }
        }
        return true;
    }
}
