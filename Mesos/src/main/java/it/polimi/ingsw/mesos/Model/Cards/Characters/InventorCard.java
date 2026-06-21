package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

/**
 * Character card identified by an invention icon used in tribe scoring and
 * invention-related building effects.
 */
public final class InventorCard extends CharacterCard {
    private final InventionIcon invention_icon;

    /**
     * Creates an inventor card from JSON data.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @param invention_icon icon represented by this inventor
     * @throws IllegalArgumentException if the minimum player count is invalid
     *                                  or the invention icon is null
     */
    @JsonCreator
    public InventorCard(
            @JsonProperty("era")           Era era,
            @JsonProperty("minPlayers")    int min_players,
            @JsonProperty("inventionIcon") InventionIcon invention_icon
    ) {
        super(era, min_players);
        if (invention_icon == null) throw new IllegalArgumentException("Invention Icon cannot be null");
        this.invention_icon = invention_icon;
    }

    /**
     * Returns the invention icon represented by this inventor.
     *
     * @return invention icon
     */
    public InventionIcon getInventionIcon() {
        return this.invention_icon;
    }

    /**
     * Delegates this inventor card's description to the supplied visitor.
     *
     * @param visitor visitor that describes inventor cards
     * @return visitor-provided inventor description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeInventorCard(this);
    }

    /**
     * Returns the display name of this character type.
     *
     * @return {@code "Inventor"}
     */
    @Override
    public String getReadableName() {
        return "Inventor";
    }
}
