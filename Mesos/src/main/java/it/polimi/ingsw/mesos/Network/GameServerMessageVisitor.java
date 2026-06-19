package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Messages.EventNotification;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

public interface GameServerMessageVisitor {
    /**
     * Handles a notification describing an event or ancillary game effect.
     *
     * @param notification event notification to handle
     */
    void visitEventNotification(EventNotification notification);
    /**
     * Handles an update containing a new game snapshot.
     *
     * @param update game-view update to handle
     */
    void visitGameViewUpdate(GameViewUpdate update);
}
