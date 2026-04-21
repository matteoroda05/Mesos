package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionStateTest extends StateTestSupport {

    private ActionState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // Occupy an offer tile so ActionState constructor doesn't fail
        Color firstPlayer = player_colors.getFirst();
        // game_context.getGameTable().getOfferTrack().getTile(0).occupy(firstPlayer);
        state = new ActionState(model_controller);
    }

    @Test
    void getStatusReturnsRoundActions() {
        assertEquals(GameStatus.ROUND_ACTIONS, state.getStatus());
    }

    @Test
    void chooseCardInvalidPlayerReturnsSameState() {
        Color otherPlayer = player_colors.get(1);
        GameState nextState = state.chooseCard(otherPlayer, 0, true);
        assertSame(state, nextState);
    }

    @Test
    void chooseOfferReturnsSameState() {
        GameState nextState = state.chooseOffer(state.getActivePlayer(), 0);
        assertSame(state, nextState);
    }

    @Test
    void chooseCardDecrementsPicks() {
        // Offer tile 0 has some picks. Let's check.
        int initialTop = state.getRemainingTopChoices();
        int initialBottom = state.getRemainingBottomChoices();

        Color p = state.getActivePlayer();

        if (initialTop > 0) {
            state.chooseCard(p, 0, true);
            assertEquals(initialTop - 1, state.getRemainingTopChoices());
        } else if (initialBottom > 0) {
            state.chooseCard(p, 0, false);
            assertEquals(initialBottom - 1, state.getRemainingBottomChoices());
        }
    }

    @Test
    void processWithFoodGainAddsFoodAndTransitions() throws Exception {
        // Create custom offer tiles including one with food gain
        java.util.List<OfferTile> customTiles = new java.util.ArrayList<>();
        customTiles.add(new OfferTile('A', 0, 0, 3, 2));
        for (int i = 1; i < 5; i++) {
            customTiles.add(new OfferTile((char)('A' + i), 1, 1, 0, 2));
        }

        CardLoader.CardData baseData = CardLoader.load();
        CardLoader.CardData customData = new CardLoader.CardData(
                baseData.tribe_cards(), baseData.building_cards(), customTiles);

        it.polimi.ingsw.mesos.Controller.GameSetup setup = new it.polimi.ingsw.mesos.Controller.GameSetup();
        game_context = setup.createGame("CustomGame", 3, customData);
        setup.createMapOfPlayers(game_context, java.util.List.of("N", "M", "E"));
        model_controller = new it.polimi.ingsw.mesos.Controller.ModelController(game_context);
        player_colors = game_context.getPlayers().stream().map(Player::getColor).toList();
        game_context.getGameTable().getTurnOrderTile().clear();

        Color p = player_colors.getFirst();
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(p);

        int initialFood = game_context.getPlayer(p).getFood();

        state = new ActionState(model_controller);
        GameState nextState = state.process();

        // 3 food from offer tile + 2 food from turn order space (first space for 3 players)
        assertEquals(initialFood + 3 + 2, game_context.getPlayer(p).getFood());
        assertInstanceOf(EventState.class, nextState);
    }

    @Test
    void processTriggersExtrasWhenNoMorePlayers() throws Exception {
        // Create custom offer tiles with 0 picks to trigger endTurn immediately
        java.util.List<OfferTile> customTiles = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            customTiles.add(new OfferTile((char)('A' + i), 0, 0, 0, 2));
        }

        CardLoader.CardData baseData = CardLoader.load();
        CardLoader.CardData customData = new CardLoader.CardData(
                baseData.tribe_cards(), baseData.building_cards(), customTiles);

        it.polimi.ingsw.mesos.Controller.GameSetup setup = new it.polimi.ingsw.mesos.Controller.GameSetup();
        game_context = setup.createGame("CustomGame2", 3, customData);
        setup.createMapOfPlayers(game_context, java.util.List.of("N", "M", "E"));
        model_controller = new it.polimi.ingsw.mesos.Controller.ModelController(game_context);
        player_colors = game_context.getPlayers().stream().map(Player::getColor).toList();
        game_context.getGameTable().getTurnOrderTile().clear();

        Color p = player_colors.getFirst();
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(p);

        // Give the player a BuildingAfterEveryone
        BuildingAfterEveryone building =
                new BuildingAfterEveryone(
                        Era.FIRST, 2, 0, 0);
        building.setOwner(game_context.getPlayer(p));
        game_context.getPlayer(p).getTribe().addCard(building);

        state = new ActionState(model_controller);

        // Now call process
        GameState nextState = state.process();

        // It should be an ActionState (the extra one)
        assertInstanceOf(ActionState.class, nextState);
        ActionState extraState = (ActionState) nextState;
        assertEquals(p, extraState.getActivePlayer());
        assertEquals(1, extraState.getRemainingTopChoices());
        assertEquals(0, extraState.getRemainingBottomChoices());
    }
}
