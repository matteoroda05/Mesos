package it.polimi.ingsw.mesos.model;

/**
 * Result of a food/prestige calculation.
 *
 * @param food food gained or missing
 * @param prestige prestige gained
 * @param penalty penalty applied when food is missing
 */
public record Result(int food, int prestige, int penalty) {
    /**
     * Validates penalty consistency with the food result.
     */
    public Result {
        if (penalty < 0) throw new IllegalArgumentException("penalty cannot be negative");
        if (food > 0 && penalty != 0) throw new IllegalArgumentException("If food is positive penalty has to be 0");
        if (food < 0 && penalty == 0) throw new IllegalArgumentException("If food is negative there must be a penalty");
    }
}
