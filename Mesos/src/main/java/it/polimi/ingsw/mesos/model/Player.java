package it.polimi.ingsw.mesos.model;

import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.ConnectionStatus;

/**
 * Models a game participant's identity, resources, connection state, and
 * owned tribe.
 */
public final class Player {
    private final String nickname;
    private final Color color;
    private final Tribe tribe;
    private ConnectionStatus connection_status;
    private int food;
    private int prestige_points;

    /**
     * Creates a player with a fresh tribe, zero prestige, and connected status.
     *
     * @param name player nickname
     * @param color player color
     * @param food initial food supply
     * @throws IllegalArgumentException if the initial food is negative
     */
    public Player(String name, Color color, int food) {
        this.nickname = name;
        this.color = color;
        if (food < 0) {
            throw new IllegalArgumentException("Initial food must be non-negative");
        }
        this.food = food;
        this.prestige_points = 0;
        this.connection_status = ConnectionStatus.CONNECTED;
        this.tribe = new Tribe();
    }

    /**
     * Returns the player's nickname.
     *
     * @return player nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns the player's color.
     *
     * @return player color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the player's tribe.
     *
     * @return mutable tribe owned by this player
     */
    public Tribe getTribe() {
        return tribe;
    }

    /**
     * Returns the current food supply.
     *
     * @return current food amount
     */
    public int getFood() {
        return food;
    }
    
    /**
     * Returns the current prestige score.
     *
     * @return current prestige points
     */
    public int getPrestigePoints() {
        return prestige_points;
    }

    /**
     * Returns the player's current connection status.
     *
     * @return connection status
     */
    public ConnectionStatus getConnectionStatus() {
        return connection_status;
    }

    /**
     * Updates the connection status if the transition is valid.
     * Setting the same status again is a no-op.
     *
     * @param status new connection status
     * @throws IllegalArgumentException if the status is null
     * @throws IllegalStateException if the transition is not allowed from the
     *                               current status
     */
    public void setConnectionStatus(ConnectionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("connection status cannot be null");
        }
        if (status == connection_status) {
            return;
        }

        boolean valid = switch (connection_status) {
            case CONNECTED -> status == ConnectionStatus.DISCONNECTED;
            case DISCONNECTED -> status == ConnectionStatus.CONNECTED || status == ConnectionStatus.SKIPPED;
            case SKIPPED -> status == ConnectionStatus.REJOINED;
            case REJOINED -> status == ConnectionStatus.CONNECTED || status == ConnectionStatus.SKIPPED;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid connection status transition: " + connection_status + " -> " + status);
        }
        connection_status = status;
    }

    /**
     * Adds food to the player's supply.
     *
     * @param amount non-negative amount of food to add
     * @throws IllegalArgumentException if the amount is negative
     */
    public void gainFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food gained must be non-negative");
        }

        this.food += amount;
    }

    /**
     * Removes food from the player and returns the unpaid remainder when the
     * requested amount is greater than the current supply.
     *
     * @param amount non-negative amount of food to remove
     * @return the amount that could not be paid because the player lacked food
     * @throws IllegalArgumentException if the amount is negative
     */
    public int loseFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Food lost must be non-negative");
        }

        int out = amount > this.food ? amount - this.food : 0;
        this.food = amount >= this.food ? 0 : this.food - amount;
        return out;
    }

    /**
     * Applies a prestige-point delta to the player score.
     * Negative deltas are allowed and no lower bound is enforced.
     *
     * @param points points to add, or a negative value to subtract
     */
    public void changePrestigePoints(int points) {
        this.prestige_points += points;
    }
}
