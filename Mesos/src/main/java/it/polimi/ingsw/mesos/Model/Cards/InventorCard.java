package it.polimi.ingsw.mesos.Model.Cards;

import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class InventorCard extends CharacterCard {
    private final InventionIcon inventionIcon;

    public InventorCard(Era era, int min_players, InventionIcon inventionIcon) {
        super(era, min_players);
        this.inventionIcon = inventionIcon;
    }

    public InventionIcon getInventionIcon() {
        return inventionIcon;
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.ARTIST;
    }
}