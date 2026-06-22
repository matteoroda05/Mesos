package it.polimi.ingsw.mesos.model.cards.characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.model.cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.model.cards.CharacterCard;
import it.polimi.ingsw.mesos.model.enums.Era;

/**
 * Character card that reduces the food required during sustenance events.
 */
public final class GathererCard extends CharacterCard {
    /**
     * Creates a gatherer card from JSON data.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range accepted by the base card
     */
    @JsonCreator
    public GathererCard(
            @JsonProperty("era") Era era,
            @JsonProperty("minPlayers") int min_players
    ) {
        super(era, min_players);
    }

    /**
     * Returns the fixed sustenance discount granted by this gatherer.
     *
     * @return food requirement discount
     */
    public int getDiscount() {
        return 3;
    }

    /**
     * Delegates this gatherer card's description to the supplied visitor.
     *
     * @param visitor visitor that describes gatherer cards
     * @return visitor-provided gatherer description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeGathererCard(this);
    }

    /**
     * Returns the display name of this character type.
     *
     * @return {@code "Gatherer"}
     */
    @Override
    public String getReadableName() {
        return "Gatherer";
    }
}
