package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameSetup;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndTurn;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ActionStateTest extends StateTestSupport {

    private ActionState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        Color firstPlayer = player_colors.getFirst();
        game_context.getGameTable().getTurnOrderTile().clear();
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(firstPlayer);
        state = new ActionState(model_controller);
    }

    @Test
    void getStatusReturnsRoundActions() {
        assertEquals(GameStatus.ROUND_ACTIONS, state.getStatus());
    }

    @Test
    void chooseCardInvalidPlayerThrows() {
        Color otherPlayer = player_colors.get(1);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> state.chooseCard(otherPlayer, 0, true)
        );

        assertTrue(exception.getMessage().contains("Wrong turn"));
    }

    @Test
    void chooseOfferThrowsBecauseWrongGameState() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> state.chooseOffer(state.getActivePlayer(), 0)
        );

        assertTrue(exception.getMessage().contains("wrong game state"));
    }

    @Test
    void chooseCardOnTopRow_withPicksRemaining_decrementsRemainingTopChoices() {
        Color player = player_colors.getFirst();
        ActionSpyController spy = new ActionSpyController(game_context, Optional.empty(), List.of());
        ActionState multiPick = new ActionState(spy, player, 2, true);

        multiPick.chooseCard(player, 0, true);

        assertEquals(1, multiPick.getRemainingTopChoices());
        assertEquals(0, multiPick.getRemainingBottomChoices());
    }

    @Test
    void processReturnsSameStateWhenPicksRemain() {
        assertSame(state, state.process());
    }

    @Test
    void constructorSkipsToEventsWhenNoOfferTileIsOccupied() {
        game_context.getGameTable().getOfferTrack().getTile(0).clear();

        ActionState skipped = new ActionState(model_controller);

        assertInstanceOf(EventState.class, skipped.process());
    }

    @Test
    void extraActionCanTargetBottomRow() {
        Color player = player_colors.getFirst();

        ActionState extra = new ActionState(model_controller, player, 2, false);

        assertEquals(player, extra.getActivePlayer());
        assertEquals(0, extra.getRemainingTopChoices());
        assertEquals(2, extra.getRemainingBottomChoices());
    }

    @Test
    void chooseCardRejectsRowsWithoutRemainingPicks() {
        Color player = player_colors.getFirst();
        ActionState topOnly = new ActionState(model_controller, player, 1, true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> topOnly.chooseCard(player, 0, false));

        assertTrue(exception.getMessage().contains("Wrong row"));
    }

    @Test
    void skipTurnRejectsWrongPlayerAndClearsStandardOfferAction() {
        Color activePlayer = state.getActivePlayer();
        Color otherPlayer = player_colors.stream().filter(color -> color != activePlayer).findFirst().orElseThrow();
        OfferTile occupiedTile = game_context.getGameTable().getOfferTrack().getTile(0);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> state.skipTurn(otherPlayer));

        assertTrue(exception.getMessage().contains("Wrong turn"));

        GameState nextState = state.skipTurn(activePlayer);

        assertInstanceOf(EventState.class, nextState);
        assertTrue(occupiedTile.isAvailable());
    }

    @Test
    void skipTurnOnExtraActionDoesNotClearCurrentOfferTile() {
        Color activePlayer = state.getActivePlayer();
        OfferTile occupiedTile = game_context.getGameTable().getOfferTrack().getTile(0);

        GameState nextState = new ActionState(model_controller, activePlayer, 1, true)
                .skipTurn(activePlayer);

        assertInstanceOf(ActionState.class, nextState);
        assertEquals(activePlayer, occupiedTile.getOccupant());
    }

    @Test
    void passRejectsWrongPlayer() {
        Color activePlayer = state.getActivePlayer();
        Color otherPlayer = player_colors.stream().filter(color -> color != activePlayer).findFirst().orElseThrow();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new ActionState(model_controller, activePlayer, 1, true).pass(otherPlayer));

        assertTrue(exception.getMessage().contains("Wrong turn"));
    }

    @Test
    void passRejectsNormalActionState() {
        Color activePlayer = state.getActivePlayer();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> state.pass(activePlayer));

        assertTrue(exception.getMessage().contains("normal action state"));
    }

    @Test
    void passRejectsNonOptionalExtraAction() {
        Color activePlayer = state.getActivePlayer();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> new ActionState(model_controller, activePlayer, 1, true).pass(activePlayer)
        );

        assertTrue(exception.getMessage().contains("normal action state"));
    }

    @Test
    void passSkipsOptionalAfterEveryoneExtraPickAndContinuesNormally() throws Exception {
        rebuildGameWithOfferTiles("OptionalPassFlow", noPickOfferTiles());

        Color player = player_colors.getFirst();
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(player);

        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.FIRST, 2, 0, 0);
        building.setOwner(game_context.getPlayer(player));
        game_context.getPlayer(player).getTribe().addCard(building);

        state = new ActionState(model_controller);
        GameState extraState = state.process();

        assertInstanceOf(ActionState.class, extraState);
        assertFalse(building.isActive());

        GameState nextState = extraState.pass(player);

        assertInstanceOf(EventState.class, nextState);
        assertTrue(building.isActive());
    }

    @Test
    void processWithFoodGainAddsFoodAndTransitions() throws Exception {
        rebuildGameWithOfferTiles("CustomGame", foodGainOfferTiles());

        Color p = player_colors.getFirst();
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(p);

        int initialFood = game_context.getPlayer(p).getFood();

        state = new ActionState(model_controller);
        GameState nextState = state.process();

        assertEquals(initialFood + 3 + 2, game_context.getPlayer(p).getFood());
        assertInstanceOf(EventState.class, nextState);
    }

    @Test
    void processTriggersExtrasWhenNoMorePlayers() throws Exception {
        rebuildGameWithOfferTiles("CustomGame2", noPickOfferTiles());

        Color p = player_colors.getFirst();
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(p);

        BuildingAfterEveryone building = new BuildingAfterEveryone(Era.FIRST, 2, 0, 0);
        building.setOwner(game_context.getPlayer(p));
        game_context.getPlayer(p).getTribe().addCard(building);

        state = new ActionState(model_controller);

        GameState nextState = state.process();

        assertInstanceOf(ActionState.class, nextState);
        ActionState extraState = (ActionState) nextState;
        assertEquals(p, extraState.getActivePlayer());
        assertEquals(1, extraState.getRemainingTopChoices());
        assertEquals(0, extraState.getRemainingBottomChoices());
    }

    @Test
    void processEndsStandardTurnRunsOnlyActivePlayerEndTurnBuildingsAndLoadsNextOfferTile() throws Exception {
        rebuildGameWithOfferTiles("StandardTurnLoadsNext", noPickOfferTiles());

        Color first = player_colors.get(0);
        Color second = player_colors.get(1);
        game_context.getGameTable().getOfferTrack().getTile(0).occupy(first);
        game_context.getGameTable().getOfferTrack().getTile(1).occupy(second);

        CountingEndTurnBuilding firstBuilding = new CountingEndTurnBuilding();
        firstBuilding.setOwner(game_context.getPlayer(first));
        game_context.getPlayer(first).getTribe().addCard(firstBuilding);

        CountingEndTurnBuilding secondBuilding = new CountingEndTurnBuilding();
        secondBuilding.setOwner(game_context.getPlayer(second));
        game_context.getPlayer(second).getTribe().addCard(secondBuilding);

        state = new ActionState(model_controller);
        GameState nextState = state.process();

        assertSame(state, nextState);
        assertEquals(second, state.getActivePlayer());
        assertEquals(1, firstBuilding.calls);
        assertEquals(0, secondBuilding.calls);
        assertTrue(game_context.getGameTable().getOfferTrack().getTile(0).isAvailable());
        assertEquals(first, game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom().getFirst());
    }

    @Test
    void extraTopCardChoiceTriggersOwnedOnCardBuildingAndFinishes() {
        Color player = player_colors.getFirst();
        CountingOnCardBuilding building = new CountingOnCardBuilding();
        building.setOwner(game_context.getPlayer(player));
        game_context.getPlayer(player).getTribe().addCard(building);
        ActionSpyController spy = new ActionSpyController(
                game_context,
                Optional.of(new HunterCard(Era.FIRST, 2, false)),
                List.of(building)
        );

        GameState nextState = new ActionState(spy, player, 1, true)
                .chooseCard(player, 0, true);

        assertInstanceOf(EventState.class, nextState);
        assertEquals(1, spy.chooseCardCalls);
        assertEquals(1, building.calls);
    }

    @Test
    void extraBottomBuildingChoiceDoesNotTriggerOnCardBuilding() {
        Color player = player_colors.getFirst();
        CountingOnCardBuilding building = new CountingOnCardBuilding();
        building.setOwner(game_context.getPlayer(player));
        ActionSpyController spy = new ActionSpyController(
                game_context,
                Optional.empty(),
                List.of(building)
        );

        GameState nextState = new ActionState(spy, player, 1, false)
                .chooseCard(player, 0, false);

        assertInstanceOf(EventState.class, nextState);
        assertEquals(1, spy.chooseCardCalls);
        assertEquals(0, building.calls);
    }

    @Test
    void bottomCharacterChoiceIgnoresOnCardBuildingsOwnedByOtherPlayers() {
        Color player = player_colors.getFirst();
        Color other = player_colors.get(1);
        CountingOnCardBuilding building = new CountingOnCardBuilding();
        building.setOwner(game_context.getPlayer(other));
        ActionSpyController spy = new ActionSpyController(
                game_context,
                Optional.of(new HunterCard(Era.FIRST, 2, false)),
                List.of(building)
        );

        GameState nextState = new ActionState(spy, player, 1, false)
                .chooseCard(player, 0, false);

        assertInstanceOf(EventState.class, nextState);
        assertEquals(1, spy.chooseCardCalls);
        assertEquals(0, building.calls);
    }

    private void rebuildGameWithOfferTiles(String gameName, List<OfferTile> offerTiles) throws IOException {
        CardLoader.CardData baseData = CardLoader.load();
        CardLoader.CardData customData = new CardLoader.CardData(
                baseData.tribe_cards(), baseData.building_cards(), offerTiles);

        GameSetup setup = new GameSetup();
        game_context = setup.createGame(gameName, 3, customData);
        setup.createMapOfPlayers(game_context, List.of("N", "M", "E"));
        model_controller = new ModelController(game_context);
        player_colors = game_context.getPlayers().stream().map(Player::getColor).toList();
        game_context.getGameTable().getTurnOrderTile().clear();
    }

    private static List<OfferTile> noPickOfferTiles() {
        List<OfferTile> tiles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tiles.add(new OfferTile((char) ('A' + i), 0, 0, 0, 2));
        }
        return tiles;
    }

    private static List<OfferTile> foodGainOfferTiles() {
        List<OfferTile> tiles = new ArrayList<>();
        tiles.add(new OfferTile('A', 0, 0, 3, 2));
        for (int i = 1; i < 5; i++) {
            tiles.add(new OfferTile((char) ('A' + i), 1, 1, 0, 2));
        }
        return tiles;
    }

    private static final class ActionSpyController extends ModelController {
        private final Optional<CharacterCard> chosenCard;
        private final List<BuildingCard> onCardBuildings;
        private int chooseCardCalls;

        private ActionSpyController(
                GameContext gameContext,
                Optional<CharacterCard> chosenCard,
                List<BuildingCard> onCardBuildings
        ) {
            super(gameContext);
            this.chosenCard = chosenCard;
            this.onCardBuildings = onCardBuildings;
        }

        @Override
        public Optional<CharacterCard> playerChooseCard(Color player_color, int card, boolean is_top) {
            chooseCardCalls++;
            return chosenCard;
        }

        @Override
        public int getAvailableCards(Color player_color, boolean is_top) {
            return 0;
        }

        @Override
        public OfferTile getNextPlayerOfferTile() {
            return null;
        }

        @Override
        public List<BuildingCard> getBuildingsWithTrigger(BuildingTrigger trigger) {
            if (trigger == BuildingTrigger.ON_ACQUIRE_CHARACTER) {
                return onCardBuildings;
            }
            return List.of();
        }
    }

    private static final class CountingOnCardBuilding extends BuildingOnCard {
        private int calls;

        private CountingOnCardBuilding() {
            super(Era.FIRST, 2, 0, 0, 1, true, false);
        }

        @Override
        public void effect(CharacterCard card, ModelController visitor) {
            calls++;
        }
    }

    private static final class CountingEndTurnBuilding extends BuildingEndTurn {
        private int calls;

        private CountingEndTurnBuilding() {
            super(Era.FIRST, 2, 0, 0, 1);
        }

        @Override
        public void effect(TurnOrderTile order, ModelController visitor) {
            calls++;
        }
    }
}
