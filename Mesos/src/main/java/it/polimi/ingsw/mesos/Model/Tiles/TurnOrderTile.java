package it.polimi.ingsw.mesos.Model.Tiles;

import java.util.ArrayList;
import java.util.List;


import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Player;

public class TurnOrderTile {

    private final List<TurnOrderSpace> spaces;

    public TurnOrderTile(List<TurnOrderSpace> spaces) {
        if (spaces == null) {
            throw new IllegalArgumentException("spaces cannot be null");
        }

        this.spaces = new ArrayList<>(spaces);

    }

    public void placeTotem(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        for (TurnOrderSpace space : spaces) {
            if (space.isFree()) {
                space.setOccupant(player.getColor());

                int bonus = space.getBonus();
                if (bonus > 0) {
                    player.gainFood(bonus);
                } else if (bonus < 0 && player.getFood() >= -bonus) {
                    player.loseFood(-bonus);
                } else if (bonus < 0) {
                    player.changePrestigePoints(-2);
                }

                return;
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

    public List<TurnOrderSpace> getSpaces() {
        return spaces;
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