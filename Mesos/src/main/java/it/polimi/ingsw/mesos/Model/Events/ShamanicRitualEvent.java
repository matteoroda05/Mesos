package it.polimi.ingsw.mesos.Model.Events;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;

public class ShamanicRitualEvent extends EventCard {
    private final int winner_bonus;
    private final int loser_penalty;

    public ShamanicRitualEvent(Era era, int winner_bonus, int loser_penalty) {
        super(era);

        if (winner_bonus < 5 || winner_bonus > 15) throw new IllegalArgumentException("Winner bonus must be positive");
        if (loser_penalty < -7 || loser_penalty > -3) throw new IllegalArgumentException("Loser penalty must be negative");

        this.winner_bonus = winner_bonus;
        this.loser_penalty = loser_penalty;
    }

    @Override
    public List<Integer> calculateOutcome(List<Tribe> players_tribes) {
        List<Integer> player_stars = new ArrayList<>();
        List<Integer> player_outcomes = new ArrayList<>();

        int lowest_stars = Integer.MAX_VALUE;
        int highest_stars = Integer.MIN_VALUE;
        for (Tribe tribe : players_tribes) {
            int stars = tribe.countShamanStars();
            player_stars.add(stars);

            if (stars < lowest_stars) {
                lowest_stars = stars;
            } else if (stars > highest_stars) {
                highest_stars = stars;
            }
        }

        for (int stars : player_stars) {
            if (stars == highest_stars) {
                player_outcomes.add(winner_bonus);
            } else if (stars == lowest_stars) {
                player_outcomes.add(loser_penalty);
            } else {
                player_outcomes.add(0);
            }
        }

        return player_outcomes;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_SHAMANIC_RITUAL;
    }
}