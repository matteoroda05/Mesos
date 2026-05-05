package it.polimi.ingsw.mesos.Model.Cards.Events;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Result;

public class CavePaintingEvent extends EventCard {
    private final int threshold;
    private final int points_per_artist;
    private final int penalty;

    @JsonCreator
    public CavePaintingEvent(
            @JsonProperty("era")             Era era,
            @JsonProperty("threshold")       int threshold,
            @JsonProperty("pointsPerArtist") int points_per_artist,
            @JsonProperty("penalty")         int penalty
    ) {
        super(era);

        if (threshold < 1 || threshold > 3) throw new IllegalArgumentException("Threshold must be positive");
        if (points_per_artist < 1 || points_per_artist > 3) throw new IllegalArgumentException("Points per artist must be positive");
        if (penalty != -2) throw new IllegalArgumentException("Penalty must be negative");

        this.threshold = threshold;
        this.points_per_artist = points_per_artist;
        this.penalty = penalty;
    }

    /**
     * Awards prestige to tribes that meet the artist threshold and applies the
     * event penalty to tribes below it.
     *
     * @param players_tribes tribes keyed by player color
     * @return cave-painting outcome keyed by player color
     */
    @Override
    public Map<Color, Result> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, Result> player_outcomes = new HashMap<>();

        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            int artists = tribe.getValue().getNumCharacterOfType(ArtistCard.class);
            Result val;
            if (artists >= threshold) {
                val = new Result(0, points_per_artist * artists, 0);
            } else {
                val = new Result(0, penalty, 0);
            }
            player_outcomes.put(tribe.getKey(), val);
        }

        return player_outcomes;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getPointsPerArtist() {
        return points_per_artist;
    }

    public int getPenalty() {
        return penalty;
    }

    /**
     * Returns the building trigger used by cave-painting-event buildings.
     *
     * @return cave painting event trigger
     */
    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_CAVE_PAINTINGS;
    }
}
