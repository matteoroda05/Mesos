package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Network.CardSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

import static org.junit.jupiter.api.Assertions.*;

public class CardTest {
    private Card card;

    static class TestCard extends Card {
        public TestCard(Era era, int minPlayers) {
            super(era, minPlayers);
        }

        @Override
        public String accept(CardDescriptionVisitor visitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CardSummary getSummary(int selectionIndex) {
            return null;
        }

        @Override
        public String getReadableName() {
            return "";
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
    }

    @Test
    void constructorTestThrowsException1(){
        assertThrows(IllegalArgumentException.class, 
                () -> new TestCard(Era.FIRST, -1));
    }

    @Test
    void constructorTestThrowsException2(){
        assertThrows(IllegalArgumentException.class,
                () -> new TestCard(Era.FIRST, 7));
    }

    @Test
    void cardReturnsFalseToIsEvent(){
        assertFalse(card.isEvent());
    }
}
