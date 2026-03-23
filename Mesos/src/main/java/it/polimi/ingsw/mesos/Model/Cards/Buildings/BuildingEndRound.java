package it.polimi.ingsw.mesos.Model.Cards.Buildings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameTable;

public class BuildingEndRound extends BuildingCard {
    @JsonCreator
    public BuildingEndRound(
            @JsonProperty("era")            Era era,
            @JsonProperty("minPlayers")     int min_players,
            @JsonProperty("foodCost")       int food_cost,
            @JsonProperty("prestigePoints") int prestige_points
    ) {
        super(era, min_players, food_cost, prestige_points, BuildingTrigger.ON_END_ROUND);
    }

    // TODO: add visitor that can allow extra card pick
    public void effect(GameTable table) {
        final boolean cards_available = !table.getTopRow()
                .getTakeableCards(owner.getFood(), owner.getTribe().buildersDiscount()).isEmpty();
        if (cards_available) {}
    }
}
