package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

class ArtistCardTest {

    @Test
    void constructorInitializesEraCorrectly() {
        ArtistCard card = new ArtistCard(Era.FIRST, 2);
        assertEquals(Era.FIRST, card.getEra());
    }

    @Test
    void constructorInitializesMinPlayersCorrectly() {
        ArtistCard card = new ArtistCard(Era.SECOND, 3);
        assertEquals(3, card.getMinPlayers());
    }

    @Test
    void constructorWithThirdEra() {
        ArtistCard card = new ArtistCard(Era.THIRD, 4);
        assertEquals(Era.THIRD, card.getEra());
        assertEquals(4, card.getMinPlayers());
    }

    @Test
    void isNotAnEvent() {
        ArtistCard card = new ArtistCard(Era.FIRST, 2);
        assertFalse(card.isEvent());
    }
}
