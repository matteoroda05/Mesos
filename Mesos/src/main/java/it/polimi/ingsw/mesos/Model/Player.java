package it.polimi.ingsw.mesos.Model;

import java.util.UUID;

import it.polimi.ingsw.mesos.Model.Enums.Color;

public final class Player {
    private UUID player_id;
    private final String nickname;
    private final Color color;
    private final Tribe tribe;
    private int food;
    private int prestige_points;

    public Player(String name, Color color, int food) {
        this.player_id = UUID.randomUUID();
        this.nickname = name;
        this.color = color;
        if (food < 0) {
            throw new IllegalArgumentException("Initial food must be non-negative");
        }
        this.food = food;
        this.prestige_points = 0;
        this.tribe = new Tribe();
    }

    public String getNickname() {
        return nickname;
    }

    public Color getColor() {
        return color;
    }

    public Tribe getTribe() {
        return tribe;
    }

    public int getFood() {
        return food;
    }
    
    public int getPrestigePoints() {
        return prestige_points;
    }

    public void gainFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food gained must be non-negative");
        }

        this.food += amount;
    }

    public void loseFood(int amount) {
        if (this.food < amount) {
            throw new IllegalArgumentException("Food lost must be non-negative");
        }

        this.food -= amount;
    }

    public void gainPrestigePoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points must be non-negative");
        }

        this.prestige_points += points;
    }

    public void losePrestigePoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points must be non-negative");
        }

        this.prestige_points -= points;
    }
}
