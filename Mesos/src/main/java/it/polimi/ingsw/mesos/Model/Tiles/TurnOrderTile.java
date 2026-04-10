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


    public List<Color> currentOrderTopToBottom() {
        List<Color> order = new ArrayList<>();

        for (TurnOrderSpace space : spaces) {
            if (space.getOccupant() != null) {
                order.add(space.getOccupant());
            }
        }

        return order;
    }

    public void clearFirst() {
        for (TurnOrderSpace space : spaces) {
            if (!space.isFree()) {
                space.clear();
                return;
            }
        }

        throw new IllegalStateException("No occupied spaces in TurnOrderTile");
    }

    public void clear() {
        for (TurnOrderSpace space : spaces) {
            space.clear();
        }
    }

    public boolean isFull() {
        for (TurnOrderSpace space : spaces) {
            if (space.isFree()) {
                return false;
            }
        }
        return true;
    }
}
