package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.CardSummary;

abstract public class CharacterCard extends TribeCard {
    /**
     * Creates a character card.
     *
     * @param era era this card belongs to
     * @param min_players minimum players required to use this card
     * @throws IllegalArgumentException if the minimum player count is outside
     *                                  the range accepted by {@link Card}
     */
    public CharacterCard(Era era, int min_players) {
        super(era, min_players);
    }

    /**
     * Creates the client-facing summary of this character card.
     *
     * @param selectionIndex index used to select this card in its source row
     * @return summary with character details and no building cost or prestige
     */
    @Override
    public CardSummary getSummary(int selectionIndex) {
        return new CardSummary(
                selectionIndex,
                this.getReadableName(),
                this.getEra(),
                false,
                this.accept(CardDescriptionVisitorImpl.INSTANCE),
                null,
                null,
                this.getPresentationId()
        );
    }
}
