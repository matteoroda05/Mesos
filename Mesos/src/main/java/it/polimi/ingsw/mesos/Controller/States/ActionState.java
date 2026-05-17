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
    private boolean skip = false;

    /**
     * Creates the action state for the next occupied offer tile.
     * If no occupied offer tile exists, the state marks itself for automatic
     * transition to event resolution.
     *
     * @param controller model controller used to read and apply game state
     * @throws NullPointerException if the controller is null
     */
    public ActionState(ModelController controller) {
        super(controller);
        OfferTile tile = controller.getNextPlayerOfferTile();
        if (tile != null) {
            loadTile(tile);
        } else {
            skip = true;
        }
    }

    /**
     * Creates an action state for bonus picks granted by a building effect.
     *
     * @param controller model controller used to apply the extra action
     * @param color player receiving the extra action
     * @param extra_picks number of extra card choices granted
     * @param top_row true when the extra choices apply to the top row
     * @implNote This constructor does not validate the player color or
     *           {@code extra_picks}; callers are expected to pass a valid
     *           player and a non-negative number of picks.
     */
    public ActionState(ModelController controller, Color color, int extra_picks, boolean top_row) {
        super(controller);
        p = color;
        food_gain = 0;
        is_extra = true;
        if (top_row) pick_top_row = extra_picks;
        else pick_bottom_row = extra_picks;
    }

    /**
     * Handles one card choice by the active player, applies any
     * on-character-acquired building effects, and advances when the action is
     * complete.
     *
     * @param player_color color of the player choosing a card
     * @param card index of the selected card in the chosen row
     * @param is_top true when selecting from the top market row
     * @return this state if more choices remain, otherwise the next state
     * @throws RuntimeException if the command is issued by a non-active player
     *                          or from a row with no remaining choices
     * @throws IllegalArgumentException if the model rejects the selected card
     */
    @Override
    public GameState chooseCard(Color player_color, int card, boolean is_top) {
        if (p != player_color) {
            throw new RuntimeException("Wrong turn: it's player " + p + "'s turn, not "  + player_color + "'s turn (dumbass)" );
        }

        Optional<CharacterCard> chosen;
        if (is_top && pick_top_row > 0) {
            chosen = controller.playerChooseCard(player_color, card, true);
            pick_top_row--;
        } else if (!is_top && pick_bottom_row > 0) {
            chosen = controller.playerChooseCard(player_color, card, false);
            pick_bottom_row--;
        } else {
            throw new RuntimeException("Wrong row: you tried picking from the wrong row");
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

    /**
     * Skips the active player's remaining actions and advances the action flow.
     * Non-extra actions also clear the player's offer tile.
     *
     * @param player_color color of the player whose action is skipped
     * @return next state after the skip is applied
     * @throws RuntimeException if the command is issued by a non-active player
     * @throws IllegalStateException if the model cannot skip the current offer
     *                               action
     */
    @Override
    public GameState skipTurn(Color player_color) {
        if (p != player_color) {
            throw new RuntimeException("Wrong turn: it's player " + p + "'s turn, not "  + player_color + "'s turn");
        }

        pick_top_row = 0;
        pick_bottom_row = 0;
        food_gain = 0;

        if (!is_extra) {
            controller.playerSkipAction(player_color);
        }
        return advance();
    }

    /**
     * Lets the active player decline an extra card pick granted after everyone
     * has taken their normal action.
     *
     * @param player_color color of the player passing the extra action
     * @return next state after the extra action is passed
     * @throws RuntimeException if the command is issued by a non-active player
     *                          or during a normal action state
     */
    @Override
    public GameState pass(Color player_color) {
        if (p != player_color) {
            throw new RuntimeException("Wrong turn: it's player " + p + "'s turn, not "  + player_color + "'s turn");
        }

        if (is_extra) {
            return advance();
        }

        throw new RuntimeException("Impossible to pass the turn: normal action state");
    }

    /**
     * Performs automatic action-state work.
     * Food-only offers grant their food immediately and end the turn; states
     * with no available remaining choices also end the turn.
     *
     * @return this state when player input is still needed, otherwise the next
     *         state
     */
    @Override
    public GameState process() {
        if (skip) {
            return new EventState(controller);
        }

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
     * Checks whether the active player has no usable choices left and has
     * already received any pending food gain.
     *
     * @return true if the player has completed all actions
     * @throws IllegalArgumentException if the active player is null or not
     *                                  registered
     */
    private boolean hasFinished() {
        final int max_top_picks = controller.getAvailableCards(p, true);
        final int max_bottom_picks = controller.getAvailableCards(p, false);

        return (pick_bottom_row <= 0 || max_bottom_picks <= 0) && (pick_top_row <= 0 || max_top_picks <= 0) && food_gain == 0;
    }

    /**
     * Ends the active player's action.
     * Standard actions return the player's totem and apply end-turn buildings;
     * extra actions skip that standard end-turn procedure.
     *
     * @return next action, extra-action, or event-resolution state
     */
    private GameState endTurn() {
        if (!is_extra) {
            controller.playerEndTurn();
            applyEndTurnBuildings();
        }

        return advance();
    }

    /**
     * Applies active-player buildings that react to the end-turn trigger.
     *
     * @throws IllegalStateException if the active player has not returned to
     *                               the turn-order tile
     */
    private void applyEndTurnBuildings(){
        List<BuildingEndTurn> buildings = controller.getBuildingsWithTrigger(BuildingTrigger.ON_END_TURN)
                .stream()
                .filter(building -> building.getOwner().getColor() == this.p)
                .map(building -> ((BuildingEndTurn) building))
                .toList();
        TurnOrderTile tile = controller.getTurnOrderTile();
        buildings.forEach(building -> building.effect(tile, controller));
    }

    /**
     * Loads the next occupied offer tile, starts extra actions after all offers
     * are resolved, or advances to event resolution when no extra action exists.
     *
     * @return this state when another normal action starts, an extra-action
     *         state, or an event-resolution state
     */
    private GameState advance() {
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
     * Finds the first available extra action granted by after-everyone
     * buildings.
     * When no extra action is available, all such buildings are reset for the
     * next round.
     *
     * @return null if there are no extra actions, otherwise the state that
     *         handles the extra action
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
     * Configures this state from an occupied offer tile.
     *
     * @param tile offer tile to load
     * @throws NullPointerException if the tile is null
     */
    private void loadTile(OfferTile tile) {
        p = tile.getOccupant();
        pick_top_row = tile.getTopPicks();
        pick_bottom_row = tile.getBottomPicks();
        food_gain = tile.getFoodGain();
    }

    /**
     * Reports that the game is waiting for card choices or automatic action
     * resolution.
     *
     * @return round-actions status
     */
    @Override
    public GameStatus getStatus() {
        return GameStatus.ROUND_ACTIONS;
    }

    /**
     * Returns the player currently resolving an action.
     *
     * @return active player color, or null when no offer tile is loaded
     */
    @Override
    public Color getActivePlayer() {
        return p;
    }

    /**
     * Returns the active player's remaining top-row choices.
     *
     * @return remaining top-row choices
     */
    @Override
    public int getRemainingTopChoices() {
        return pick_top_row;
    }

    /**
     * Returns the active player's remaining bottom-row choices.
     *
     * @return remaining bottom-row choices
     */
    @Override
    public int getRemainingBottomChoices() {
        return pick_bottom_row;
    }
}
