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
    /**
     * Describes a builder card's purchase and scoring effect.
     *
     * @param card builder card to describe
     * @return human-readable builder effect description
     */
    String describeBuilderCard(BuilderCard card);
    /**
     * Describes a gatherer card's sustenance discount.
     *
     * @param card gatherer card to describe
     * @return human-readable gatherer effect description
     */
    String describeGathererCard(GathererCard card);
    /**
     * Describes whether a hunter card has an active hunting effect.
     *
     * @param card hunter card to describe
     * @return human-readable hunter effect description
     */
    String describeHunterCard(HunterCard card);
    /**
     * Describes the invention icon provided by an inventor card.
     *
     * @param card inventor card to describe
     * @return human-readable inventor effect description
     */
    String describeInventorCard(InventorCard card);
    /**
     * Describes the number of shamanic stars on a shaman card.
     *
     * @param card shaman card to describe
     * @return human-readable shaman effect description
     */
    String describeShamanCard(ShamanCard card);
    /**
     * Describes an artist card.
     *
     * @param card artist card to describe
     * @return human-readable artist effect description
     */
    String describeArtistCard(ArtistCard card);
    /**
     * Describes the per-hunter rewards of a hunt event.
     *
     * @param card hunt event to describe
     * @return human-readable hunt-event effect description
     */
    String describeHuntEvent(HuntEvent card);
    /**
     * Describes the threshold and outcome of a cave-painting event.
     *
     * @param card cave-painting event to describe
     * @return human-readable cave-painting effect description
     */
    String describeCavePaintingEvent(CavePaintingEvent card);
    /**
     * Describes the winner and loser outcomes of a shamanic ritual.
     *
     * @param card shamanic-ritual event to describe
     * @return human-readable ritual effect description
     */
    String describeShamanicRitualEvent(ShamanicRitualEvent card);
    /**
     * Describes the food requirement and penalty of a sustenance event.
     *
     * @param card sustenance event to describe
     * @return human-readable sustenance effect description
     */
    String describeSustenanceEvent(SustenanceEvent card);
    /**
     * Describes a building triggered by a matching event.
     *
     * @param card bonus-event building to describe
     * @return human-readable building effect description
     */
    String describeBuildingBonusEvent(BuildingBonusEvent card);
    /**
     * Describes an end-turn building effect.
     *
     * @param card end-turn building to describe
     * @return human-readable building effect description
     */
    String describeBuildingEndTurn(BuildingEndTurn card);
    /**
     * Describes a building activated while cards are taken.
     *
     * @param card on-card building to describe
     * @return human-readable building effect description
     */
    String describeBuildingOnCard(BuildingOnCard card);
    /**
     * Describes a building that modifies shamanic ritual scoring.
     *
     * @param card ritual building to describe
     * @return human-readable building effect description
     */
    String describeBuildingShamanicRitual(BuildingShamanicRitual card);
    /**
     * Describes an end-game building scoring effect.
     *
     * @param card end-game building to describe
     * @return human-readable building effect description
     */
    String describeBuildingEndGame(BuildingEndGame card);
    /**
     * Describes a building that grants an action after every player acts.
     *
     * @param card after-everyone building to describe
     * @return human-readable building effect description
     */
    String describeBuildingAfterEveryone(BuildingAfterEveryone card);
}
