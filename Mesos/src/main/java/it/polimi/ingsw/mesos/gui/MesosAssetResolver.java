package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.CardSummary;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MesosAssetResolver {
    private static final Pattern TRIBE_ID_PATTERN = Pattern.compile(
            "^(hunter|builder|gatherer|artist|inventor|shaman)_era_([123])_([a-z])$");
    private static final Pattern BUILDING_ID_PATTERN = Pattern.compile("^building_era_([123])_(\\d{2})$");
    private static final Map<String, Integer> TRIBE_TYPE_OFFSETS = Map.of(
            "hunter", 0,
            "builder", 8,
            "gatherer", 11,
            "artist", 14,
            "inventor", 18,
            "shaman", 22
    );
    private static final Map<String, String> EVENT_RESOURCE_PATHS = Map.ofEntries(
            Map.entry("hunt_event_era_1", "/cards_png/carte_front/events/Cards_total_front_85.png"),
            Map.entry("sustenance_event_era_1", "/cards_png/carte_front/events/Cards_total_front_86.png"),
            Map.entry("shamanic_ritual_event_era_1", "/cards_png/carte_front/events/Cards_total_front_87.png"),
            Map.entry("cave_painting_event_era_1", "/cards_png/carte_front/events/Cards_total_front_88.png"),
            Map.entry("hunt_event_era_2", "/cards_png/carte_front/events/Cards_total_front_89.png"),
            Map.entry("sustenance_event_era_2", "/cards_png/carte_front/events/Cards_total_front_90.png"),
            Map.entry("shamanic_ritual_event_era_2", "/cards_png/carte_front/events/Cards_total_front_91.png"),
            Map.entry("cave_painting_event_era_2", "/cards_png/carte_front/events/Cards_total_front_92.png"),
            Map.entry("hunt_event_era_3", "/cards_png/carte_front/events/Cards_total_front_93.png"),
            Map.entry("cave_painting_event_era_3", "/cards_png/carte_front/events/Cards_total_front_94.png"),
            Map.entry("sustenance_event_era_3", "/cards_png/carte_front/events/Cards_total_front_95.png"),
            Map.entry("shamanic_ritual_event_era_3", "/cards_png/carte_front/events/Cards_total_front_96.png")
    );
    private static final int[] BUILDING_COUNTS_BY_ERA = {6, 7, 8};

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

    public Optional<Image> resolveTurnOrderTile(int playerCount) {
        if (playerCount < 2 || playerCount > 5) {
            return Optional.empty();
        }
        return load("/cards_png/carte_front/turn_order_tile/turn_order_tile_" + playerCount + ".png");
    }

    public Optional<Image> resolveHeroImage() {
        return load("/cards_png/Box_ITA_ART_1.png");
    }

    public Optional<Image> resolveTribeBack(Era era) {
        if (era == null) {
            return Optional.empty();
        }
        return load("/cards_png/carte_back/tribe_" + eraSuffix(era) + ".png");
    }

    public Optional<Image> resolveBuildingBack(Era era) {
        if (era == null) {
            return Optional.empty();
        }
        return load("/cards_png/carte_back/building_" + eraSuffix(era) + ".png");
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
        Matcher tribeMatcher = TRIBE_ID_PATTERN.matcher(presentationId);
        if (tribeMatcher.matches()) {
            String type = tribeMatcher.group(1);
            int eraIndex = Integer.parseInt(tribeMatcher.group(2)) - 1;
            int withinTypeIndex = tribeMatcher.group(3).charAt(0) - 'a';
            Integer typeOffset = TRIBE_TYPE_OFFSETS.get(type);
            if (typeOffset == null || eraIndex < 0 || eraIndex > 2 || withinTypeIndex < 0) {
                return null;
            }

            int cardNumber = 1 + (eraIndex * 28) + typeOffset + withinTypeIndex;
            if (cardNumber < 1 || cardNumber > 84) {
                return null;
            }
            return "/cards_png/carte_front/tribe/Cards_total_front_" + cardNumber + ".png";
        }

        String eventPath = EVENT_RESOURCE_PATHS.get(presentationId);
        if (eventPath != null) {
            return eventPath;
        }

        Matcher buildingMatcher = BUILDING_ID_PATTERN.matcher(presentationId);
        if (buildingMatcher.matches()) {
            int eraIndex = Integer.parseInt(buildingMatcher.group(1)) - 1;
            int withinEraIndex = Integer.parseInt(buildingMatcher.group(2));
            if (eraIndex < 0 || eraIndex >= BUILDING_COUNTS_BY_ERA.length) {
                return null;
            }
            if (withinEraIndex < 1 || withinEraIndex > BUILDING_COUNTS_BY_ERA[eraIndex]) {
                return null;
            }

            int cardNumber = 97 + withinEraIndex - 1;
            for (int index = 0; index < eraIndex; index++) {
                cardNumber += BUILDING_COUNTS_BY_ERA[index];
            }
            return "/cards_png/carte_front/buildings/Cards_total_front_" + cardNumber + ".png";
        }

        return null;
    }

    private String eraSuffix(Era era) {
        return switch (era) {
            case FIRST -> "era_1";
            case SECOND -> "era_2";
            case THIRD -> "era_3";
        };
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
