package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Cards.Events.*;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TribeDeckTest {

    private HunterCard c1;
    private HunterCard c2;
    private ShamanCard c3;
    private ArtistCard c4;
    private InventorCard c5;
    private BuilderCard c6;
    private GathererCard c7;
    private GathererCard c8;

    private CavePaintingEvent e1;
    private HuntEvent e2;
    private ShamanicRitualEvent e3;
    private SustenanceEvent e4;

    @BeforeEach
    void setUp() {
        c1 = new HunterCard(Era.FIRST, 2, true);
        c2 = new HunterCard(Era.SECOND, 2, false);
        c3 = new ShamanCard(Era.FIRST, 2, 1);
        c4 = new ArtistCard(Era.FIRST, 2);
        c5 = new InventorCard(Era.FIRST, 2, InventionIcon.ARROW);
        c6 = new BuilderCard(Era.FIRST, 2, 3, 2);
        c7 = new GathererCard(Era.SECOND, 2);
        c8 = new GathererCard(Era.THIRD, 2);

        e1 = new CavePaintingEvent(Era.FIRST, 1, 2, -2);
        e2 = new HuntEvent(Era.FIRST, 3, 1);
        e3 = new ShamanicRitualEvent(Era.FIRST, 5, -3);
        e4 = new SustenanceEvent(Era.FIRST, -3, 1);

    }

    @Test
    void constructorThrowsExceptionWhenPlayersBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> new TribeDeck(List.of(), Globals.MIN_NUM_PLAYERS - 1));
    }

    @Test
    void constructorThrowsExceptionWhenPlayersAboveMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> new TribeDeck(List.of(), Globals.MAX_NUM_PLAYERS + 1));
    }

    @Test
    void isEmptyReturnsTrueWhenDeckIsEmpty() {
        TribeDeck tribeDeck = new TribeDeck(List.of(), 2);

        assertTrue(tribeDeck.isEmpty());
    }

    @Test
    void isEmptyReturnsFalseWhenDeckHasCards() {
        TribeDeck tribeDeck = new TribeDeck(List.of(c1, c2, c3), 2);

        assertFalse(tribeDeck.isEmpty());
    }

    @Test
    void drawTopRowReturnsPlayersPlusFourCards() {
        TribeDeck tribeDeck = new TribeDeck(List.of(c1, c2, c3, c4, c5, c6, c7, c8), 2);

        List<TribeCard> drawn = tribeDeck.drawTopRow();

        assertEquals(6, drawn.size());
        assertFalse(tribeDeck.isEmpty());
    }

    @Test
    void drawTopRowDrawsCardsFromEndOfDeckInCorrectOrder() {
        TribeDeck tribeDeck = new TribeDeck(List.of(c1, c2, c3, c4, c5, c6, c7, c8), 2);

        List<TribeCard> drawn = tribeDeck.drawTopRow();

        assertEquals(List.of(c8, c7, c6, c5, c4, c3), drawn);
    }

    @Test
    void drawInitialBottomRowReturnsPlayersPlusOneCards() {
        TribeDeck tribeDeck = new TribeDeck(List.of(c1, c2, c3, c4, c5), 2);

        List<TribeCard> drawn = tribeDeck.drawInitialBottomRow();

        assertEquals(3, drawn.size());
    }

    @Test
    void drawInitialBottomRowReturnsOnlyNonEventCards() {
        TribeDeck tribeDeck = new TribeDeck(List.of(c1, e1, c2, c3, e2, c4, e3, c5), 2);

        List<TribeCard> drawn = tribeDeck.drawInitialBottomRow();

        assertEquals(3, drawn.size());
        assertTrue(drawn.stream().noneMatch(Card::isEvent));
    }

    @Test
    void drawInitialBottomRowSkipsEventsAndStillDrawsEnoughNonEventCards() {
        TribeDeck tribeDeck = new TribeDeck(List.of(c1, e1, c2, c3, e2, c4, e3, c5), 2);

        List<TribeCard> drawn = tribeDeck.drawInitialBottomRow();

        assertEquals(List.of(c5, c4, c3), drawn);
    }

    @Test
    void constructorWithDeckCopiesInputList() {
        List<TribeCard> originalDeck = new ArrayList<>(List.of(c1, c2, c3, c4, c5, c6));
        TribeDeck tribeDeck = new TribeDeck(originalDeck, 2);

        originalDeck.clear();

        List<TribeCard> drawn = tribeDeck.drawTopRow();

        assertEquals(6, drawn.size());
        assertEquals(List.of(c6, c5, c4, c3, c2, c1), drawn);
    }
}