package it.polimi.ingsw.mesos.model.cards.characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.model.cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.model.cards.CharacterCard;
import it.polimi.ingsw.mesos.model.enums.Era;

/**
 * Character card that contributes ritual stars during shamanic ritual events.
 */
public final class ShamanCard extends CharacterCard {
    private final int num_stars;

    /**
     * Creates a shaman card from JSON data.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @param num_stars ritual stars printed on this shaman
     * @throws IllegalArgumentException if the minimum player count is invalid
     *                                  or the number of stars is outside 1 to 3
     */
    @JsonCreator
    public ShamanCard(
            @JsonProperty("era")        Era era,
            @JsonProperty("minPlayers") int min_players,
            @JsonProperty("numStars")   int num_stars
    ) {
        super(era, min_players);

        if (num_stars < 1 || num_stars > 3) throw new IllegalArgumentException("Number of stars must be between 1 and 3");

        this.num_stars = num_stars;
    }

    /**
     * Returns the ritual stars printed on this shaman.
     *
     * @return number of stars
     */
    public int getStars() {
        return num_stars;
    }

    /**
     * Delegates this shaman card's description to the supplied visitor.
     *
     * @param visitor visitor that describes shaman cards
     * @return visitor-provided shaman description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeShamanCard(this);
    }

    /**
     * Returns the display name of this character type.
     *
     * @return {@code "Shaman"}
     */
    @Override
    public String getReadableName() {
        return "Shaman";
    }
}
