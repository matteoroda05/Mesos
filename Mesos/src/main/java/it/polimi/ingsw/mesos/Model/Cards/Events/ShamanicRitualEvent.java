package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.EventPhase;
import it.polimi.ingsw.mesos.Model.Tribe;

public class ShamanicRitualEvent extends EventCard {
    private final int winner_bonus;
    private final int loser_penalty;

    @JsonCreator
    public ShamanicRitualEvent(
            @JsonProperty("era")          Era era,
            @JsonProperty("winnerBonus")  int winner_bonus,
            @JsonProperty("loserPenalty") int loser_penalty
    ) {
        super(era);

        if (winner_bonus < 5 || winner_bonus > 15) throw new IllegalArgumentException("Winner bonus must be positive");
        if (loser_penalty < -7 || loser_penalty > -3) throw new IllegalArgumentException("Loser penalty must be negative");

        this.winner_bonus = winner_bonus;
        this.loser_penalty = loser_penalty;
    }

    @Override
    public Map<Color, Integer> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, Integer> player_stars = new HashMap<>();
        Map<Color, Integer> player_outcomes = new HashMap<>();

        int lowest_stars = Integer.MAX_VALUE;
        int highest_stars = Integer.MIN_VALUE;
        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            int stars = tribe.getValue().countShamanStars();
            player_stars.put(tribe.getKey(), stars);

            if (stars < lowest_stars) {
                lowest_stars = stars;
            }
            if (stars > highest_stars) {
                highest_stars = stars;
            }
        }

        Map<Color, List<BuildingCard>> buildings = players_tribes.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getBuildingsWithTrigger(asTrigger()).stream()
                        .filter(building ->
                                ((BuildingShamanicRitual) building).getPhase() != EventPhase.PRE_EVENT).toList()
        ));

        buildings.forEach((key, list) -> list.forEach(building ->
                player_stars.replace(key, player_stars.get(key) + ((BuildingShamanicRitual) building).getStarBonus())));

        for (Map.Entry<Color, Integer> stars : player_stars.entrySet()) {
            if (stars.getValue() == highest_stars) {
                player_outcomes.put(stars.getKey(), winner_bonus);
            } else if (stars.getValue() == lowest_stars) {
                player_outcomes.put(stars.getKey(), loser_penalty);
            } else {
                player_outcomes.put(stars.getKey(), 0);
            }
        }

        return player_outcomes;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL;
    }
}