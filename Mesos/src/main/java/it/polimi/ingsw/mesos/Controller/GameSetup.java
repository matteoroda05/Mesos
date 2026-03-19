package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.GameTable;
import it.polimi.ingsw.mesos.Model.OfferTrack;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import it.polimi.ingsw.mesos.Model.BuildingDeck;
import it.polimi.ingsw.mesos.Model.TribeDeck;
import it.polimi.ingsw.mesos.Globals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//TO DO. for multy game support we should implement the copy constructor for all the model classes,
//  and use it to create a new GameContext for each game, instead of sharing the same GameTable instance.
//  This way we can have multiple games running simultaneously without interference.
public class GameSetup {

    private static final Map<Integer, List<Integer>> FOOD_BONUS_BY_PLAYER_COUNT = Map.of(
            2, List.of(1, -1),
            3, List.of(2, 0, -1),
            4, List.of(2, 1, 0, -1),
            5, List.of(3, 1, 0, 0, -1));

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

        List<TribeCard> tribeCards = selectTribeCards(cardData.tribeCards(), playerCount);
        List<BuildingCard> buildingCards = selectBuildingCards(cardData.buildingCards(), playerCount);
        List<OfferTile> offerTiles = selectOfferTiles(cardData.offerTiles(), playerCount);
        List<TurnOrderSpace> turnOrderSpaces = createTurnOrderSpaces(playerCount);

        TribeDeck tribeDeck = new TribeDeck(tribeCards, playerCount);
        BuildingDeck buildingDeck = new BuildingDeck(buildingCards);
        OfferTrack offerTrack = new OfferTrack(offerTiles, playerCount);
        TurnOrderTile turnOrderTile = new TurnOrderTile(turnOrderSpaces);

        GameTable gameTable = new GameTable(offerTrack, turnOrderTile, buildingDeck, tribeDeck);

        return new GameContext(gameName, playerCount, gameTable);
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
}