package it.polimi.ingsw.mesos.Model.Enums;

/**
 * Distinguishes building effects applied before an event outcome from those
 * applied after it.
 */
public enum EventPhase {
    PRE_EVENT,  // before calling calculateOutcome() : changes the inputs
    POST_EVENT  // after calling calculateOutcome() : changes the outputs
}
