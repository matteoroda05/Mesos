package it.polimi.ingsw.mesos.Model.Cards;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.mesos.Model.Card;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Events.CavePaintingEvent;
import it.polimi.ingsw.mesos.Model.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.Model.Events.SustenanceEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HunterCard.class,           name = "HUNTER"),
        @JsonSubTypes.Type(value = ShamanCard.class,           name = "SHAMAN"),
        @JsonSubTypes.Type(value = ArtistCard.class,           name = "ARTIST"),
        @JsonSubTypes.Type(value = BuilderCard.class,          name = "BUILDER"),
        @JsonSubTypes.Type(value = InventorCard.class,         name = "INVENTOR"),
        @JsonSubTypes.Type(value = GathererCard.class,         name = "GATHERER"),
        @JsonSubTypes.Type(value = HuntEvent.class,            name = "HUNT_EVENT"),
        @JsonSubTypes.Type(value = SustenanceEvent.class,      name = "SUSTENANCE_EVENT"),
        @JsonSubTypes.Type(value = ShamanicRitualEvent.class,  name = "SHAMANIC_RITUAL_EVENT"),
        @JsonSubTypes.Type(value = CavePaintingEvent.class,    name = "CAVE_PAINTING_EVENT")
})

abstract public class TribeCard extends Card {
    public TribeCard(Era era, int min_players) {
        super(era, min_players);
    }
}