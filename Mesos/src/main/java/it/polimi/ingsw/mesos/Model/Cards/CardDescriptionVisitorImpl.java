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
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;

/**
 * Stateless {@link CardDescriptionVisitor} that builds the short human-readable
 * effect description shown to clients for each concrete card type.
 */
public class CardDescriptionVisitorImpl implements CardDescriptionVisitor {

    /** Shared instance: this visitor holds no state. */
    public static final CardDescriptionVisitorImpl INSTANCE = new CardDescriptionVisitorImpl();

    /**
     * Prevents construction outside the shared stateless instance.
     */
    private CardDescriptionVisitorImpl() {}

    /**
     * Builds the builder card's discount and end-game prestige summary.
     *
     * @param card builder card to describe
     * @return formatted builder summary
     */
    @Override
    public String describeBuilderCard(BuilderCard card) {
        return "Building discount " + card.getBuildingDiscount()
                + ", end game +" + card.getPrestigePoints() + " pp";
    }

    /**
     * Builds the gatherer card's sustenance-discount summary.
     *
     * @param card gatherer card to describe
     * @return formatted gatherer summary
     */
    @Override
    public String describeGathererCard(GathererCard card) {
        return "Sustenance discount " + card.getDiscount() + " food";
    }

    /**
     * Indicates whether the hunter card is currently active.
     *
     * @param card hunter card to describe
     * @return active or inactive hunter label
     */
    @Override
    public String describeHunterCard(HunterCard card) {
        return card.isActiveHunter() ? "Active hunter" : "Hunter";
    }

    /**
     * Builds the inventor card's invention-icon summary.
     *
     * @param card inventor card to describe
     * @return formatted invention-icon label
     */
    @Override
    public String describeInventorCard(InventorCard card) {
        return "Icon " + card.getInventionIcon();
    }

    /**
     * Builds the shaman card's star-count summary.
     *
     * @param card shaman card to describe
     * @return formatted shamanic-star count
     */
    @Override
    public String describeShamanCard(ShamanCard card) {
        return card.getStars() + " shamanic stars";
    }

    /**
     * Returns the artist card's current empty effect description.
     *
     * @param card artist card to describe
     * @return an empty string
     */
    @Override
    public String describeArtistCard(ArtistCard card) {
        return "";
    }

    /**
     * Builds the hunt event's per-hunter reward summary.
     *
     * @param card hunt event to describe
     * @return formatted hunt reward description
     */
    @Override
    public String describeHuntEvent(HuntEvent card) {
        return "+" + card.getPrestigePerHunter() + " pp and +" + card.getFoodPerHunter()
                + " food per hunter";
    }

    /**
     * Builds the cave-painting event's threshold and scoring summary.
     *
     * @param card cave-painting event to describe
     * @return formatted cave-painting outcome description
     */
    @Override
    public String describeCavePaintingEvent(CavePaintingEvent card) {
        return "With at least " + card.getThreshold() + " artists: +"
                + card.getPointsPerArtist()
                + " pp each, otherwise " + card.getPenalty() + " pp";
    }

    /**
     * Builds the shamanic ritual's winner and loser scoring summary.
     *
     * @param card shamanic-ritual event to describe
     * @return formatted ritual outcome description
     */
    @Override
    public String describeShamanicRitualEvent(ShamanicRitualEvent card) {
        return "Winners +" + card.getWinnerBonus()
                + " pp, losers " + card.getLoserPenalty() + " pp";
    }

    /**
     * Builds the sustenance event's food requirement and penalty summary.
     *
     * @param card sustenance event to describe
     * @return formatted sustenance outcome description
     */
    @Override
    public String describeSustenanceEvent(SustenanceEvent card) {
        return card.getFoodPerCharacter() + " food per character, each deficit costs "
                + card.getPointPenalty() + " pp";
    }

    /**
     * Builds the event-triggered building's bonus summary.
     *
     * @param card bonus-event building to describe
     * @return formatted building trigger and reward description
     */
    @Override
    public String describeBuildingBonusEvent(BuildingBonusEvent card) {
        return "On " + formatTrigger(card.getTrigger())
                + ": " + formatName(card.getCharacterType().getSimpleName())
                + " gives " + signed(card.getFoodBonus(), " food")
                + ", +" + card.getPrestigeBonus() + " pp";
    }

    /**
     * Builds the end-turn building's conditional food-bonus summary.
     *
     * @param card end-turn building to describe
     * @return formatted end-turn effect description
     */
    @Override
    public String describeBuildingEndTurn(BuildingEndTurn card) {
        return "End turn: +" + card.getFoodBonus() + " food with positive turn order tile bonus";
    }

    /**
     * Builds the card-selection building's conditional food-bonus summary.
     *
     * @param card on-card building to describe
     * @return formatted card-selection effect description
     */
    @Override
    public String describeBuildingOnCard(BuildingOnCard card) {
        return card.isSetCompletion()
                ? "When taking cards: +" + card.getFoodBonus() + " food if you complete a set"
                : "Inventor pair: +" + card.getFoodBonus() + " food with duplicate icon";
    }

    /**
     * Builds the shamanic ritual building's scoring-modifier summary.
     *
     * @param card ritual building to describe
     * @return formatted ritual-modifier description
     */
    @Override
    public String describeBuildingShamanicRitual(BuildingShamanicRitual card) {
        return card.getStarBonus() != 0
                ? "Ritual: +" + card.getStarBonus() + " stars before scoring"
                : card.getWinMultiplier() != 0
                        ? "Ritual: the winner multiplies by x" + card.getWinMultiplier()
                        : "Ritual: cancel your penalty if you lose";
    }

    /**
     * Builds the end-game building's first applicable scoring summary.
     *
     * @param card end-game building to describe
     * @return formatted scoring description, or an empty string when no known
     *         scoring rule applies
     */
    @Override
    public String describeBuildingEndGame(BuildingEndGame card) {
        if (card.isDoubleBuildersPrestige()) {
            return "End game: double builders' prestige";
        }
        if (card.getCharacterSetBonusPrestige() != 0) {
            return "End game: +" + card.getCharacterSetBonusPrestige() + " pp per complete character set";
        }
        if (card.getCharacterMultiplier() != 0 && card.getCharacterType() != null) {
            return "End game: +" + card.getCharacterMultiplier() + " pp per "
                    + formatName(card.getCharacterType().getSimpleName());
        }
        if (card.getPrestigeBonus() != 0) {
            return "End game: +" + card.getPrestigeBonus() + " pp";
        }
        return "";
    }

    /**
     * Returns the after-everyone building's current empty effect description.
     *
     * @param card after-everyone building to describe
     * @return an empty string
     */
    @Override
    public String describeBuildingAfterEveryone(BuildingAfterEveryone card) {
        return "";
    }

    /**
     * Converts a class-style name into a readable display name.
     *
     * @param rawName raw class or type name
     * @return formatted display name
     */
    public static String formatName(String rawName) {
        String baseName = rawName
                .replace("Card", "")
                .replace("Event", " Event");

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < baseName.length(); index++) {
            char character = baseName.charAt(index);
            if (index > 0 && Character.isUpperCase(character) && baseName.charAt(index - 1) != ' ') {
                builder.append(' ');
            }
            builder.append(character);
        }
        return builder.toString().trim();
    }

    /**
     * Formats a building trigger enum for display.
     *
     * @param trigger trigger to format
     * @return lowercase trigger label without the event prefix
     * @throws NullPointerException if trigger is null
     */
    private static String formatTrigger(BuildingTrigger trigger) {
        return trigger.name()
                .replace("ON_EVENT_", "")
                .replace('_', ' ')
                .toLowerCase();
    }

    /**
     * Formats a signed numeric value with a suffix.
     *
     * @param value value to format
     * @param suffix suffix appended after the value
     * @return value with a leading plus sign when non-negative
     */
    public static String signed(int value, String suffix) {
        if (value >= 0) {
            return "+" + value + suffix;
        }
        return value + suffix;
    }
}
