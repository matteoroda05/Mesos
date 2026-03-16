package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public final class HunterCard extends CharacterCard {
    private final boolean active_hunter;

    public HunterCard(Era era, int min_players, boolean active_hunter) {
        super(era, min_players);
        this.active_hunter = active_hunter;
    }

    public boolean isactive_hunter() {
        return active_hunter;
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.HUNTER;
    }
}