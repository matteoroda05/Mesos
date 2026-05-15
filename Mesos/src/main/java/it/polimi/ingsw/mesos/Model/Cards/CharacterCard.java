package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.Era;

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
}
