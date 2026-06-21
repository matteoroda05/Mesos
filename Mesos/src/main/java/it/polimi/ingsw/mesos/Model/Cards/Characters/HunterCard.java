package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;

/**
 * Character card representing a hunter, which contributes to hunt-event
 * rewards and can be marked active.
 */
public final class HunterCard extends CharacterCard {
    private final boolean active_hunter;

    /**
     * Creates a hunter card from JSON data.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @param active_hunter whether this hunter is active for hunt effects
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range accepted by the base card
     */
    @JsonCreator
    public HunterCard(
            @JsonProperty("era")          Era era,
            @JsonProperty("minPlayers")   int min_players,
            @JsonProperty("activeHunter") boolean active_hunter
    ) {
        super(era, min_players);
        this.active_hunter = active_hunter;
    }

    /**
     * Returns whether this hunter is marked active.
     *
     * @return true when active
     */
    public boolean isActiveHunter() {
        return active_hunter;
    }

    /**
     * Delegates this hunter card's description to the supplied visitor.
     *
     * @param visitor visitor that describes hunter cards
     * @return visitor-provided hunter description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeHunterCard(this);
    }

    /**
     * Returns the display name of this character type.
     *
     * @return {@code "Hunter"}
     */
    @Override
    public String getReadableName() {
        return "Hunter";
    }
}
