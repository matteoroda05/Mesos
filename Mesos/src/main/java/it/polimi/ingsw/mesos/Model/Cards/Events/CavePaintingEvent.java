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

    @Override
    public Map<Color, Integer> calculateOutcome(Map<Color, Tribe> players_tribes) {
        Map<Color, Integer> player_outcomes = new HashMap<>();

        for (Map.Entry<Color, Tribe> tribe : players_tribes.entrySet()) {
            int artists = tribe.getValue().getNumCharacterOfType(ArtistCard.class);
            if (artists >= threshold) {
                player_outcomes.put(tribe.getKey(), points_per_artist * artists);
            } else {
                player_outcomes.put(tribe.getKey(), penalty);
            }
        }

        return player_outcomes;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_CAVE_PAINTINGS;
    }
}