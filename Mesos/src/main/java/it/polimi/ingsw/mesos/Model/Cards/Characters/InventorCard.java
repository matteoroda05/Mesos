package it.polimi.ingsw.mesos.Model.Cards.Characters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public final class InventorCard extends CharacterCard {
    private final InventionIcon invention_icon;

    @JsonCreator
    public InventorCard(
            @JsonProperty("era")           Era era,
            @JsonProperty("minPlayers")    int min_players,
            @JsonProperty("inventionIcon") InventionIcon invention_icon
    ) {
        super(era, min_players);
        if (invention_icon == null) throw new IllegalArgumentException("Invention Icon cannot be null");
        this.invention_icon = invention_icon;
    }

    public InventionIcon getInventionIcon() {
        return this.invention_icon;
    }
}