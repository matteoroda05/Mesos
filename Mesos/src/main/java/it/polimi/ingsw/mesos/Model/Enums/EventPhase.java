package it.polimi.ingsw.mesos.Model.Enums;

public enum EventPhase {
    PRE_EVENT,  // before calling calculateOutcome() : changes the inputs
    POST_EVENT  // after calling calculateOutcome() : changes the outputs
}
