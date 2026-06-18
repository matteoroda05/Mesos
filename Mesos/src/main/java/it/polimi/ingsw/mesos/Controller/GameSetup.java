package it.polimi.ingsw.mesos.Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Model.BuildingDeck;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
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

    /**
     * Creates the static game model for a new match by selecting the cards and
     * tiles valid for the player count, creating the decks, and wiring the game
     * table.
     *
     * @param game_name name of the game
     * @param player_count number of players in the match
     * @param card_data loaded card and tile definitions
     * @return initialized game context without players assigned yet
     * @throws IllegalArgumentException if the game name is null or blank, the
     *                                  player count is outside the supported
     *                                  range, card data is null, or a required
     *                                  card/tile list is null
     * @throws IllegalStateException if there are not enough eligible building
     *                               cards for one era
     */
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

        List<TribeCard> tribe_cards = selectTribeCards(card_data.tribe_cards(), player_count);
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

    /**
     * Creates players from nicknames, assigns random colors and starting food,
     * then registers them in the game context.
     *
     * @param game_context game context to populate
     * @param nicknames player nicknames in seating order
     * @throws IllegalArgumentException if the context or nickname list is null,
     *                                  the nickname count does not match the
     *                                  game size, or any nickname is null or
     *                                  blank
     */
    public void createMapOfPlayers(GameContext game_context, List<String> nicknames) {
        createMapOfPlayers(game_context, nicknames, Map.of());
    }

    /**
     * Creates players from nicknames, honoring any reserved colors first and
     * assigning unique fallback colors to the remaining players. The initial
     * turn order is randomized, and starting food is assigned by each player's
     * resulting turn-order position.
     *
     * @param game_context game context to populate
     * @param nicknames player nicknames (join order; the turn order is randomized)
     * @param reservedColors reserved colors keyed by nickname
     * @throws IllegalArgumentException if the context, nickname list, or
     *                                  reserved color map is invalid, if the
     *                                  nickname count does not match the game
     *                                  size, or if reserved colors are
     *                                  duplicated or assigned to unknown
     *                                  nicknames
     */
    public void createMapOfPlayers(GameContext game_context, List<String> nicknames, Map<String, Color> reservedColors) {
        if (nicknames == null) {
            throw new IllegalArgumentException("nickname cannot be null");
        }
        if(game_context == null) {
            throw new IllegalArgumentException("game_context cannot be null");
        }
        if (reservedColors == null) {
            throw new IllegalArgumentException("reservedColors cannot be null");
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

        List<Color> usedColors = new ArrayList<>();
        for (Map.Entry<String, Color> reservedEntry : reservedColors.entrySet()) {
            String reservedNickname = reservedEntry.getKey();
            Color reservedColor = reservedEntry.getValue();
            if (reservedNickname == null || reservedNickname.isBlank()) {
                throw new IllegalArgumentException("reserved nickname cannot be null or blank");
            }
            if (reservedColor == null) {
                throw new IllegalArgumentException("reserved color cannot be null");
            }
            if (!nicknames.contains(reservedNickname)) {
                throw new IllegalArgumentException("reserved color assigned to unknown nickname");
            }
            if (usedColors.contains(reservedColor)) {
                throw new IllegalArgumentException("reserved colors must be unique");
            }
            usedColors.add(reservedColor);
        }

        List<Color> availableColors = new ArrayList<>(Arrays.asList(Color.values()));
        availableColors.removeAll(usedColors);
        shuffle(availableColors);

        // Randomize the initial turn order (rule setup step 9): the iteration order of this
        // map is the order in which totems are placed on the turn-order tile, and starting
        // food is then assigned by that random position (step 10).
        List<String> seatingOrder = new ArrayList<>(nicknames);
        shuffle(seatingOrder);

        Map<Color, Player> players_by_color = new LinkedHashMap<>();
        int[] starting_food = PLAYER_STARTING_FOOD();
        int fallbackColorIndex = 0;
        for (int seat = 0; seat < seatingOrder.size(); seat++) {
            String nickname = seatingOrder.get(seat);
            Color assignedColor = reservedColors.get(nickname);
            if (assignedColor == null) {
                assignedColor = availableColors.get(fallbackColorIndex);
                fallbackColorIndex++;
            }
            int initial_food = starting_food[seat];
            Player player = new Player(nickname, assignedColor, initial_food);
            players_by_color.put(assignedColor, player);
        }

        game_context.setupPlayers(players_by_color);
    }

    /**
     * Selects and shuffles the required number of building cards for each era,
     * considering the minimum-player requirement of each card.
     *
     * @param all_cards complete building-card pool
     * @param player_count number of players in the match
     * @return selected building cards grouped by era order
     * @throws IllegalArgumentException if the card pool is null or the player
     *                                  count is unsupported
     * @throws NullPointerException if the card pool contains null entries
     * @throws IllegalStateException if an era does not have enough eligible
     *                               building cards
     */
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

    /**
     * Keeps only offer tiles that are valid for the current player count.
     *
     * @param all_tiles complete offer-tile pool
     * @param player_count number of players in the match
     * @return offer tiles usable in this match
     * @throws IllegalArgumentException if the tile pool is null
     * @throws NullPointerException if the tile pool contains null entries
     */
    private List<OfferTile> selectOfferTiles(List<OfferTile> all_tiles, int player_count) {
        if (all_tiles == null) {
            throw new IllegalArgumentException("offerTiles cannot be null");
        }

        return all_tiles.stream()
                .filter(tile -> tile.getMinNumPlayers() <= player_count)
                .map(tile -> new OfferTile(
                        tile.getOfferTileId(),
                        tile.getTopPicks(),
                        tile.getBottomPicks(),
                        tile.getFoodGain(),
                        tile.getMinNumPlayers()
                ))
                .toList();
    }

    /**
     * Creates the turn-order spaces and their food bonuses for the configured
     * player count.
     *
     * @param player_count number of players in the match
     * @return turn-order spaces used by the game table
     * @throws IllegalArgumentException if the player count is unsupported
     */
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

    /**
     * Builds the tribe deck for the match by filtering invalid cards, shuffling
     * cards within each era, and placing final events so they are drawn last.
     *
     * @param cards complete tribe-card pool
     * @param player_count number of players in the match
     * @return ordered tribe deck compatible with {@link TribeDeck#drawTopRow()}
     * @throws IllegalArgumentException if the card pool is null
     * @throws NullPointerException if the card pool contains null entries
     */
    static List<TribeCard> selectTribeCards(List<TribeCard> cards, int player_count) {
        if (cards == null) {
            throw new IllegalArgumentException("cards cannot be null");
        }

        Map<Era, List<TribeCard>> cards_by_era = new EnumMap<>(Era.class);
        for (Era era : Era.values()) {
            cards_by_era.put(era, new ArrayList<>());
        }

        List<TribeCard> final_events = new ArrayList<>();
        for (TribeCard card : cards) {
            if (card.getMinPlayers() <= player_count){
                if (isFinalEvent(card)) {
                    final_events.add(card);
                } else {
                    cards_by_era.get(card.getEra()).add(card);
                }
            }
        }

        for (List<TribeCard> era_cards : cards_by_era.values()) {
            shuffle(era_cards);
        }


        shuffle(final_events);
        List<TribeCard> result = new ArrayList<>(final_events);
        // The list is reversed because TribeDeck uses "removeLast"
        for (Era era : List.of(Era.THIRD, Era.SECOND, Era.FIRST)) {
            result.addAll(cards_by_era.get(era));
        }

        return result;
    }

    /**
     * Detects final third-era events that must be kept at the end of the tribe
     * deck.
     *
     * @param card card to inspect
     * @return true when the card is a final event
     * @throws NullPointerException if the card is null
     */
    private static boolean isFinalEvent(Card card) {
        return card.isEvent() &&
                card.getEra() == Era.THIRD &&
                (((EventCard) card).asTrigger() == BuildingTrigger.ON_EVENT_SUSTENANCE ||
                        ((EventCard) card).asTrigger() == BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL);
    }
}
