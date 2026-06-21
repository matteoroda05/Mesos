package it.polimi.ingsw.mesos.model.cards.characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.model.cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.model.cards.CharacterCard;
import it.polimi.ingsw.mesos.model.enums.Era;

/**
 * Character card representing an artist, whose quantity contributes to
 * cave-painting and end-game scoring.
 */
public final class ArtistCard extends CharacterCard {

    /**
     * Creates an artist card from JSON data.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range accepted by the base card
     */
    @JsonCreator
    public ArtistCard(
            @JsonProperty("era")        Era era,
            @JsonProperty("minPlayers") int min_players
    ) {
        super(era, min_players);
    }

    /**
     * Delegates this artist card's description to the supplied visitor.
     *
     * @param visitor visitor that describes artist cards
     * @return visitor-provided artist description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeArtistCard(this);
    }

    /**
     * Returns the display name of this character type.
     *
     * @return {@code "Artist"}
     */
    @Override
    public String getReadableName() {
        return "Artist";
    }
}
