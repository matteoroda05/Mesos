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

    private CardDescriptionVisitorImpl() {}

    @Override
    public String describeBuilderCard(BuilderCard card) {
        return "Building discount " + card.getBuildingDiscount()
                + ", end game +" + card.getPrestigePoints() + " pp";
    }

    @Override
    public String describeGathererCard(GathererCard card) {
        return "Sustenance discount " + card.getDiscount() + " food";
    }

    @Override
    public String describeHunterCard(HunterCard card) {
        return card.isActiveHunter() ? "Active hunter" : "Hunter";
    }

    @Override
    public String describeInventorCard(InventorCard card) {
        return "Icon " + card.getInventionIcon();
    }

    @Override
    public String describeShamanCard(ShamanCard card) {
        return card.getStars() + " shamanic stars";
    }

    @Override
    public String describeArtistCard(ArtistCard card) {
        return "";
    }

    @Override
    public String describeHuntEvent(HuntEvent card) {
        return "+" + card.getPrestigePerHunter() + " pp and +" + card.getFoodPerHunter()
                + " food per hunter";
    }

    @Override
    public String describeCavePaintingEvent(CavePaintingEvent card) {
        return "With at least " + card.getThreshold() + " artists: +"
                + card.getPointsPerArtist()
                + " pp each, otherwise " + card.getPenalty() + " pp";
    }

    @Override
    public String describeShamanicRitualEvent(ShamanicRitualEvent card) {
        return "Winners +" + card.getWinnerBonus()
                + " pp, losers " + card.getLoserPenalty() + " pp";
    }

    @Override
    public String describeSustenanceEvent(SustenanceEvent card) {
        return card.getFoodPerCharacter() + " food per character, each deficit costs "
                + card.getPointPenalty() + " pp";
    }

    @Override
    public String describeBuildingBonusEvent(BuildingBonusEvent card) {
        return "On " + formatTrigger(card.getTrigger())
                + ": " + formatName(card.getCharacterType().getSimpleName())
                + " gives " + signed(card.getFoodBonus(), " food")
                + ", +" + card.getPrestigeBonus() + " pp";
    }

    @Override
    public String describeBuildingEndTurn(BuildingEndTurn card) {
        return "End turn: +" + card.getFoodBonus() + " food with positive turn order tile bonus";
    }

    @Override
    public String describeBuildingOnCard(BuildingOnCard card) {
        return card.isSetCompletion()
                ? "When taking cards: +" + card.getFoodBonus() + " food if you complete a set"
                : "Inventor pair: +" + card.getFoodBonus() + " food with duplicate icon";
    }

    @Override
    public String describeBuildingShamanicRitual(BuildingShamanicRitual card) {
        return card.getStarBonus() != 0
                ? "Ritual: +" + card.getStarBonus() + " stars before scoring"
                : card.getWinMultiplier() != 0
                        ? "Ritual: the winner multiplies by x" + card.getWinMultiplier()
                        : "Ritual: cancel your penalty if you lose";
    }

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
