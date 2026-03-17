package it.polimi.ingsw.mesos.Model.Cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

// Test also valid for TribeCard, CharacterCard
public class CardTest {
    private Card card;

    static class TestCard extends Card {
        public TestCard(Era era, int minPlayers) {
            super(era, minPlayers);
        }
    }

    @BeforeEach
    void setUp(){
        this.card = new TestCard(Era.SECOND, 3);
    }

    @Test
    void constructorTestInitialisation(){
        assertEquals(Era.SECOND, card.getEra());
        assertEquals(3, card.getMinPlayers());
        assertTrue(card.getCardId() >= 0);
    }

    @Test
    void constructorTestThrowsException(){
        assertThrows(IllegalArgumentException.class, 
                () -> new TestCard(Era.FIRST, -1));
    }

    @Test
    void differentCardIds(){
        Card card1 = new TestCard(Era.FIRST, 2);
        Card card2 = new TestCard(Era.SECOND, 0);

        assertNotSame(card1.getCardId(), card2.getCardId());
    }
    
    @Test
    void cardReturnsFalseToIsEvent(){
        assertEquals(false, card.isEvent());
    }

    @Test
    void cardComparison1(){
        Card card1 = new TestCard(Era.THIRD, 2);
        Card card2 = new TestCard(Era.SECOND, 0);

        assertTrue(card1.compareTo(card2) > 0);
        assertTrue(card2.compareTo(card1) < 0);
    }

    @Test
    void cardComparison2(){
        Card card1 = new TestCard(Era.SECOND, 2);
        Card card2 = new TestCard(Era.SECOND, 0);

        int id1 = card1.getCardId();
        int id2 = card2.getCardId();

        assertEquals(Integer.compare(id1, id2), card1.compareTo(card2));
        assertEquals(Integer.compare(id2, id1), card2.compareTo(card1));
    }

    @Test
    void cardComparisonThrowsExeption(){
        assertThrows(IllegalArgumentException.class, 
            () -> card.compareTo(null));
    }

    @Test
    void cardComparisonSame(){
        assertEquals(0, card.compareTo(card));
    }
}