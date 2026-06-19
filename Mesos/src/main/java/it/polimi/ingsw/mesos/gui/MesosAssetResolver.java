package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Network.CardSummary;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class MesosAssetResolver {
    private static final Pattern TRIBE_ID_PATTERN = Pattern.compile(
            "^(hunter|builder|gatherer|artist|inventor|shaman)_era_([123])_([a-z])$");
    private static final Pattern BUILDING_ID_PATTERN = Pattern.compile("^building_era_([123])_(\\d{2})$");
    private static final Pattern EVENT_ID_PATTERN = Pattern.compile(
            "^(hunt|sustenance|shamanic_ritual|cave_painting)_event_era_([123])$");

    private final Map<String, Optional<Image>> imageCache;

    public MesosAssetResolver() {
        this.imageCache = new ConcurrentHashMap<>();
    }

    public Optional<Image> resolveCard(CardSummary cardSummary) {
        if (cardSummary == null || cardSummary.presentationId() == null) {
            return Optional.empty();
        }

        String resourcePath = resolveCardResourcePath(cardSummary.presentationId());
        if (resourcePath == null) {
            return Optional.empty();
        }
        return load(resourcePath);
    }

    public Optional<Image> resolveTotem(Color color) {
        if (color == null) {
            return Optional.empty();
        }

        String resourcePath = switch (color) {
            case BLUE -> "/cards_png/totems/totem_blue.png";
            case YELLOW -> "/cards_png/totems/totem_yellow.png";
            case ORANGE -> "/cards_png/totems/totem_orange.png";
            case WHITE -> "/cards_png/totems/totem_white.png";
            case BLACK -> "/cards_png/totems/totem_purple.png";
        };
        return load(resourcePath);
    }

    public Optional<Image> resolveOfferTile(char tileId) {
        return switch (Character.toUpperCase(tileId)) {
            case 'A' -> load("/cards_png/carte_front/offer_tiles/offer_tile_A.png");
            case 'B' -> loadOfferHalf("/cards_png/carte_front/offer_tiles/offer_tile_B_C.png", false);
            case 'C' -> loadOfferHalf("/cards_png/carte_front/offer_tiles/offer_tile_B_C.png", true);
            case 'D' -> load("/cards_png/carte_front/offer_tiles/offer_tile_D.png");
            case 'E' -> loadOfferHalf("/cards_png/carte_front/offer_tiles/offer_tile_E_F.png", false);
            case 'F' -> loadOfferHalf("/cards_png/carte_front/offer_tiles/offer_tile_E_F.png", true);
            case 'G' -> load("/cards_png/carte_front/offer_tiles/offer_tile_G.png");
            default -> Optional.empty();
        };
    }

    public Optional<Image> resolveRoundEventInfo() {
        return load("/cards_png/card_event_info_118.png");
    }

    public Optional<Image> resolveEndGameInfo() {
        return load("/cards_png/card_info_end_game.png");
    }

    public Optional<Image> resolveBackgroundImage(String filename) {
        return load("/cards_png/" + filename);
    }

    private String resolveCardResourcePath(String presentationId) {
        if (TRIBE_ID_PATTERN.matcher(presentationId).matches()) {
            return "/cards_png/carte_front/tribe/" + presentationId + ".png";
        }
        if (BUILDING_ID_PATTERN.matcher(presentationId).matches()) {
            return "/cards_png/carte_front/buildings/" + presentationId + ".png";
        }
        if (EVENT_ID_PATTERN.matcher(presentationId).matches()) {
            return "/cards_png/carte_front/events/" + presentationId + ".png";
        }
        return null;
    }

    private Optional<Image> load(String resourcePath) {
        return imageCache.computeIfAbsent(resourcePath, this::loadImage);
    }

    private Optional<Image> loadOfferHalf(String resourcePath, boolean rightHalf) {
        String cacheKey = resourcePath + (rightHalf ? "#right" : "#left");
        return imageCache.computeIfAbsent(cacheKey, ignored -> loadHalfImage(resourcePath, rightHalf));
    }

    private Optional<Image> loadImage(String resourcePath) {
        URL resourceUrl = MesosAssetResolver.class.getResource(resourcePath);
        if (resourceUrl == null) {
            return Optional.empty();
        }
        return Optional.of(new Image(resourceUrl.toExternalForm(), true));
    }

    private Optional<Image> loadHalfImage(String resourcePath, boolean rightHalf) {
        URL resourceUrl = MesosAssetResolver.class.getResource(resourcePath);
        if (resourceUrl == null) {
            return Optional.empty();
        }

        Image sourceImage = new Image(resourceUrl.toExternalForm(), false);
        PixelReader pixelReader = sourceImage.getPixelReader();
        if (pixelReader == null) {
            return Optional.of(sourceImage);
        }

        int sourceWidth = (int) Math.round(sourceImage.getWidth());
        int sourceHeight = (int) Math.round(sourceImage.getHeight());
        int halfWidth = Math.max(1, sourceWidth / 2);
        int cropX = rightHalf ? sourceWidth - halfWidth : 0;
        return Optional.of(new WritableImage(pixelReader, cropX, 0, halfWidth, sourceHeight));
    }
}
