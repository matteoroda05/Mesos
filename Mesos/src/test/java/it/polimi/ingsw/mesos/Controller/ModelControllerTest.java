package it.polimi.ingsw.mesos.Controller;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.ConnectionStatus;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.MarketRow;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Result;

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
        assertEquals(game_context, model_controller.getGameContext());
    }

    @Test
    void constructorThrowsExceptionWhenGameContextIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelController(null));
    }

    @Test
    void getNextTurnAndOfferTileReturnNullWhenEmpty() {
        while (!game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().isEmpty()) {
            game_context.getGameTable().getTurnOrderTile().clearFirst();
        }

        assertEquals(null, model_controller.getNextPlayerTurn());
        assertEquals(null, model_controller.getNextPlayerOfferTile());
    }

    @Test
    void accessorsReturnCurrentModelDataWhenCollectionsArePopulated() {
        Player player = firstPlayer();
        BuildingOnCard building = new BuildingOnCard(Era.FIRST, 2, 0, 0, 1, true, false);
        building.setOwner(player);
        player.getTribe().addCard(building);
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(player.getColor());

        assertEquals(player.getColor(), model_controller.getNextPlayerTurn());
        assertEquals(player.getColor(), model_controller.getNextPlayerOfferTile().getOccupant());
        assertEquals(game_context.getPlayers().size(), model_controller.getTribes().size());
        assertEquals(1, model_controller.getBuildingsWithTrigger(BuildingTrigger.ON_ACQUIRE_CHARACTER).size());
        assertEquals(game_context.getGameTable().getTurnOrderTile(), model_controller.getTurnOrderTile());
        assertEquals(game_context.getGameTable(), model_controller.getGameTable());
        assertTrue(model_controller.getAvailableCards(player.getColor(), true) >= 0);
        assertTrue(model_controller.getAvailableCards(player.getColor(), false) >= 0);
    }

    @Test
    void endRoundIncreasesRoundNumber() {
        int initialRound = game_context.getRoundNumber();
        model_controller.endRound();
        assertEquals(initialRound + 1, game_context.getRoundNumber());
    }

    @Test
    void endRoundBeginsNewEraWhenMarketRefillRevealsNextEra() {
        Era initialEra = game_context.getCurrentEra();

        for (int i = 0; i < 20 && game_context.getCurrentEra() == initialEra; i++) {
            model_controller.endRound();
        }

        assertEquals(Era.SECOND, game_context.getCurrentEra());
    }

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

    @Test
    void playerChooseCardRejectsOutOfRangeBuildingSelection() {
        MarketRow topRow = game_context.getGameTable().getTopRow();
        int outOfRange = topRow.getTribeCards().size() + topRow.getBuildingCards().size() + 1;

        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerChooseCard(firstPlayer().getColor(), outOfRange, true));
    }

    @Test
    void playerChooseCardRejectsUnaffordableBuilding() {
        Player player = firstPlayer();
        player.loseFood(player.getFood());
        int firstBuildingIndex = game_context.getGameTable().getTopRow().getTribeCards().size();

        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerChooseCard(player.getColor(), firstBuildingIndex, true));
    }

    @Test
    void playerChooseCardBuysAffordableBuilding() {
        Player player = firstPlayer();
        player.gainFood(20);
        int buildingsBefore = player.getTribe().getNumBuildings();
        int firstBuildingIndex = game_context.getGameTable().getTopRow().getTribeCards().size();

        assertTrue(model_controller.playerChooseCard(player.getColor(), firstBuildingIndex, true).isEmpty());
        assertEquals(buildingsBefore + 1, player.getTribe().getNumBuildings());
    }

    @Test
    void playerChooseCardRejectsExactEndBuildingIndex() {
        Player player = firstPlayer();
        player.gainFood(20);
        MarketRow topRow = game_context.getGameTable().getTopRow();
        int firstInvalidIndex = topRow.getTribeCards().size() + topRow.getBuildingCards().size();

        assertThrows(IndexOutOfBoundsException.class,
                () -> model_controller.playerChooseCard(player.getColor(), firstInvalidIndex, true));
    }

    @Test
    void playerChooseCardRejectsEventsAndHandlesHunterFoodBonusBranches() {
        Player player = firstPlayer();
        MarketRow bottomRow = game_context.getGameTable().getBottomRow();
        bottomRow.removeTribeCards();
        bottomRow.placeCard(new SustenanceEvent(Era.FIRST, -1, 1));

        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerChooseCard(player.getColor(), 0, false));

        bottomRow.removeTribeCards();
        bottomRow.placeCard(new HunterCard(Era.FIRST, 2, true));
        int foodBeforeActiveHunter = player.getFood();
        assertTrue(model_controller.playerChooseCard(player.getColor(), 0, false).isPresent());
        assertEquals(foodBeforeActiveHunter + 1, player.getFood());

        bottomRow.placeCard(new HunterCard(Era.FIRST, 2, false));
        int foodBeforeInactiveHunter = player.getFood();
        assertTrue(model_controller.playerChooseCard(player.getColor(), 0, false).isPresent());
        assertEquals(foodBeforeInactiveHunter, player.getFood());
    }

    @Test
    void playerChangeFoodThrowsExceptionWhenPlayerColorIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerAddFood(5, null));
    }

    @Test
    void playerAddFoodRejectsNegativeAmountsAndAddsValidFood() {
        Player player = firstPlayer();
        int initialFood = player.getFood();

        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerAddFood(-1, player.getColor()));
        model_controller.playerAddFood(2, player.getColor());

        assertEquals(initialFood + 2, player.getFood());
    }

    @Test
    void playerChangePrestigeThrowsExceptionWhenPlayerColorIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerChangePrestige(5, null));
    }

    @Test
    void playerChangePrestigeUpdatesPoints() {
        Player player = firstPlayer();
        int initialPrestige = player.getPrestigePoints();

        model_controller.playerChangePrestige(3, player.getColor());

        assertEquals(initialPrestige + 3, player.getPrestigePoints());
    }

    @Test
    void playerApplyOutcomeHandlesFoodGainLossPrestigeAndValidation() {
        Player player = firstPlayer();
        int initialFood = player.getFood();
        int initialPrestige = player.getPrestigePoints();

        assertThrows(IllegalArgumentException.class,
                () -> model_controller.playerApplyOutcome(new Result(0, 0, 0), null));

        model_controller.playerApplyOutcome(new Result(2, 1, 0), player.getColor());
        assertEquals(initialFood + 2, player.getFood());
        assertEquals(initialPrestige + 1, player.getPrestigePoints());

        model_controller.playerApplyOutcome(new Result(-1, 0, 2), player.getColor());
        assertEquals(initialFood + 1, player.getFood());
        assertEquals(initialPrestige + 1, player.getPrestigePoints());
    }

    @Test
    void playerApplyOutcomeConvertsUnpaidFoodToPrestigePenaltyAndSkipsZeroPrestigeChange() {
        Player player = firstPlayer();
        player.loseFood(player.getFood());

        model_controller.playerApplyOutcome(new Result(-3, 0, 2), player.getColor());

        assertEquals(0, player.getFood());
        assertEquals(-6, player.getPrestigePoints());

        model_controller.playerApplyOutcome(new Result(0, 0, 0), player.getColor());
        assertEquals(-6, player.getPrestigePoints());
    }

    @Test
    void playerEndTurnClearsOfferTileAndPlacesTotemBackInTurnOrder() {
        Player player = firstPlayer();
        game_context.getGameTable().getTurnOrderTile().clear();
        OfferTile tile = game_context.getGameTable().getOfferTrack().getTile(0);
        tile.occupy(player.getColor());

        model_controller.playerEndTurn();

        assertTrue(tile.isAvailable());
        assertFalse(game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().isEmpty());
        assertEquals(player.getColor(), game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().getFirst());
    }

    @Test
    void playerChoseOfferValidatesTurnAndAvailabilityThenOccupiesTile() {
        Color first = model_controller.getNextPlayerTurn();
        Color second = game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().get(1);

        assertThrows(IllegalArgumentException.class, () -> model_controller.playerChoseOffer(null, 0));
        assertThrows(IllegalArgumentException.class, () -> model_controller.playerChoseOffer(first, -1));
        assertThrows(IllegalStateException.class, () -> model_controller.playerChoseOffer(second, 0));

        model_controller.playerChoseOffer(first, 0);
        OfferTile occupied = game_context.getGameTable().getOfferTrack().getTile(0);
        assertEquals(first, occupied.getOccupant());

        Color next = model_controller.getNextPlayerTurn();
        assertThrows(IllegalStateException.class, () -> model_controller.playerChoseOffer(next, 0));
    }

    @Test
    void connectionStatusAccessorsValidateAndDelegateTransitions() {
        Player player = firstPlayer();

        assertThrows(IllegalArgumentException.class,
                () -> model_controller.getPlayerConnectionStatus(null));
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.changePlayerConnectionStatus(null, ConnectionStatus.DISCONNECTED));
        assertThrows(IllegalArgumentException.class,
                () -> model_controller.changePlayerConnectionStatus(player.getColor(), null));

        assertEquals(ConnectionStatus.CONNECTED, model_controller.getPlayerConnectionStatus(player.getColor()));
        model_controller.changePlayerConnectionStatus(player.getColor(), ConnectionStatus.DISCONNECTED);
        assertEquals(ConnectionStatus.DISCONNECTED, model_controller.getPlayerConnectionStatus(player.getColor()));
        model_controller.changePlayerConnectionStatus(player.getColor(), ConnectionStatus.SKIPPED);
        assertEquals(ConnectionStatus.SKIPPED, model_controller.getPlayerConnectionStatus(player.getColor()));
        assertThrows(IllegalStateException.class,
                () -> model_controller.changePlayerConnectionStatus(player.getColor(), ConnectionStatus.CONNECTED));
    }

    @Test
    void restoreRejoinedPlayersPlacesTotemAndMarksThemConnected() {
        Player player = firstPlayer();
        int initialFood = player.getFood();
        game_context.getGameTable().getTurnOrderTile().clear();
        model_controller.changePlayerConnectionStatus(player.getColor(), ConnectionStatus.DISCONNECTED);
        model_controller.changePlayerConnectionStatus(player.getColor(), ConnectionStatus.SKIPPED);
        model_controller.changePlayerConnectionStatus(player.getColor(), ConnectionStatus.REJOINED);

        model_controller.restoreRejoinedPlayers();

        assertEquals(ConnectionStatus.CONNECTED, player.getConnectionStatus());
        assertEquals(List.of(player.getColor()), game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom());
        assertEquals(initialFood + 2, player.getFood());
    }

    @Test
    void playerSkipOfferValidatesTurnAndRemovesFirstTurnOrderTotem() {
        Color first = model_controller.getNextPlayerTurn();
        Color second = game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().get(1);

        assertThrows(IllegalArgumentException.class, () -> model_controller.playerSkipOffer(null));
        assertThrows(IllegalStateException.class, () -> model_controller.playerSkipOffer(second));

        model_controller.playerSkipOffer(first);

        assertFalse(game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().contains(first));
    }

    @Test
    void playerSkipActionValidatesFirstOccupiedOfferAndClearsIt() {
        Color first = firstPlayer().getColor();
        Color second = game_context.getPlayers().get(1).getColor();
        OfferTile firstTile = game_context.getGameTable().getOfferTrack().getTile(0);
        OfferTile secondTile = game_context.getGameTable().getOfferTrack().getTile(1);
        firstTile.occupy(first);
        secondTile.occupy(second);

        assertThrows(IllegalArgumentException.class, () -> model_controller.playerSkipAction(null));
        assertThrows(IllegalStateException.class, () -> model_controller.playerSkipAction(second));

        model_controller.playerSkipAction(first);

        assertTrue(firstTile.isAvailable());
        assertEquals(second, secondTile.getOccupant());
    }

    @Test
    void restoreSkippedPlayerValidatesColorAndAppliesTurnOrderOutcome() {
        Player player = firstPlayer();
        int initialFood = player.getFood();
        game_context.getGameTable().getTurnOrderTile().clear();

        assertThrows(IllegalArgumentException.class, () -> model_controller.restoreSkippedPlayer(null));

        model_controller.restoreSkippedPlayer(player.getColor());

        assertEquals(List.of(player.getColor()), game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom());
        assertEquals(initialFood + 2, player.getFood());
    }

    @Test
    void restoreSkippedPlayerClearsStaleOfferOccupancyBeforeRestoringFirstAvailableTurnOrderSlot() {
        Player restoredPlayer = firstPlayer();
        Player blockingPlayer = game_context.getPlayers().get(1);
        OfferTile staleOfferTile = game_context.getGameTable().getOfferTrack().getTile(0);

        game_context.getGameTable().getTurnOrderTile().clear();
        game_context.getGameTable().getTurnOrderTile().getSpaces().get(0).setOccupant(blockingPlayer.getColor());
        staleOfferTile.occupy(restoredPlayer.getColor());

        model_controller.restoreSkippedPlayer(restoredPlayer.getColor());

        assertTrue(staleOfferTile.isAvailable());
        assertEquals(
                List.of(blockingPlayer.getColor(), restoredPlayer.getColor()),
                game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom());
    }

    private Player firstPlayer() {
        return game_context.getPlayers().getFirst();
    }
}
