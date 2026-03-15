package it.polimi.ingsw.mesos.Model.Enums;

public enum Era { 
    FIRST,
    SECOND,
    THIRD;

    public Era next() {
        Era[] eras = values();
        int nextIndex = this.ordinal() + 1;

        if (nextIndex >= eras.length) {
            throw new IllegalStateException("Already at last era");
        }

        return eras[nextIndex];
    }
}
