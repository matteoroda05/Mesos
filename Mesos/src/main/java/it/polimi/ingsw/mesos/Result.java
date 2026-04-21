package it.polimi.ingsw.mesos;

public record Result(int food, int prestige, int penalty) {
    public Result {
        if (penalty < 0) throw new IllegalArgumentException("penalty cannot be negative");
        if (food > 0 && penalty != 0) throw new IllegalArgumentException("If food is positive penalty has to be 0");
        if (food < 0 && penalty == 0) throw new IllegalArgumentException("If food is negative there must be a penalty");
    }
}
