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

    /**
     * Adds food to the player's supply.
     *
     * @param amount non-negative amount of food to add
     */
    public void gainFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food gained must be non-negative");
        }
        System.out.println("adding " + amount + " food to player " + this.color);

        this.food += amount;
    }

    /**
     * Removes food from the player and returns the unpaid remainder when the
     * requested amount is greater than the current supply.
     *
     * @param amount non-negative amount of food to remove
     * @return the amount that could not be paid because the player lacked food
     */
    public int loseFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food lost must be non-negative");
        }
        System.out.println("removing " + amount + " food to player " + this.color);

        int out = amount > this.food ? amount - this.food : 0;
        this.food = amount >= this.food ? 0 : this.food - amount;
        return out;
    }

    /**
     * Applies a prestige-point delta to the player score.
     *
     * @param points points to add, or a negative value to subtract
     */
    public void changePrestigePoints(int points) {
        System.out.println("adding " + points + " prestige to player " + this.color);
        this.prestige_points += points;
    }
}
