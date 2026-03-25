package it.polimi.ingsw.mesos.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import it.polimi.ingsw.mesos.Model.*;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

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

    public void endRound() {
        this.game_context.newRound();
        if (this.game_context.getGameTable().newRound()){
            this.game_context.getGameTable().beginNewEra();
        }
    }

    public OfferTile getNextPlayerOfferTile() {
        List<OfferTile> tiles = game_context.getGameTable().getOfferTrack().getOccupiedTiles();

        if (tiles.isEmpty()) return null;
        else return tiles.getFirst();
    }

    public Color getNextPlayerTurn() {
        List<Color> tiles = game_context.getGameTable().getTurnOrderTile().currentOrderTopToBottom();

        if (tiles.isEmpty()) return null;
        else return tiles.getFirst();
    }

    public Map<Color, Tribe> getTribes() {
        return game_context.getPlayers().stream().collect(Collectors.toMap(
                Player::getColor,
                Player::getTribe
        ));
    }

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
    public void playerEndTurn(){
        GameTable game_table = this.game_context.getGameTable();
        Color player_color = game_table.getOfferTrack().getOccupiedTiles().getFirst().clear();
        Player player = this.game_context.getPlayer(player_color);
        game_table.getTurnOrderTile().placeTotem(player); // TODO: assign rewards (food)
    }

    public Optional<CharacterCard> playerChooseCard(Color player_color, int card, boolean is_top){
        if (card < 0) throw new IllegalArgumentException("card cannot be negative");
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player player = this.game_context.getPlayer(player_color);
        MarketRow market_row = is_top ? this.game_context.getGameTable().getTopRow() : this.game_context.getGameTable().getBottomRow();

        if (card > market_row.getTribeCards().size()){
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

            player.getTribe().addCard(wanted);
            return Optional.ofNullable(wanted);
        }

        return Optional.empty();
    }

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

    public void playerChangeFood(int food_amount, Color player_color) {
        if (food_amount < 0) throw new IllegalArgumentException("food_amount cannot be " +
                "negative for gain operations");
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player p = this.game_context.getPlayer(player_color);
        p.gainFood(food_amount);
    }

    public void playerChangeFood(int food_amount, Color player_color, int substitute_prestige) {
        if (food_amount > 0) throw new IllegalArgumentException("food_amount cannot be " +
                "positive for loss operations");
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player p = this.game_context.getPlayer(player_color);
        food_amount = -food_amount;
        int prestige_loss = substitute_prestige * p.loseFood(food_amount);
        if (prestige_loss != 0) {
            p.changePrestigePoints(- prestige_loss);
        }
    }

    public void playerChangePrestige(int prestige_amount, Color player_color) {
        if (player_color == null) throw new IllegalArgumentException("player_color cannot be null");
        Player p = this.game_context.getPlayer(player_color);
        p.changePrestigePoints(prestige_amount);
    }
}
