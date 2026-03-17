package it.polimi.ingsw.mesos.Model.Events;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tribe;

public class CavePaintingEvent extends EventCard {
    private final int threshold;
    private final int points_per_artist;
    private final int penalty;

    public CavePaintingEvent(Era era, int threshold, int points_per_artist, int penalty) {
        super(era);

        if (threshold < 1 || threshold > 3) throw new IllegalArgumentException("Threshold must be positive");
        if (points_per_artist < 1 || points_per_artist > 3) throw new IllegalArgumentException("Points per artist must be positive");
        if (penalty != -2) throw new IllegalArgumentException("Penalty must be negative");

        this.threshold = threshold;
        this.points_per_artist = points_per_artist;
        this.penalty = penalty;
    }

    @Override
    public List<Integer> calculateOutcome(List<Tribe> players_tribes) {
        List<Integer> player_outcomes = new ArrayList<>();

        for (Tribe tribe : players_tribes) {
            int artists = tribe.getNumCharacterOfType(CharacterType.ARTIST);
            if (artists >= threshold) {
                player_outcomes.add(points_per_artist * artists);
            } else {
                player_outcomes.add(penalty);
            }
        }

        return player_outcomes;
    }

    @Override
    public BuildingTrigger asTrigger() {
        return BuildingTrigger.ON_EVENT_CAVE_PAINTINGS;
    }
}