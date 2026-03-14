package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurnOrderTile {

    private final List<TurnOrderSpace> spaces;

    public TurnOrderTile(int numPlayers) {
        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("numPlayers must be between 2 and 5");
        }

        this.spaces = new ArrayList<>();

        spaces.add(new TurnOrderSpace(3));

        spaces.add(new TurnOrderSpace(1));

        for (int i = 2; i < numPlayers; i++) {
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
                }

                boolean isLastSpace = (i == spaces.size() - 1);
                if (isLastSpace) {
                    if (player.getFood() > 0) {
                        player.loseFood(1);
                    } else {
                        player.losePrestigePoints(1);
                    }
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