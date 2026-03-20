package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.GameTable;
import it.polimi.ingsw.mesos.Model.OfferTrack;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import it.polimi.ingsw.mesos.Model.BuildingDeck;
import it.polimi.ingsw.mesos.Model.TribeDeck;
import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameSetup {

    private static final Map<Integer, List<Integer>> FOOD_BONUS_BY_PLAYER_COUNT = Map.of(
            2, List.of(1, -1),
            3, List.of(2, 0, -1),
            4, List.of(2, 1, 0, -1),
            5, List.of(3, 1, 0, 0, -1));
    private static final List<Integer> INITIAL_FOOD_BY_PLAYER_ORDER = List.of(2, 3, 3, 4, 5);

    public GameContext createGame(String gameName, int playerCount, CardLoader.CardData cardData) {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("gameName cannot be null or blank");
        }
        if (playerCount < Globals.MIN_NUM_PLAYERS || playerCount > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "playerCount must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
        if (cardData == null) {
            throw new IllegalArgumentException("cardData cannot be null");
        }
        // reverseEraOrder is true for tribe cards and false for building cards, because 
        // the way the card are taken from tribeDeck is : removeLast.
        List<TribeCard> tribeCards = randomizeCardsWithinEra(
            selectTribeCards(cardData.tribeCards(), playerCount),
            true);
        List<BuildingCard> buildingCards = randomizeCardsWithinEra(
            selectBuildingCards(cardData.buildingCards(), playerCount),
            false);
        List<OfferTile> offerTiles = selectOfferTiles(cardData.offerTiles(), playerCount);
        List<TurnOrderSpace> turnOrderSpaces = createTurnOrderSpaces(playerCount);

        TribeDeck tribeDeck = new TribeDeck(tribeCards, playerCount);
        BuildingDeck buildingDeck = new BuildingDeck(buildingCards);
        OfferTrack offerTrack = new OfferTrack(offerTiles, playerCount);
        TurnOrderTile turnOrderTile = new TurnOrderTile(turnOrderSpaces);

        GameTable gameTable = new GameTable(offerTrack, turnOrderTile, buildingDeck, tribeDeck);

        return new GameContext(gameName, playerCount, gameTable);
    }

    public void createMapOfPlayers(GameContext game_context, List<String> nicknames) {
        if (nicknames == null) {
            throw new IllegalArgumentException("nickname cannot be null");
        }
        if(game_context == null) {
            throw new IllegalArgumentException("game_context cannot be null");
        }
        if(nicknames.size() != game_context.getGameSize()) {
            throw new IllegalArgumentException(
                    "Number of nicknames must be equal to " + game_context.getGameSize() + ".");
        }

        for (String nickname : nicknames) {
            if (nickname == null || nickname.isBlank()) {
                throw new IllegalArgumentException("nickname cannot be null or blank");
            }
        }

        List<Color> shuffledColors = new ArrayList<>(Arrays.asList(Color.values()));
        Collections.shuffle(shuffledColors);

        Map<Color, Player> playersByColor = new EnumMap<>(Color.class);
        for (int i = 0; i < nicknames.size(); i++) {
            Color randomColor = shuffledColors.get(i);
            int initialFood = INITIAL_FOOD_BY_PLAYER_ORDER.get(i);
            Player player = new Player(nicknames.get(i), randomColor, initialFood);
            playersByColor.put(randomColor, player);
        }

        game_context.setupPlayers(playersByColor);
    }

    private List<TribeCard> selectTribeCards(List<TribeCard> allCards, int playerCount) {
        if (allCards == null) {
            throw new IllegalArgumentException("tribeCards cannot be null");
        }

        return allCards.stream()
                .filter(card -> card.getMinPlayers() <= playerCount)
                .toList();
    }

    private List<BuildingCard> selectBuildingCards(List<BuildingCard> allCards, int playerCount) {
        if (allCards == null) {
            throw new IllegalArgumentException("buildingCards cannot be null");
        }

        return allCards.stream()
                .filter(card -> card.getMinPlayers() <= playerCount)
                .toList();
    }

    private List<OfferTile> selectOfferTiles(List<OfferTile> allTiles, int playerCount) {
        if (allTiles == null) {
            throw new IllegalArgumentException("offerTiles cannot be null");
        }

        return allTiles.stream()
                .filter(tile -> tile.getMinNumPlayers() <= playerCount)
                .toList();
    }

    private List<TurnOrderSpace> createTurnOrderSpaces(int playerCount) {
        List<Integer> foodBonuses = FOOD_BONUS_BY_PLAYER_COUNT.get(playerCount);
        if (foodBonuses == null) {
            throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        }

        List<TurnOrderSpace> spaces = new ArrayList<>();
        for (int i = 0; i < foodBonuses.size(); i++) {
            spaces.add(new TurnOrderSpace(foodBonuses.get(i)));
        }
        return spaces;
    }

    static <T extends Card> List<T> randomizeCardsWithinEra(List<T> cards, boolean reverseEraOrder) {
        if (cards == null) {
            throw new IllegalArgumentException("cards cannot be null");
        }

        Map<Era, List<T>> cardsByEra = new EnumMap<>(Era.class);
        for (Era era : Era.values()) {
            cardsByEra.put(era, new ArrayList<>());
        }

        for (T card : cards) {
            cardsByEra.get(card.getEra()).add(card);
        }

        for (List<T> eraCards : cardsByEra.values()) {
            Collections.shuffle(eraCards);
        }

        List<Era> eraOrder = new ArrayList<>(List.of(Era.values()));
        if (reverseEraOrder) {
            Collections.reverse(eraOrder);
        }

        List<T> result = new ArrayList<>();
        for (Era era : eraOrder) {
            result.addAll(cardsByEra.get(era));
        }

        return result;
    }
}