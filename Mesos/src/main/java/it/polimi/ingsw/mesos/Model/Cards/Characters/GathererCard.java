package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

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

    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.visit(this);
    }
}
