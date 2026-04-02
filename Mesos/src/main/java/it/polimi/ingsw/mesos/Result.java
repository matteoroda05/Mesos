package it.polimi.ingsw.mesos;

public record Result(int food, int prestige, int penalty) {
    public Result {
        if (penalty < 0) throw new IllegalArgumentException("penalty cannot be negative");
        if (food >= 0 && penalty != 0) throw new IllegalArgumentException("If food is positive penalty has to be 0");
        if (penalty != 0 && prestige != 0) throw new IllegalArgumentException("If penalty is positive prestige has to be 0");
    }
}
