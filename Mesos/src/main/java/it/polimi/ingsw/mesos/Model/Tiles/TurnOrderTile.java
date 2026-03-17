package it.polimi.ingsw.mesos.Model.Tiles;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Player;

public class TurnOrderTile {

    private final List<TurnOrderSpace> spaces;

    public TurnOrderTile(int num_players) {
        if (num_players < Globals.MIN_NUM_PLAYERS || num_players > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                "numPlayers must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }

        this.spaces = new ArrayList<>();

        int[] bonuses = switch (num_players) {
            case 2 -> new int[] {1, -1};
            case 3 -> new int[] {2, 0, -1};
            case 4 -> new int[] {2, 1, 0, -1};
            case 5 -> new int[] {3, 1, 0, 0, -1};
            default -> throw new IllegalStateException("Unexpected number of players: " + num_players);
        };

        for (int bonus : bonuses) {
            spaces.add(new TurnOrderSpace(bonus));
        }
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