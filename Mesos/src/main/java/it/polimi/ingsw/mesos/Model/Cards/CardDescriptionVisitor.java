package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingBonusEvent;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndTurn;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.GathererCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ShamanCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.CavePaintingEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;

/**
 * Visitor that produces a human-readable description for a concrete card.
 * Each concrete card type accepts this visitor through {@link Card#accept}.
 */
public interface CardDescriptionVisitor {
    String visit(BuilderCard card);
    String visit(GathererCard card);
    String visit(HunterCard card);
    String visit(InventorCard card);
    String visit(ShamanCard card);
    String visit(ArtistCard card);
    String visit(HuntEvent card);
    String visit(CavePaintingEvent card);
    String visit(ShamanicRitualEvent card);
    String visit(SustenanceEvent card);
    String visit(BuildingBonusEvent card);
    String visit(BuildingEndTurn card);
    String visit(BuildingOnCard card);
    String visit(BuildingShamanicRitual card);
    String visit(BuildingEndGame card);
    String visit(BuildingAfterEveryone card);
}
