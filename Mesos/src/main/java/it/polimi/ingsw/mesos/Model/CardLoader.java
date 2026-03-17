package it.polimi.ingsw.mesos.Model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class CardLoader {

    private static final String RESOURCE_PATH = "/Cards.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CardLoader() {}


    public static CardData load() throws IOException {
        try (InputStream in = CardLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Card resource not found: " + RESOURCE_PATH);
            }
            return load(in);
        }
    }


    public static CardData load(InputStream in) throws IOException {
        JsonNode root = MAPPER.readTree(in);

        List<TribeCard> tribeCards = MAPPER.convertValue(
                root.get("tribeCards"),
                new TypeReference<>() {}
        );

        List<BuildingCard> buildingCards = MAPPER.convertValue(
                root.get("buildingCards"),
                new TypeReference<>() {}
        );

        return new CardData(tribeCards, buildingCards);
    }


    public record CardData(List<TribeCard> tribeCards, List<BuildingCard> buildingCards) {}
}