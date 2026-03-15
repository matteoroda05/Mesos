package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;

public class TurnOrderTile {

    private final List<TurnOrderSpace> spaces;

    public TurnOrderTile(int num_players) {
        if (num_players < Globals.MIN_NUM_PLAYERS || num_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "numPlayers must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }

        this.spaces = new ArrayList<>();

        spaces.add(new TurnOrderSpace(3));

        spaces.add(new TurnOrderSpace(1));

        for (int i = 2; i < num_players; i++) {
            spaces.add(new TurnOrderSpace(0));
        }
    }

    public void placeTotem(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        for (int i = 0; i < spaces.size(); i++) {
            TurnOrderSpace space = spaces.get(i);

            if (space.isFree()) {
                space.setOccupant(player.getColor());

                int bonus = space.getBonus();
                if (bonus > 0) {
                    player.gainFood(bonus);
                } else if (bonus < 0 && player.getFood() >= bonus) {
                    player.loseFood(-bonus);
                } else if (bonus < 0 && player.getFood() < bonus) {
                    player.losePrestigePoints(2);
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