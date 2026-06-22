package it.polimi.ingsw.mesos.model.enums;

/**
 * Enumerates the three chronological eras of a game.
 */
public enum Era { 
    FIRST,
    SECOND,
    THIRD;

    /**
     * Returns the era that immediately follows this one.
     *
     * @return the next era
     * @throws IllegalStateException if this era is already the last one
     */
    public Era next() {
        Era[] eras = values();
        int nextIndex = this.ordinal() + 1;

        if (nextIndex >= eras.length) {
            throw new IllegalStateException("Already at last era");
        }

        return eras[nextIndex];
    }
}
