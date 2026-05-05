package it.polimi.ingsw.mesos.Controller;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.GameContext;

class ModelControllerTest {

    private ModelController model_controller;
    private GameContext game_context;

    @BeforeEach
    void setUp() {
        GameSetup game_setup = new GameSetup();
        try {
            game_context = game_setup.createGame("TestGame", 3, CardLoader.load());
        } catch (IOException e) {
            return;
        }

        List<String> player_names = List.of("Nicola", "Mathis", "Elia");
        game_setup.createMapOfPlayers(game_context, player_names);
        model_controller = new ModelController(game_context);
    }

    @Test
    void constructorInitializesCorrectly() {
        assertNotNull(model_controller);
        assertEquals(game_context, model_controller.getGameContext());
        assertNotNull(model_controller);
    }

    @Test
    void constructorThrowsExceptionWhenGameContextIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelController(null));
    }

    @Test
    void getGameContextReturnsCorrectGameContext() {
        assertEquals(game_context, model_controller.getGameContext());
    }

    @Test
    void getEventsBottomReturnsBottomRowEvents() {
        List<EventCard> events = model_controller.getEventsBottom();
        assertNotNull(events);
    }

    @Test
    void getAllEventsReturnsAllEvents() {
        List<EventCard> events = model_controller.getAllEvents();
        assertNotNull(events);
    }

    @Test
    void endRoundIncreasesRoundNumber() {
        int initialRound = game_context.getRoundNumber();
        model_controller.endRound();
        assertEquals(initialRound + 1, game_context.getRoundNumber());
    }

//    @Test
//    void playerEndTurnMovesPlayerFromOfferTrackToTurnOrderTile() {
//        // Place player on offer track
//        gameTable.getOfferTrack().getTile(0).occupy(Color.BLUE);
//
//        int initialTurnOrderSize = gameTable.getTurnOrderTile().currentOrderTopToBottom().size();
//
//        model_controller.playerEndTurn();
//
//        // Verify player was removed from offer track
//        assertTrue(gameTable.getOfferTrack().getTile(0).isAvailable());
//        // Verify player was added to turn order tile
//        assertEquals(initialTurnOrderSize + 1, gameTable.getTurnOrderTile().currentOrderTopToBottom().size());
//    }

    @Test
    void playerChooseCardThrowsExceptionWhenCardIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerChooseCard(Color.BLUE, -1, true));
    }

    @Test
    void playerChooseCardThrowsExceptionWhenPlayerColorIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerChooseCard(null, 0, true));
    }

//    @Test
//    void playerChooseCardThrowsExceptionWhenCardIsOutOfRange() {
//        MarketRow topRow = gameTable.getTopRow();
//        int totalCards = topRow.getTribeCards().size() + topRow.getBuildingCards().size();
//
//        assertThrows(IllegalArgumentException.class,
//                () -> model_controller.playerChooseCard(Color.BLUE, totalCards + 10, true));
//    }

//    @Test
//    void playerChooseCardTakesCharacterCardFromTopRow() {
//        MarketRow topRow = gameTable.getTopRow();
//        List<Card> tribeCards = topRow.getTribeCards();
//
//        // Find first non-event character card
//        int cardIndex = -1;
//        for (int i = 0; i < tribeCards.size(); i++) {
//            if (tribeCards.get(i) instanceof CharacterCard) {
//                cardIndex = i;a
//                break;
//            }
//        }
//
//        if (cardIndex >= 0) {
//            int initialTribeSize = player1.getTribe().getCards().size();
//            model_controller.playerChooseCard(Color.BLUE, cardIndex, true);
//            assertEquals(initialTribeSize + 1, player1.getTribe().getCards().size());
//        }
//    }

//    @Test
//    void playerChooseCardTakesBuildingCardWhenPlayerHasEnoughFood() {
//        MarketRow topRow = gameTable.getTopRow();
//        int cardIndex = topRow.getTribeCards().size(); // First building card
//
//        if (!topRow.getBuildingCards().isEmpty()) {
//            int initialFood = player1.getFood();
//            int initialTribeSize = player1.getTribe().getCards().size();
//            int cost = topRow.getBuildingCost(0) - player1.getTribe().buildersDiscount();
//
//            if (cost <= initialFood) {
//                model_controller.playerChooseCard(Color.BLUE, cardIndex, true);
//                assertEquals(initialTribeSize + 1, player1.getTribe().getCards().size());
//                assertEquals(initialFood - cost, player1.getFood());
//            }
//        }
//    }

//    @Test
//    void playerChooseCardThrowsExceptionWhenPlayerCannotAffordBuilding() {
//        // Create a player with very little food
//        Player poorPlayer = new Player("PoorPlayer", Color.GREEN, 0);
//        Map<Color, Player> players = new EnumMap<>(Color.class);
//        players.put(Color.BLUE, player1);
//        players.put(Color.YELLOW, player2);
//        players.put(Color.GREEN, poorPlayer);
//
//        MarketRow topRow = gameTable.getTopRow();
//        int buildingIndex = topRow.getTribeCards().size();
//
//        if (!topRow.getBuildingCards().isEmpty()) {
//            assertThrows(IllegalArgumentException.class,
//                    () -> model_controller.playerChooseCard(Color.GREEN, buildingIndex, true));
//        }
//    }

//    @Test
//    void playerChoseOfferPlacesPlayerOnCorrectOfferTile() {
//        gameTable.getTurnOrderTile().clearFirst();
//        gameTable.getTurnOrderTile().placeTotem(player1);
//
//        model_controller.playerChoseOffer(Color.BLUE, 1);
//
//        assertFalse(gameTable.getOfferTrack().getTile(1).isAvailable());
//        assertEquals(Color.BLUE, gameTable.getOfferTrack().getTile(1).getOccupant());
//    }

//    @Test
//    void playerChoseOfferThrowsExceptionWhenPlayerIsNotFirstInTurnOrder() {
//        // Ensure player2 is first
//        gameTable.getTurnOrderTile().clearFirst();
//        gameTable.getTurnOrderTile().clearFirst();
//        gameTable.getTurnOrderTile().placeTotem(player2);
//        gameTable.getTurnOrderTile().placeTotem(player1);
//
//        assertThrows(IllegalStateException.class,
//                () -> model_controller.playerChoseOffer(Color.BLUE, 1));
//    }

//    @Test
//    void playerChangeFoodIncreasesPlayerFood() {
//        int initialFood = player1.getFood();
//        model_controller.playerChangeFood(5, Color.BLUE);
//        assertEquals(initialFood + 5, player1.getFood());
//    }

    @Test
    void playerChangeFoodThrowsExceptionWhenPlayerColorIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerAddFood(5, null));
    }

//    @Test
//    void playerChangeFoodWithSubstituteDecreasesFood() {
//        player1.gainFood(5);
//        int initialFood = player1.getFood();
//
//        model_controller.playerChangeFood(-3, Color.BLUE, 2);
//
//        assertEquals(initialFood - 3, player1.getFood());
//    }

//    @Test
//    void playerChangeFoodWithSubstituteUsesPrestigeWhenNotEnoughFood() {
//        int initialFood = player1.getFood();
//        int initialPrestige = player1.getPrestigePoints();
//
//        model_controller.playerChangeFood(-15, Color.BLUE, 2);
//
//        assertEquals(0, player1.getFood());
//        // Food deficit: 15 - initialFood, so prestige change: 2 * (initialFood - 15)
//        assertEquals(initialPrestige + 2 * (initialFood - 15), player1.getPrestigePoints());
//    }

//    @Test
//    void playerChangePrestigeUpdatesPlayerPrestige() {
//        int initialPrestige = player1.getPrestigePoints();
//        model_controller.playerChangePrestige(10, Color.BLUE);
//        assertEquals(initialPrestige + 10, player1.getPrestigePoints());
//    }

//    @Test
//    void playerChangePrestigeCanDecreasePrestige() {
//        player1.changePrestigePoints(20);
//        int initialPrestige = player1.getPrestigePoints();
//
//        model_controller.playerChangePrestige(-5, Color.BLUE);
//
//        assertEquals(initialPrestige - 5, player1.getPrestigePoints());
//    }

    @Test
    void playerChangePrestigeThrowsExceptionWhenPlayerColorIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerChangePrestige(5, null));
    }
}
