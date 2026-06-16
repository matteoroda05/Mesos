package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Network.Messages.EventNotification;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

public interface GameServerMessageVisitor {
    void visitEventNotification(EventNotification notification);
    void visitGameViewUpdate(GameViewUpdate update);
}
