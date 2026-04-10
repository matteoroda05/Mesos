package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndTurn;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;

import java.util.List;
import java.util.Optional;

public class ActionState extends GameState {
    private int pick_top_row;
    private int pick_bottom_row;
    private Color p;
    private int food_gain;
    private boolean is_extra = false;

    public ActionState(ModelController controller) {
        super(controller);
        OfferTile tile = controller.getNextPlayerOfferTile();
        loadTile(tile);
    }

    public ActionState(ModelController controller, Color color, int extra_picks, boolean top_row) {
        super(controller);
        p = color;
        food_gain = 0;
        is_extra = true;
        if (top_row) pick_top_row = extra_picks;
        else pick_bottom_row = extra_picks;
    }

    /**
     * Handles the selection of one card by the specified player: checks that the request is valid (if not send specific
     * error message to the clients), activates interested buildings and adds card to the player's tribe
     * @param player_color: color of the player that is trying to pick a card
     * @param card: index of the requested card
     * @param is_top: row from which the player is picking
     * @return next game state
     */
    @Override
    public GameState chooseCard(Color player_color, int card, boolean is_top) {
        if (p != player_color) {
            // TODO: set error in model and ward clients
            return this;
        }

        Optional<CharacterCard> chosen;
        if (is_top && pick_top_row > 0) {
            chosen = controller.playerChooseCard(player_color, card, true);
            pick_top_row--;
        } else if (!is_top && pick_bottom_row > 0) {
            chosen = controller.playerChooseCard(player_color, card, false);
            pick_bottom_row--;
        } else {
            // TODO: set error in model to warn clients
            return this;
        }

        if (chosen.isPresent()) {
            List<BuildingOnCard> buildings = controller.getBuildingsWithTrigger(BuildingTrigger.ON_ACQUIRE_CHARACTER)
                    .stream().filter(building -> building.getOwner().getColor() == this.p).map(building -> ((BuildingOnCard) building)).toList();
            buildings.forEach(building -> building.effect(chosen.get(), controller));
        }

        if (hasFinished()) {
            return endTurn();
        }

        return this;
    }

    @Override
    public GameState chooseOffer(Color player_color, int offer_tile) {
        // TODO: set error in model to warn clients
        return this;
    }

    /**
     * If the current player chose the offer with the food then give the food to him and end is turn, otherwise do nothing
     * @return next state
     */
    @Override
    public GameState process() {
        if (food_gain != 0) {
            controller.playerAddFood(food_gain, p);
            food_gain = 0;
            return endTurn();
        }
        if (hasFinished()) {
            return endTurn();
        }
        return this;
    }

    /**
     * Checks if the player can take any cards and, if the offer consist of food, if it got the food already
     * @return true if the player has completed all the actions
     */
    private boolean hasFinished() {
        final int max_top_picks = controller.getAvailableCards(p, true);
        final int max_bottom_picks = controller.getAvailableCards(p, false);

        return (pick_bottom_row <= 0 || max_bottom_picks <= 0) && (pick_top_row <= 0 || max_top_picks <= 0) && food_gain == 0;
    }

    /**
     * Ends the turn of the player (activates buildings and position the totem to the turn order tile). The next state is
     * another player's turn or the resolution of events. If this is the last player then activate buildings for after everyone.
     * If this is already a bonus action then skips the standard end turn procedure.
     * @return new game state
     */
    private GameState endTurn() {
        if (!is_extra) {
            controller.playerEndTurn();

            List<BuildingEndTurn> buildings = controller.getBuildingsWithTrigger(BuildingTrigger.ON_END_TURN)
                    .stream().map(building -> ((BuildingEndTurn) building)).toList();
            TurnOrderTile tile = controller.getTurnOrderTile();
            buildings.forEach(building -> building.effect(tile, controller));
        }

        OfferTile next = controller.getNextPlayerOfferTile();
        if (next == null) {
            ActionState next_state = extras();
            if (next_state == null) {
                return new EventState(controller);
            }

            return next_state;
        }

        loadTile(next);
        return this;
    }

    /**
     * Handles the extra actions (granted by buildings) that take place after everyone has completed their turn
     * @return null if there are no extra actions, the state with the extra actions otherwise
     */
    private ActionState extras() {
        List<BuildingAfterEveryone> buildings = controller.getBuildingsWithTrigger(BuildingTrigger.ON_AFTER_EVERYONE)
                .stream().map(building -> ((BuildingAfterEveryone) building)).toList();

        for (BuildingAfterEveryone building : buildings) {
            ActionState next_state = building.effect(controller.getGameTable(), controller);
            if (next_state != null) {
                return next_state;
            }
        }

        // reset buildings active
        controller.getBuildingsWithTrigger(BuildingTrigger.ON_AFTER_EVERYONE).forEach(building -> ((BuildingAfterEveryone) building).reset());

        return null;
    }

    /**
     * Construct the state based on the contents of the tile
     * @param tile: offer tile to load
     */
    private void loadTile(OfferTile tile) {
        p = tile.getOccupant();
        pick_top_row = tile.getTopPicks();
        pick_bottom_row = tile.getBottomPicks();
        food_gain = tile.getFoodGain();
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.ROUND_ACTIONS;
    }

    @Override
    public Color getActivePlayer() {
        return p;
    }

    @Override
    public int getRemainingTopChoices() {
        return pick_top_row;
    }

    @Override
    public int getRemainingBottomChoices() {
        return pick_bottom_row;
    }
}
