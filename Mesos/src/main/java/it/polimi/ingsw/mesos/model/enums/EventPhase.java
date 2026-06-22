package it.polimi.ingsw.mesos.model.enums;

/**
 * Distinguishes building effects applied before an event outcome from those
 * applied after it.
 */
public enum EventPhase {
    PRE_EVENT,  // before calling calculateOutcome() : changes the inputs
    POST_EVENT  // after calling calculateOutcome() : changes the outputs
}
