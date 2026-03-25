package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Enums.Color;

public final class Player {
    private final String nickname;
    private final Color color;
    private final Tribe tribe;
    private int food;
    private int prestige_points;

    public Player(String name, Color color, int food) {
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

    public int loseFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food lost must be non-negative");
        }

        int out = amount > this.food ? amount - this.food : 0;
        this.food = amount >= this.food ? 0 : this.food - amount;
        return out;
    }

    public void changePrestigePoints(int points) {
        this.prestige_points += points;
    }
}
