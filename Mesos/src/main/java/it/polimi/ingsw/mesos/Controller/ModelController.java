package it.polimi.ingsw.mesos.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.Model.*;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import it.polimi.ingsw.mesos.Result;

public class ModelController {
    private final GameContext game_context;

    public ModelController(GameContext game_context) {
        if (game_context == null) throw new IllegalArgumentException("game_context cannot be null");
        this.game_context = game_context;
    }

    // TODO: add methods to get needed variables from GameContext and remove getGameContext()
    public GameContext getGameContext() {
        return this.game_context;
    }

    public List<EventCard> getEventsBottom() {
        return this.game_context.getGameTable().visibleEvents(false);
    }

    public List<EventCard> getAllEvents() {
        return this.game_context.getGameTable().visibleEvents(true);
    }

    /**
     * Counts the cards that a player can currently take from the selected
     * market row, considering the player's food and builder discounts.
     *
     * @param player_color color of the player whose options are counted
     * @param is_top true to inspect the top row, false for the bottom row
     * @return number of cards currently available to that player
     */
    public int getAvailableCards(Color player_color, boolean is_top) {
        Player player = game_context.getPlayer(player_color);
        MarketRow row = is_top
                ? game_context.getGameTable().getTopRow()
                : game_context.getGameTable().getBottomRow();

        return row.getTakeableCards(player.getFood(), player.getTribe().buildersDiscount()).size();
    }

    /**
     * Advances the model to the next round and, when the market refill reveals
     * a new era, performs the era transition as well.
     */
    public void endRound() {
        this.game_context.newRound();
        if (this.game_context.getGameTable().newRound()){
            this.game_context.getGameTable().beginNewEra();
            this.game_context.newEra();
        }
    }

    /**
     * Returns the offer tile belonging to the next player that must resolve
     * their action phase.
     *
     * @return the first occupied offer tile, or null when every offer has been
     *         resolved
     */
    public OfferTile getNextPlayerOfferTile() {
        List<OfferTile> tiles = game_context.getGameTable().getOfferTrack().getOccupiedTiles();

        if (tiles.isEmpty()) return null;
        else return tiles.getFirst();
    }

    /**
     * Returns the next player that must choose an offer tile according to the
     * current turn-order tile.
     *
     * @return the first color in turn order, or null when no player remains
     */
    public Color getNextPlayerTurn() {
        List<Color> tiles = game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom();

        if (tiles.isEmpty()) return null;
        else return tiles.getFirst();
    }

    /**
     * Builds a lookup of all player tribes keyed by player color.
     *
     * @return current tribes by color
     */
    public Map<Color, Tribe> getTribes() {
        return game_context.getPlayers().stream().collect(Collectors.toMap(
                Player::getColor,
                Player::getTribe
        ));
    }

    /**
     * Finds every owned building that reacts to the given trigger.
     *
     * @param trigger trigger to search for across all player tribes
     * @return matching buildings from all players
     */
    public List<BuildingCard> getBuildingsWithTrigger(BuildingTrigger trigger) {
        return game_context.getPlayers().stream()
                .map(Player::getTribe)
                .map(tribe -> tribe.getBuildingsWithTrigger(trigger))
                .flatMap(List::stream).toList();
    }

    public TurnOrderTile getTurnOrderTile() {
        return game_context.getGameTable().getTurnOrderTile();
    }

    public GameTable getGameTable() {
        return game_context.getGameTable();
    }

    // PLAYER ACTIONS
    /**
     * Ends the action phase for the player occupying the first offer tile:
     * clears that offer tile, places the player's totem back on the turn-order
     * tile, and applies the turn-order placement result.
     */
    public void playerEndTurn(){
        GameTable game_table = this.game_context.getGameTable();
        Color player_color = game_table.getOfferTrack().getOccupiedTiles().getFirst().clear();
        Result placing_result = game_table.getTurnOrderTile().placeTotem(player_color);
        playerApplyOutcome(placing_result, player_color);
    }

    /**
     * Resolves a player's card choice from a market row. Building choices are
     * checked against discounted cost and added to the tribe, while character
     * choices can trigger immediate character-specific effects.
     *
     * @param player_color color of the player choosing a card
     * @param card index in the selected market row
     * @param is_top true for the top row, false for the bottom row
     * @return the chosen character card when a character was taken, otherwise
     *         an empty optional for building purchases
     */
    public Optional<CharacterCard> playerChooseCard(Color player_color, int card, boolean is_top){
        if (card < 0) throw new IllegalArgumentException("card cannot be negative");
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player player = this.game_context.getPlayer(player_color);
        MarketRow market_row = is_top ? this.game_context.getGameTable().getTopRow() : this.game_context.getGameTable().getBottomRow();

        if (card >= market_row.getTribeCards().size()){
            // For building cards
            card -= market_row.getTribeCards().size();
            if (card > market_row.getBuildingCards().size()) throw new IllegalArgumentException("card is out of range");
            int cost = market_row.getBuildingCost(card) - player.getTribe().buildersDiscount();
            if (cost > player.getFood()) throw new IllegalArgumentException("player cannot afford this building");
            BuildingCard card_to_buy = market_row.takeBuildingCard(card);
            player.getTribe().addCard(card_to_buy);
            int check = player.loseFood(cost);
            if (check > 0) throw new IllegalStateException("player lost more food than than available");
        } else {
            // For character cards
            CharacterCard wanted;
            try {
                wanted = market_row.takeCharacterCard(card);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("player tried to take an event card");
            }

            Class<? extends CharacterCard> card_type = player.getTribe().addCard(wanted);
            if (card_type == HunterCard.class && ((HunterCard)wanted).isActiveHunter()) {
                player.gainFood(player.getTribe().getNumCharacterOfType(HunterCard.class));
            }
            return Optional.ofNullable(wanted);
        }

        return Optional.empty();
    }

    /**
     * Moves a player from the front of the turn-order tile to the selected
     * offer tile after validating that the player is allowed to choose now.
     *
     * @param player_color color of the player choosing an offer
     * @param offer_tile index of the selected offer tile
     */
    public void playerChoseOffer(Color player_color, int offer_tile){
        GameTable game_table = this.game_context.getGameTable();
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        if (offer_tile < 0) throw new IllegalArgumentException("offer_tile cannot be negative");
        if (! game_table.getTurnOrderTile().currentOrderTopToBottom().getFirst().equals(player_color)) {
            throw new IllegalStateException("Player " + player_color + " is not the first player in the turn order");
        }
        OfferTile chosen = game_table.getOfferTrack().getTile(offer_tile);
        if (!chosen.isAvailable()) throw new IllegalStateException("Offer tile " + offer_tile + " is not available");
        game_table.getTurnOrderTile().clearFirst();
        game_table.getOfferTrack().getTile(offer_tile).occupy(player_color);
    }

    /**
     * Grants food to a player.
     *
     * @param food_amount non-negative amount of food to add
     * @param player_color color of the player receiving food
     */
    public void playerAddFood(int food_amount, Color player_color) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        if (food_amount < 0) throw new IllegalArgumentException("food_amount cannot be negative");
        this.game_context.getPlayer(player_color).gainFood(food_amount);
    }

    /**
     * Applies a prestige-point change to a player through the model.
     *
     * @param prestige_amount points to add, or a negative value to subtract
     * @param player_color color of the player whose score changes
     */
    public void playerChangePrestige(int prestige_amount, Color player_color) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player p = this.game_context.getPlayer(player_color);
        p.changePrestigePoints(prestige_amount);
    }

    /**
     * Applies a generic event or tile outcome to a player, converting unpaid
     * food into prestige penalties when required.
     *
     * @param outcome result to apply
     * @param player_color color of the affected player
     */
    public void playerApplyOutcome(Result outcome, Color player_color) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player p = this.game_context.getPlayer(player_color);
        int penalty = 0;
        if (outcome.food() > 0){
            p.gainFood(outcome.food());
        } else if (outcome.food() < 0) {
            penalty = p.loseFood(- outcome.food()) * outcome.penalty();
        }
        if (outcome.prestige() + penalty != 0){
            p.changePrestigePoints(outcome.prestige() + penalty);
        }
    }
}
