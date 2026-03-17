package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class InventorCard extends CharacterCard {
    private final InventionIcon inventionIcon;

    @JsonCreator
    public InventorCard(
            @JsonProperty("era")           Era era,
            @JsonProperty("minPlayers")    int min_players,
            @JsonProperty("inventionIcon") InventionIcon inventionIcon
    ) {
        super(era, min_players);
        this.inventionIcon = inventionIcon;
    }

    public InventionIcon getInventionIcon() {
        return inventionIcon;
    }

    @Override
    public CharacterType getCharacterType() {
        return CharacterType.INVENTOR;
    }
}