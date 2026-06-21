package it.polimi.ingsw.mesos.model.cards.buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.controller.ModelController;
import it.polimi.ingsw.mesos.controller.states.ActionState;
import it.polimi.ingsw.mesos.model.cards.BuildingCard;
import it.polimi.ingsw.mesos.model.cards.CardDescriptionVisitor;
import it.polimi.ingsw.mesos.model.enums.BuildingTrigger;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.GameTable;

/**
 * Building card that grants its owner one optional extra top-row pick after
 * every player has completed a normal action.
 */
public class BuildingAfterEveryone extends BuildingCard {
    private boolean to_apply = true;

    /**
     * Creates an after-everyone building from JSON data.
     *
     * @param era era this building belongs to
     * @param min_players minimum players required to use this building
     * @param food_cost printed food cost
     * @param prestige_points printed prestige points
     * @throws IllegalArgumentException if the base card data is invalid
     */
    @JsonCreator
    public BuildingAfterEveryone(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_AFTER_EVERYONE);
    }

    // TODO: add visitor that can allow extra card pick
    /**
     * Consumes this once-per-round building effect and, when the owner can take
     * at least one top-row card, creates an action state for the extra pick.
     * The caller is responsible for checking {@link #isActive()} before
     * invoking this method.
     *
     * @param table game table used to inspect top-row availability
     * @param controller controller that will drive the generated action state
     * @return an action state for the extra pick, or null when no card can be
     *         taken
     * @throws NullPointerException if the owner, table, or controller is null
     */
    public ActionState effect(GameTable table, ModelController controller) {
        to_apply = false;  // effect applied this round, waits reset for new round

        final boolean cards_available = !table.getTopRow()
                .getTakeableCards(owner.getFood(), owner.getTribe().buildersDiscount()).isEmpty();
        if (cards_available) {
            return new ActionState(controller, owner.getColor(), 1, true, true);
        }
        return null;
    }

    /**
     * Checks whether this once-per-round effect is still available.
     *
     * @return true before the effect has been consumed for the round
     */
    public boolean isActive() {
        return to_apply;
    }

    /**
     * Reactivates this building effect for a new round.
     */
    public void reset() {
        to_apply = true;
    }

    /**
     * Delegates this extra-action building's description to the supplied visitor.
     *
     * @param visitor visitor that describes after-everyone buildings
     * @return visitor-provided building description
     */
    @Override
    public String accept(CardDescriptionVisitor visitor) {
        return visitor.describeBuildingAfterEveryone(this);
    }

    /**
     * Returns the display name of this building type.
     *
     * @return {@code "Building After Everyone"}
     */
    @Override
    public String getReadableName() {
        return "Building After Everyone";
    }
}
