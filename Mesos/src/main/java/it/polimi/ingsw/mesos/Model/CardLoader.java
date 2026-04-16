package it.polimi.ingsw.mesos.Model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CardLoader {

    private static final String RESOURCE_PATH = "/Cards.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CardLoader() {}

    /**
     * Loads all card and tiles from the bundled JSON resource.
     *
     * @return parsed card and offer-tile data
     * @throws IOException if the resource is missing or cannot be parsed
     */
    public static CardData load() throws IOException {
        try (InputStream in = CardLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Card resource not found: " + RESOURCE_PATH);
            }
            return load(in);
        }
    }

    /**
     * Parses card and tile definitions from a JSON input stream.
     *
     * @param in stream containing the card JSON document
     * @return parsed tribe cards, building cards, and offer tiles
     * @throws IOException if the stream cannot be read or parsed
     */
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

        List<OfferTile> offerTiles = MAPPER.convertValue(
                root.get("offerTiles"),
                new TypeReference<>() {}
        );

        return new CardData(tribeCards, buildingCards, offerTiles);
    }

    public record CardData(
            List<TribeCard> tribe_cards,
            List<BuildingCard> building_cards,
            List<OfferTile> offer_tiles
    ) {}
}
