package it.polimi.ingsw.mesos.Model.Tiles;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Result;

/**
 * Turn-order track made of ordered spaces.
 *
 * @param spaces spaces stored from top to bottom
 */
public record TurnOrderTile(List<TurnOrderSpace> spaces) {

    /**
     * Returns the mutable list of turn-order spaces backing this tile.
     *
     * @return turn-order spaces
     */
    public List<TurnOrderSpace> getSpaces() {
        return spaces;
    }

    /**
     * Creates a turn-order tile by copying the provided space list.
     *
     * @param spaces spaces from top to bottom
     * @throws IllegalArgumentException if the space list is null
     */
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
     * @throws IllegalArgumentException if the player is null
     * @throws IllegalStateException if no free space exists
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
     * @throws IllegalArgumentException if the player color is null
     * @throws IllegalStateException if no free space exists
     */
    public Result placeTotem(Color player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        for (TurnOrderSpace space : spaces) {
            if (space.isFree()) {
                space.setOccupant(player);

                int bonus = space.getBonus();
                final int penalty = bonus < 0 ? 2 : 0;  // penalty for not having enough food to pay is always 2
                return new Result(bonus, 0, penalty);
            }
        }

        throw new IllegalStateException("No free spaces in TurnOrderTile");
    }


    /**
     * Reads the current turn order by scanning occupied spaces from top to
     * bottom.
     *
     * @return player colors in current turn order, or an empty list if no space
     *         is occupied
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
     *
     * @throws IllegalStateException if all spaces are already free
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
     * @return true when no free spaces remain; also true for an empty space
     *         list
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
