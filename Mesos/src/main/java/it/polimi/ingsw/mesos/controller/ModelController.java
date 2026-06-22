package it.polimi.ingsw.mesos.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.model.*;
import it.polimi.ingsw.mesos.model.cards.BuildingCard;
import it.polimi.ingsw.mesos.model.cards.CharacterCard;
import it.polimi.ingsw.mesos.model.cards.characters.HunterCard;
import it.polimi.ingsw.mesos.model.cards.EventCard;
import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.ConnectionStatus;
import it.polimi.ingsw.mesos.model.tiles.OfferTile;
import it.polimi.ingsw.mesos.model.tiles.TurnOrderTile;
import it.polimi.ingsw.mesos.dto.event.AppliedDelta;
import it.polimi.ingsw.mesos.dto.event.EventLogEntry;
import it.polimi.ingsw.mesos.dto.event.EventMessageFormatter;

/**
 * Provides state-machine operations over a {@link GameContext}, applying
 * player actions, transitions, scoring, and event-related model updates.
 */
public class ModelController {
    private final GameContext game_context;

    /**
     * Creates a model controller for one game context.
     *
     * @param game_context game context to control
     * @throws IllegalArgumentException if the context is null
     */
    public ModelController(GameContext game_context) {
        if (game_context == null) throw new IllegalArgumentException("game_context cannot be null");
        this.game_context = game_context;
    }

    // TODO: add methods to get needed variables from GameContext and remove getGameContext()
    /**
     * Returns the controlled game context.
     *
     * @return game context
     */
    public GameContext getGameContext() {
        return this.game_context;
    }

    /**
     * Returns visible event cards in the bottom market row.
     *
     * @return bottom-row events, or an empty list when none are visible
     */
    public List<EventCard> getEventsBottom() {
        return this.game_context.getGameTable().visibleEvents(false);
    }

    /**
     * Returns all visible event cards in both market rows.
     *
     * @return visible events from bottom and top rows, or an empty list when
     *         none are visible
     */
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
     * @throws IllegalArgumentException if the player color is null or not
     *                                  registered
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
     *
     * @throws IllegalStateException if the current era is already the last one
     *                               when a new era is required
     * @throws IllegalAccessError if the building deck is exhausted during an
     *                            era transition
     */
    public void endRound() {
        this.game_context.newRound();
        if (this.game_context.getGameTable().newRound(game_context.getCurrentEra())){
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
     * @throws IllegalStateException if players have not been registered
     */
    public Map<Color, Tribe> getTribes() {
        return game_context.getPlayers().stream().collect(Collectors.toMap(
                Player::getColor,
                Player::getTribe
        ));
    }

    /**
     * Returns a snapshot of each player's data for use by the network layer.
     *
     * @return player data in model order
     */
    public List<PlayerData> getPlayerData() {
        return game_context.getPlayers().stream()
                .map(p -> new PlayerData(
                        p.getNickname(),
                        p.getColor(),
                        p.getFood(),
                        p.getPrestigePoints(),
                        p.getTribe().getCharacterCards(),
                        p.getTribe().getBuildingCards(),
                        p.getTribe().countShamanStars(),
                        p.getTribe().buildersDiscount(),
                        p.getTribe().gatherersDiscount(),
                        p.getTribe().buildersEndGamePrestige(),
                        p.getTribe().buildingsPrestige(),
                        p.getTribe().inventionIcons().size()
                ))
                .toList();
    }

    /**
     * Finds every owned building that reacts to the given trigger.
     *
     * @param trigger trigger to search for across all player tribes
     * @return matching buildings from all players
     * @throws IllegalArgumentException if the trigger is null
     */
    public List<BuildingCard> getBuildingsWithTrigger(BuildingTrigger trigger) {
        return game_context.getPlayers().stream()
                .map(Player::getTribe)
                .map(tribe -> tribe.getBuildingsWithTrigger(trigger))
                .flatMap(List::stream).toList();
    }

    /**
     * Finds the buildings of the player with the specified trigger
     * @param c player color
     * @param trigger building trigger
     * @return list of buildings
     */
    public List<BuildingCard> getPlayerBuildings(Color c, BuildingTrigger trigger) {
        return game_context.getPlayer(c).getTribe().getBuildingsWithTrigger(trigger);
    }

    /**
     * Returns the turn-order tile for this game.
     *
     * @return turn-order tile
     */
    public TurnOrderTile getTurnOrderTile() {
        return game_context.getGameTable().getTurnOrderTile();
    }

    /**
     * Returns the game table for this game.
     *
     * @return game table
     */
    public GameTable getGameTable() {
        return game_context.getGameTable();
    }

    /**
     * Changes a player's connection status through the model.
     *
     * @param player_color player color
     * @param status new connection status
     * @throws IllegalArgumentException if the player color or status is null,
     *                                  or if the color is not registered
     * @throws IllegalStateException if the status transition is invalid
     */
    public void changePlayerConnectionStatus(Color player_color, ConnectionStatus status) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        game_context.getPlayer(player_color).setConnectionStatus(status);
    }

    /**
     * Reads a player's connection status.
     *
     * @param player_color player color
     * @return current connection status
     * @throws IllegalArgumentException if the player color is null or not
     *                                  registered
     */
    public ConnectionStatus getPlayerConnectionStatus(Color player_color) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        return game_context.getPlayer(player_color).getConnectionStatus();
    }

    /**
     * Restores every player marked as rejoined by placing their totem back on
     * the turn-order tile and marking them connected.
     *
     * @throws IllegalStateException if there is no free turn-order space for a
     *                               rejoined player
     */
    public void restoreRejoinedPlayers() {
        game_context.getPlayers().stream()
                .filter(player -> player.getConnectionStatus() == ConnectionStatus.REJOINED)
                .forEach(player -> {
                    restoreSkippedPlayer(player.getColor());
                    player.setConnectionStatus(ConnectionStatus.CONNECTED);
                });
    }

    /**
     * Ends the action phase for the player occupying the first offer tile:
     * clears that offer tile, places the player's totem back on the turn-order
     * tile, and applies the turn-order placement result.
     *
     * @throws java.util.NoSuchElementException if no occupied offer tile exists
     * @throws IllegalStateException if the turn-order tile has no free space
     */
    public void playerEndTurn(){
        GameTable game_table = this.game_context.getGameTable();
        Color player_color = game_table.getOfferTrack().getOccupiedTiles().getFirst().clear();
        Result placing_result = game_table.getTurnOrderTile().placeTotem(player_color);
        AppliedDelta applied = playerApplyOutcome(placing_result, player_color);
        this.game_context.logEventEntry(
                new EventLogEntry.TurnOrder(this.game_context.getPlayer(player_color).getNickname(),
                        player_color, applied.foodDelta(), applied.prestigeDelta()));
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
     * @throws IllegalArgumentException if the card index is negative, the
     *                                  player color is null or unknown, the
     *                                  index is out of range, the player takes
     *                                  an event card, or the player cannot
     *                                  afford the selected building
     * @throws IllegalStateException if a food payment reports an unpaid
     *                               remainder after affordability was checked
     */
    public Optional<CharacterCard> playerChooseCard(Color player_color, int card, boolean is_top){
        if (card < 0) throw new IllegalArgumentException("card cannot be negative");
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player player = this.game_context.getPlayer(player_color);
        MarketRow market_row = is_top ? this.game_context.getGameTable().getTopRow() : this.game_context.getGameTable().getBottomRow();

        if (card >= market_row.getTribeCards().size()){
            // For building cards
            card -= market_row.getTribeCards().size();
            if (card >= market_row.getBuildingCards().size()) throw new IllegalArgumentException("card is out of range");
            // Builder discounts can never reduce a building's cost below zero; clamp it so
            // that when the discount covers the full cost the building is taken for free.
            int cost = Math.max(0, market_row.getBuildingCost(card) - player.getTribe().buildersDiscount());
            if (cost > player.getFood()) throw new IllegalArgumentException("player cannot afford this building");
            BuildingCard card_to_buy = market_row.takeBuildingCard(card);
            player.getTribe().addCard(card_to_buy);
            card_to_buy.setOwner(player);
            if (cost > 0) {
                int check = player.loseFood(cost);
                if (check > 0) throw new IllegalStateException("player lost more food than than available");
                logNotice("Building purchased", "Player " + EventMessageFormatter.playerLabel(player.getNickname(), player_color) + " bought building "
                        + EventLogEntry.formatBuildingName(card_to_buy.getClass().getSimpleName())
                        + " losing " + cost + " food");
            }
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
                int gained = player.getTribe().getNumCharacterOfType(HunterCard.class);
                player.gainFood(gained);
                logNotice("Character effect", "Player " + EventMessageFormatter.playerLabel(player.getNickname(), player_color)
                        + " took an active Hunter, gaining " + gained + " food");
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
     * @throws IllegalArgumentException if the player color is null or the offer
     *                                  tile index is negative
     * @throws java.util.NoSuchElementException if the turn-order tile has no
     *                                          current first player
     * @throws IndexOutOfBoundsException if the offer tile index is outside the
     *                                   offer track
     * @throws IllegalStateException if the player is not first in turn order or
     *                               the offer tile is already occupied
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
     * Skips the offer choice for a disconnected player by removing it from the offer tile
     *
     * @param player_color color of the disconnected player
     * @throws IllegalArgumentException if the player color is null
     * @throws java.util.NoSuchElementException if the turn-order tile has no
     *                                          current first player
     * @throws IllegalStateException if the player is not first in turn order
     */
    public void playerSkipOffer(Color player_color) {
        GameTable game_table = this.game_context.getGameTable();
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        if (!game_table.getTurnOrderTile().currentOrderTopToBottom().getFirst().equals(player_color)) {
            throw new IllegalStateException("Player " + player_color + " is not the first player in the turn order");
        }
        game_table.getTurnOrderTile().clearFirst();
    }

    /**
     * Skips the action state for a (current) disconnected player by clearing
     * them from the offer track and not putting them back into the offer tile
     *
     * @param player_color color of the disconnected player
     * @throws IllegalArgumentException if the player color is null
     * @throws java.util.NoSuchElementException if no occupied offer tile exists
     * @throws IllegalStateException if the player is not first in offer-track
     *                               order
     */
    public void playerSkipAction(Color player_color){
        GameTable game_table = this.game_context.getGameTable();
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        if (!game_table.getOfferTrack().getOccupiedTiles().getFirst().getOccupant().equals(player_color)) {
            throw new IllegalStateException("Player " + player_color + " is not the first player in the offer ordetrack");
        }
        game_table.getOfferTrack().getOccupiedTiles().getFirst().clear();
    }

    /**
     * Restores a previously skipped player to the turn-order tile and applies
     * the placement outcome.
     *
     * @param player_color color of the player to restore
     * @throws IllegalArgumentException if the player color is null
     * @throws IllegalStateException if no free turn-order space exists
     */
    public void restoreSkippedPlayer(Color player_color) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        GameTable gameTable = this.game_context.getGameTable();
        gameTable.getOfferTrack().clearOccupantByColor(player_color);
        Result placing_result = gameTable.getTurnOrderTile().placeTotem(player_color);
        AppliedDelta applied = playerApplyOutcome(placing_result, player_color);
        this.game_context.logEventEntry(
                new EventLogEntry.TurnOrder(this.game_context.getPlayer(player_color).getNickname(),
                        player_color, applied.foodDelta(), applied.prestigeDelta()));
    }


    /**
     * Grants food to a player.
     *
     * @param food_amount non-negative amount of food to add
     * @param player_color color of the player receiving food
     * @throws IllegalArgumentException if the player color is null, the amount
     *                                  is negative, or the player is unknown
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
     * @throws IllegalArgumentException if the player color is null or unknown
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
     * @return the food and prestige change actually applied to the player
     * @throws IllegalArgumentException if the player color is null or unknown
     * @throws NullPointerException if the outcome is null
     */
    public AppliedDelta playerApplyOutcome(Result outcome, Color player_color) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player p = this.game_context.getPlayer(player_color);
        int food_before = p.getFood();
        int prestige_before = p.getPrestigePoints();
        int penalty = 0;
        if (outcome.food() > 0){
            p.gainFood(outcome.food());
        } else if (outcome.food() < 0) {
            penalty = p.loseFood(- outcome.food()) * outcome.penalty();
        }
        if (outcome.prestige() - penalty != 0){
            p.changePrestigePoints(outcome.prestige() - penalty);
        }
        return new AppliedDelta(p.getFood() - food_before, p.getPrestigePoints() - prestige_before);
    }

    /**
     * Runs an action that may change a player's resources and reports the food
     * and prestige actually applied, by reading model state before and after.
     *
     * @param color color of the player whose resources may change
     * @param action mutation to run
     * @return the food and prestige change applied to the player
     * @throws IllegalArgumentException if the color is null or unknown
     */
    public AppliedDelta captureDelta(Color color, Runnable action) {
        if (color == null) throw new IllegalArgumentException("color cannot be null");
        Player p = this.game_context.getPlayer(color);
        int food_before = p.getFood();
        int prestige_before = p.getPrestigePoints();
        action.run();
        return new AppliedDelta(p.getFood() - food_before, p.getPrestigePoints() - prestige_before);
    }

    /**
     * Records that a building was activated, describing its owner and the
     * food/prestige outcome (or a special action when there was no resource
     * change), so the event can be broadcast to clients.
     *
     * @param building building that was activated
     * @param delta food/prestige change applied by the activation
     * @param extra special-action description when there is no resource change,
     *              or null
     * @throws NullPointerException if the building has no owner
     */
    public void logBuildingActivation(BuildingCard building, AppliedDelta delta, String extra) {
        String name = EventLogEntry.formatBuildingName(building.getClass().getSimpleName());
        Player ownerPlayer = building.getOwner();
        String ownerSuffix = ownerPlayer == null ? ""
                : " from player " + EventMessageFormatter.playerLabel(ownerPlayer.getNickname(), ownerPlayer.getColor());
        StringBuilder body = new StringBuilder("Building " + name + ownerSuffix + " has been activated");
        if (delta.foodDelta() != 0 || delta.prestigeDelta() != 0) {
            body.append(", ").append(describeBuildingOutcome(delta));
        } else if (extra != null && !extra.isBlank()) {
            body.append(", ").append(extra);
        } else {
            body.append(" with no further effect");
        }
        this.game_context.logEventEntry(new EventLogEntry.Notice("Building activated", body.toString()));
    }

    /**
     * Records a free-form notification to be broadcast to clients.
     *
     * @param title heading of the notification
     * @param body descriptive body
     */
    public void logNotice(String title, String body) {
        this.game_context.logEventEntry(new EventLogEntry.Notice(title, body));
    }

    /**
     * Converts a building activation delta into a human-readable outcome.
     *
     * @param delta food and prestige changes caused by the activation
     * @return joined descriptions of the non-zero changes, or an empty string
     *         when neither value changed
     */
    private static String describeBuildingOutcome(AppliedDelta delta) {
        List<String> parts = new ArrayList<>();
        if (delta.foodDelta() != 0) {
            parts.add(buildingVerb(delta.foodDelta()) + " " + Math.abs(delta.foodDelta()) + " food");
        }
        if (delta.prestigeDelta() != 0) {
            parts.add(buildingVerb(delta.prestigeDelta()) + " " + Math.abs(delta.prestigeDelta()) + " prestige points");
        }
        return String.join(" and ", parts);
    }

    /**
     * Selects the wording used for a signed building effect.
     *
     * @param delta signed resource or prestige change
     * @return {@code "granting"} for positive changes, otherwise {@code "costing"}
     */
    private static String buildingVerb(int delta) {
        return delta > 0 ? "granting" : "costing";
    }
}
