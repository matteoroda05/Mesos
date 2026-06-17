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
    String describeBuilderCard(BuilderCard card);
    String describeGathererCard(GathererCard card);
    String describeHunterCard(HunterCard card);
    String describeInventorCard(InventorCard card);
    String describeShamanCard(ShamanCard card);
    String describeArtistCard(ArtistCard card);
    String describeHuntEvent(HuntEvent card);
    String describeCavePaintingEvent(CavePaintingEvent card);
    String describeShamanicRitualEvent(ShamanicRitualEvent card);
    String describeSustenanceEvent(SustenanceEvent card);
    String describeBuildingBonusEvent(BuildingBonusEvent card);
    String describeBuildingEndTurn(BuildingEndTurn card);
    String describeBuildingOnCard(BuildingOnCard card);
    String describeBuildingShamanicRitual(BuildingShamanicRitual card);
    String describeBuildingEndGame(BuildingEndGame card);
    String describeBuildingAfterEveryone(BuildingAfterEveryone card);
}
