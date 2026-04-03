package it.polimi.ingsw.mesos.Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.BuildingDeck;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.GameTable;
import it.polimi.ingsw.mesos.Model.OfferTrack;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import it.polimi.ingsw.mesos.Model.TribeDeck;

import static it.polimi.ingsw.mesos.Globals.BUILDING_COUNT_BY_ERA;
import static it.polimi.ingsw.mesos.Globals.PLAYER_STARTING_FOOD;
import static java.util.Collections.shuffle;

public class GameSetup {

    private static final Map<Integer, List<Integer>> FOOD_BONUS_BY_PLAYER_COUNT = Map.of(
            2, List.of(1, -1),
            3, List.of(2, 0, -1),
            4, List.of(2, 1, 0, -1),
            5, List.of(3, 1, 0, 0, -1));

    public GameContext createGame(String game_name, int player_count, CardLoader.CardData card_data) {
        if (game_name == null || game_name.isBlank()) {
            throw new IllegalArgumentException("game_name cannot be null or blank");
        }
        if (player_count < Globals.MIN_NUM_PLAYERS || player_count > Globals.MAX_NUM_PLAYERS) {
            throw new IllegalArgumentException(
                    "player_count must be between " + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS);
        }
        if (card_data == null) {
            throw new IllegalArgumentException("cardData cannot be null");
        }
        // reverseEraOrder is true for tribe cards and false for building cards, because 
        // the way the card are taken from tribeDeck is : removeLast.
        List<TribeCard> tribe_cards = randomizeCardsWithinEra(
            selectTribeCards(card_data.tribe_cards(), player_count),
            true);
        List<BuildingCard> building_cards = selectBuildingCards(card_data.building_cards(), player_count);
        List<OfferTile> offer_tiles = selectOfferTiles(card_data.offer_tiles(), player_count);
        List<TurnOrderSpace> turn_order_spaces = createTurnOrderSpaces(player_count);

        TribeDeck tribe_deck = new TribeDeck(tribe_cards, player_count);
        BuildingDeck building_deck = new BuildingDeck(building_cards);
        OfferTrack offer_track = new OfferTrack(offer_tiles, player_count);
        TurnOrderTile turn_order_tile = new TurnOrderTile(turn_order_spaces);

        GameTable game_table = new GameTable(offer_track, turn_order_tile, building_deck, tribe_deck);

        return new GameContext(game_name, player_count, game_table);
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

        List<Color> shuffled_colors = new ArrayList<>(Arrays.asList(Color.values()));
        shuffle(shuffled_colors);

        Map<Color, Player> players_by_color = new LinkedHashMap<>();
        int[] starting_food = PLAYER_STARTING_FOOD();
        for (int i = 0; i < nicknames.size(); i++) {
            Color random_color = shuffled_colors.get(i);
            int initial_food = starting_food[i];
            Player player = new Player(nicknames.get(i), random_color, initial_food);
            players_by_color.put(random_color, player);
        }

        game_context.setupPlayers(players_by_color);
    }

    private List<TribeCard> selectTribeCards(List<TribeCard> all_cards, int player_count) {
        if (all_cards == null) {
            throw new IllegalArgumentException("tribeCards cannot be null");
        }

        return all_cards.stream()
                .filter(card -> card.getMinPlayers() <= player_count)
                .toList();
    }

    private List<BuildingCard> selectBuildingCards(List<BuildingCard> all_cards, int player_count) {
        if (all_cards == null) {
            throw new IllegalArgumentException("buildingCards cannot be null");
        }

        Map<Era, Integer> cards_per_era = BUILDING_COUNT_BY_ERA(player_count);
        // TODO: should we add these "impossible" exceptions?
        if (cards_per_era == null) {
            throw new IllegalArgumentException("Unsupported player count: " + player_count);
        }

        Map<Era, List<BuildingCard>> eligible_cards_by_era = new EnumMap<>(Era.class);
        for (Era era : Era.values()) {
            eligible_cards_by_era.put(era, new ArrayList<>());
        }

        for (BuildingCard card : all_cards) {
            if (card.getMinPlayers() <= player_count) {
                eligible_cards_by_era.get(card.getEra()).add(card);
            }
        }

        List<BuildingCard> selected_cards = new ArrayList<>();
        for (Era era : Era.values()) {
            int required_cards = cards_per_era.get(era);
            List<BuildingCard> era_cards = eligible_cards_by_era.get(era);
            // TODO: same as before
            if (era_cards.size() < required_cards) {
                throw new IllegalStateException(
                        "Not enough building cards for era " + era + ": required "
                                + required_cards + ", available " + era_cards.size());
            }

            shuffle(era_cards);
            selected_cards.addAll(era_cards.subList(0, required_cards));
        }

        return selected_cards;
    }

    private List<OfferTile> selectOfferTiles(List<OfferTile> all_tiles, int player_count) {
        if (all_tiles == null) {
            throw new IllegalArgumentException("offerTiles cannot be null");
        }

        return all_tiles.stream()
                .filter(tile -> tile.getMinNumPlayers() <= player_count)
                .toList();
    }

    private List<TurnOrderSpace> createTurnOrderSpaces(int player_count) {
        List<Integer> food_bonuses = FOOD_BONUS_BY_PLAYER_COUNT.get(player_count);
        if (food_bonuses == null) {
            throw new IllegalArgumentException("Unsupported player count: " + player_count);
        }

        List<TurnOrderSpace> spaces = new ArrayList<>();
        for (Integer food_bonus : food_bonuses) {
            spaces.add(new TurnOrderSpace(food_bonus));
        }
        return spaces;
    }

    static <T extends Card> List<T> randomizeCardsWithinEra(List<T> cards, boolean reverse_era_order) {
        if (cards == null) {
            throw new IllegalArgumentException("cards cannot be null");
        }

        Map<Era, List<T>> cards_by_era = new EnumMap<>(Era.class);
        for (Era era : Era.values()) {
            cards_by_era.put(era, new ArrayList<>());
        }

        for (T card : cards) {
            cards_by_era.get(card.getEra()).add(card);
        }

        for (List<T> era_cards : cards_by_era.values()) {
            shuffle(era_cards);
        }

        List<Era> era_order = new ArrayList<>(List.of(Era.values()));
        if (reverse_era_order) {
            Collections.reverse(era_order);
        }

        List<T> result = new ArrayList<>();
        for (Era era : era_order) {
            result.addAll(cards_by_era.get(era));
        }

        return result;
    }
}
