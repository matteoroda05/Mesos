package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.dto.game.CardSummary;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resolves and caches JavaFX artwork for cards, offer tiles, totems, and
 * informational board images.
 */
public final class MesosAssetResolver {
    private static final Pattern TRIBE_ID_PATTERN = Pattern.compile(
            "^(hunter|builder|gatherer|artist|inventor|shaman)_era_([123])_([a-z])$");
    private static final Pattern BUILDING_ID_PATTERN = Pattern.compile("^building_era_([123])_(\\d{2})$");
    private static final Pattern EVENT_ID_PATTERN = Pattern.compile(
            "^(hunt|sustenance|shamanic_ritual|cave_painting)_event_era_([123])$");

    private final Map<String, Optional<Image>> imageCache;

    /**
     * Creates an asset resolver with a thread-safe cache for successful and missing resources.
     */
    public MesosAssetResolver() {
        this.imageCache = new ConcurrentHashMap<>();
    }

    /**
     * Resolves the image for a card that has a recognized presentation ID.
     *
     * @param cardSummary card summary to resolve, possibly null
     * @return cached card image, or an empty optional for absent or unsupported IDs
     */
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

    /**
     * Resolves the image for a player totem color.
     *
     * @param color color whose totem should be loaded, possibly null
     * @return cached totem image, or an empty optional when color is null or
     *         the resource is unavailable
     */
    public Optional<Image> resolveTotem(Color color) {
        if (color == null) {
            return Optional.empty();
        }

        String resourcePath = switch (color) {
            case BLUE -> "/cards_png/totems/totem_blue.png";
            case YELLOW -> "/cards_png/totems/totem_yellow.png";
            case ORANGE -> "/cards_png/totems/totem_orange.png";
            case WHITE -> "/cards_png/totems/totem_white.png";
            case BLACK -> "/cards_png/totems/totem_black.png";
        };
        return load(resourcePath);
    }

    /**
     * Resolves the image for an offer tile, cropping shared images where needed.
     *
     * @param tileId case-insensitive offer tile identifier from A through G
     * @return cached full or cropped tile image, or an empty optional for an
     *         unknown identifier or missing resource
     */
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

    /**
     * Resolves the informational image shown for event rounds.
     *
     * @return cached round-event information image, if available
     */
    public Optional<Image> resolveRoundEventInfo() {
        return load("/cards_png/card_event_info_118.png");
    }

    /**
     * Resolves the informational image shown at the end of the game.
     *
     * @return cached end-game information image, if available
     */
    public Optional<Image> resolveEndGameInfo() {
        return load("/cards_png/card_info_end_game.png");
    }

    /**
     * Resolves an image directly from the root cards resource directory.
     *
     * @param filename resource filename beneath {@code /cards_png/}
     * @return cached image, or an empty optional when the resource is absent
     */
    public Optional<Image> resolveBackgroundImage(String filename) {
        return load("/cards_png/" + filename);
    }

    /**
     * Maps a validated presentation ID to its cards resource path.
     *
     * @param presentationId card presentation ID to classify
     * @return matching resource path, or null for unsupported IDs
     */
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

    /**
     * Loads an image resource once and caches both hits and misses.
     *
     * @param resourcePath classpath resource path
     * @return cached image optional for the path
     */
    private Optional<Image> load(String resourcePath) {
        return imageCache.computeIfAbsent(resourcePath, this::loadImage);
    }

    /**
     * Loads and caches one horizontal half of a combined offer-tile image.
     *
     * @param resourcePath combined offer-tile image path
     * @param rightHalf whether to crop the right rather than left half
     * @return cached cropped image optional
     */
    private Optional<Image> loadOfferHalf(String resourcePath, boolean rightHalf) {
        String cacheKey = resourcePath + (rightHalf ? "#right" : "#left");
        return imageCache.computeIfAbsent(cacheKey, ignored -> loadHalfImage(resourcePath, rightHalf));
    }

    /**
     * Loads a full image asynchronously from a classpath resource.
     *
     * @param resourcePath classpath resource path
     * @return image optional, empty when the resource is missing
     */
    private Optional<Image> loadImage(String resourcePath) {
        URL resourceUrl = MesosAssetResolver.class.getResource(resourcePath);
        if (resourceUrl == null) {
            return Optional.empty();
        }
        return Optional.of(new Image(resourceUrl.toExternalForm(), true));
    }

    /**
     * Loads a combined tile image and crops the requested horizontal half.
     *
     * @param resourcePath classpath resource path
     * @param rightHalf whether to crop the right rather than left half
     * @return cropped image optional, or the full image if it has no pixel reader
     */
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
