package it.polimi.ingsw.mesos.CLI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Network.CardSummary;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.LobbyGameSummary;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.OfferTileSummary;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import it.polimi.ingsw.mesos.Network.TurnOrderSpaceSummary;

final class CliPrinter {
    private static final String NEW_LINE = System.lineSeparator();
    private static final int DEFAULT_BOARD_WIDTH = 180;
    private static final int MIN_BOARD_WIDTH = 100;
    private static final int MAX_BOARD_WIDTH = 240;
    private static final int MIN_CARD_WIDTH = 24;
    private static final int MAX_CARD_WIDTH = 40;
    private static final int PREFERRED_CARD_WIDTH = 28;
    private static final int CARD_BODY_HEIGHT = 7;
    private static final int OFFER_TILE_BODY_HEIGHT = 6;
    private static final int TURN_ORDER_MAX_WIDTH = 44;
    private static final int MIN_OFFER_TILE_WIDTH = 26;
    private static final int MAX_OFFER_TILE_WIDTH = 32;
    private static final int MAX_LOBBY_WIDTH = 96;
    private static final int SECTION_HORIZONTAL_PADDING = 3;
    private static final int BOX_HORIZONTAL_PADDING = 2;
    private static final String COLUMN_GAP = "  ";
    private static final String CENTER_AREA_GAP = "      ";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");
    private static final String COLOR_BUILDING = "#D6A354";
    private static final String COLOR_INVENTOR = "#62B2A0";
    private static final String COLOR_GATHERER = "#D77C45";
    private static final String COLOR_SHAMAN = "#764152";
    private static final String COLOR_BUILDER = "#31121A";
    private static final String COLOR_ARTIST = "#F2D058";
    private static final String COLOR_HUNTER = "#CD5244";
    private static final String COLOR_SUSTENANCE = "#FF9354";
    private static final String COLOR_SHAMANIC_RITUAL = "#FF8DB4";
    private static final String COLOR_CAVE_PAINTING = "#FFD616";
    private static final String COLOR_HUNT = "#FF4656";
    private static final String COLOR_PLAYER_BLACK = "#393934";
    private static final String COLOR_PLAYER_WHITE = "#DDD8CB";
    private static final String COLOR_PLAYER_BLUE = "#5894A1";
    private static final String COLOR_PLAYER_ORANGE = "#D16544";
    private static final String COLOR_PLAYER_YELLOW = "#DFB95B";

    /**
     * Renders the full game snapshot for the terminal.
     *
     * @param snapshot snapshot to render
     * @param localNickname nickname of the local player, or null when unknown
     * @return formatted snapshot text
     * @throws NullPointerException if the snapshot or one of its required lists
     *                              is null
     */
    String renderSnapshot(GameSnapshot snapshot, String localNickname) {
        int boardWidth = resolveBoardWidth();
        StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE);
        appendSection(builder, "GAME TABLE", createGameInfoBox(snapshot, localNickname, boardWidth));
        appendRow(builder, "TOP ROW", snapshot.topRow(), "top", boardWidth);
        appendSection(
                builder,
                "TURN ORDER TILE AND OFFER TRACK",
                renderCenterArea(snapshot.turnOrder(), snapshot.offerTiles(), snapshot.playerCount(), boardWidth)
        );
        appendRow(builder, "BOTTOM ROW", snapshot.bottomRow(), "bottom", boardWidth);
        appendSection(
                builder,
                snapshot.finished() ? "PLAYERS / FINAL RANKING" : "PLAYERS",
                createPlayersBox(
                        snapshot.players(),
                        snapshot.playerCount(),
                        localNickname,
                        snapshot.finished(),
                        snapshot.currentPlayerNickname(),
                        boardWidth
                )
        );
        appendSection(builder, "COMMAND HINT", createAutoDoubleBox(List.of(commandsHint(snapshot, localNickname)), boardWidth, null));

        return builder.toString();
    }

    /**
     * Renders the lobby game list grouped by player count.
     *
     * @param lobbySnapshot lobby data to render
     * @return formatted lobby text, including an empty-lobby message when no
     *         games are open
     * @throws NullPointerException if the snapshot or its game list is null
     */
    String renderLobby(LobbySnapshot lobbySnapshot) {
        int lobbyWidth = Math.min(resolveBoardWidth(), MAX_LOBBY_WIDTH);
        StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE);
        appendLines(builder, createLobbyBox(lobbySnapshot, lobbyWidth));
        builder.append(NEW_LINE)
                .append("Choose 'join' to enter an existing game or 'create' to create a new one.")
                .append(NEW_LINE)
                .append("When joining, enter only the game name.")
                .append(NEW_LINE);
        return builder.toString();
    }

    /**
     * Renders the static CLI command help.
     *
     * @return help text with supported commands
     */
    String renderHelp() {
        return String.join(NEW_LINE,
                "",
                "Available commands:",
                "  <enter>               refreshes the table",
                "  show | mostra         prints the current snapshot again",
                "  offer <index>         chooses an offer tile",
                "  card top <index>      takes a card from the top row",
                "  card bottom <index>   takes a card from the bottom row",
                "  pass | passa          skips the turn when you have extra cards to take",
                "  wait [seconds]        waits briefly and refreshes",
                "  help | aiuto          shows this help",
                "  quit | esci | exit    closes the client");
    }

    /**
     * Builds the command hint shown below a snapshot.
     *
     * @param snapshot current game snapshot
     * @param localNickname nickname of the local player
     * @return context-sensitive command hint
     * @throws NullPointerException if the snapshot is null
     */
    private String commandsHint(GameSnapshot snapshot, String localNickname) {
        if (!snapshot.started()) {
            return "Open lobby: press Enter to refresh while the other players join.";
        }
        if (snapshot.finished()) {
            return "Game finished: use 'quit' to exit or press Enter to review the result.";
        }
        if (!Objects.equals(snapshot.currentPlayerNickname(), localNickname)) {
            return "It is not your turn: press Enter to refresh or use 'wait 2'.";
        }
        return switch (snapshot.status()) {
            case ROUND_OFFER -> "Your turn: choose a tile with 'offer <index>'.";
            case ROUND_ACTIONS -> "Your turn: use 'card top <index>', 'card bottom <index>', or 'pass' for the extra card.";
            default -> "Automatic phase in progress: press Enter to refresh.";
        };
    }

    /**
     * Builds the short status message shown below the snapshot header.
     *
     * @param snapshot current game snapshot
     * @param localNickname nickname of the local player, or null when unknown
     * @return scene message that describes the current phase and active player
     * @throws NullPointerException if the snapshot or its player list is null
     */
    private String sceneMessage(GameSnapshot snapshot, String localNickname) {
        String youAre = "";
        if (localNickname != null) {
            youAre = snapshot.players().stream()
                    .filter(player -> localNickname.equals(player.nickname()))
                    .findFirst()
                    .map(player -> "You are " + playerLabel(player.nickname(), player.color()) + ". ")
                    .orElse("You are " + localNickname + ". ");
        }

        if (!snapshot.started()) {
            return youAre + "The game has not started yet.";
        }
        if (snapshot.finished()) {
            return youAre + "The game is over. Check the final ranking below.";
        }
        if (snapshot.currentPlayerNickname() == null) {
            return youAre + "The table is updating.";
        }
        if (Objects.equals(snapshot.currentPlayerNickname(), localNickname)) {
            return switch (snapshot.status()) {
                case ROUND_OFFER -> youAre + "It is your turn: choose the offer tile to prepare the round.";
                case ROUND_ACTIONS -> youAre + "It is your turn: collect the cards your tile lets you take.";
                default -> youAre + "It is your turn, but the game is resolving an automatic phase.";
            };
        }

        String currentPlayer = playerLabel(snapshot.currentPlayerNickname(), snapshot.currentPlayerColor());
        if (snapshot.status() == GameStatus.ROUND_ACTIONS) {
            return youAre + currentPlayer + " is playing. Remaining choices: top=" + snapshot.remainingTopChoices()
                    + ", bottom=" + snapshot.remainingBottomChoices() + ".";
        }
        return youAre + currentPlayer + " is playing.";
    }

    /**
     * Appends the player summary list.
     * Finished games are sorted by prestige, while active games keep the
     * snapshot order.
     *
     * @param players player summaries to render
     * @param localNickname nickname to mark as local
     * @param finished whether the game is finished
     * @param currentPlayerNickname nickname to mark as active
     * @throws NullPointerException if the builder or player list is null
     */
    private List<String> createPlayersBox(
            List<PlayerSummary> players,
            int playerCount,
            String localNickname,
            boolean finished,
            String currentPlayerNickname,
            int boardWidth
    ) {
        List<String> body = new ArrayList<>();
        if (players.isEmpty()) {
            body.add("No players present.");
            return createAutoDoubleBox(fitLines(body, playerSlots(playerCount), boardWidth - boxFixedContentWidth()), boardWidth, null);
        }

        List<PlayerSummary> orderedPlayers = finished
                ? players.stream()
                .sorted(Comparator.comparingInt(PlayerSummary::prestigePoints).reversed())
                .toList()
                : players;

        for (PlayerSummary player : orderedPlayers) {
            StringBuilder line = new StringBuilder();
            if (player.nickname().equals(currentPlayerNickname)) {
                line.append("> ");
            } else {
                line.append("  ");
            }
            if (player.nickname().equals(localNickname)) {
                line.append("* ");
            } else {
                line.append("  ");
            }
            line.append(playerLabel(player.nickname(), player.color()))
                    .append(" | food ")
                    .append(player.food())
                    .append(" | prestige ")
                    .append(player.prestigePoints())
                    .append(" | tribe ")
                    .append(player.characterCount())
                    .append(" | buildings ")
                    .append(player.buildingCount());
            body.add(line.toString());
        }

        return createAutoDoubleBox(fitLines(body, playerSlots(playerCount), boardWidth - boxFixedContentWidth()), boardWidth, null);
    }

    /**
     * Appends the turn-order and offer-track panels side by side when they fit,
     * otherwise stacks them vertically.
     *
     * @param turnOrder turn-order spaces
     * @param offerTiles offer tiles
     * @param boardWidth available board width
     * @throws NullPointerException if the builder or either list is null
     */
    private List<String> renderCenterArea(
            List<TurnOrderSpaceSummary> turnOrder,
            List<OfferTileSummary> offerTiles,
            int playerCount,
            int boardWidth
    ) {
        List<String> turnOrderPanel = renderTurnOrderPanel(turnOrder, playerCount);
        List<String> offerTrackPanel = renderOfferTrackPanel(offerTiles);

        if (lineWidth(turnOrderPanel) + lineWidth(offerTrackPanel) + CENTER_AREA_GAP.length() <= boardWidth) {
            return mergeColumns(List.of(turnOrderPanel, offerTrackPanel), CENTER_AREA_GAP);
        }

        List<String> stacked = new ArrayList<>(turnOrderPanel);
        stacked.add("");
        stacked.addAll(offerTrackPanel);
        return stacked;
    }

    /**
     * Renders the turn-order panel.
     *
     * @param turnOrder turn-order spaces to render
     * @return panel lines, including an empty message when no spaces exist
     * @throws NullPointerException if the turn-order list is null
     */
    private List<String> renderTurnOrderPanel(List<TurnOrderSpaceSummary> turnOrder, int playerCount) {
        List<String> body = new ArrayList<>();
        for (TurnOrderSpaceSummary space : turnOrder) {
            String occupant = space.occupantNickname() == null
                    ? "empty"
                    : playerLabel(space.occupantNickname(), space.occupantColor());
            body.add("[" + space.index() + "] bonus " + signed(space.foodBonus()) + " -> " + occupant);
        }
        if (turnOrder.isEmpty()) {
            body.add("No turn spaces available.");
        }
        int targetLines = playerSlots(playerCount);
        List<String> fitted = fitLines(body, targetLines, TURN_ORDER_MAX_WIDTH - boxFixedContentWidth());
        return createAutoDoubleBox(fitted, TURN_ORDER_MAX_WIDTH, null);
    }

    /**
     * Renders the offer-track panel.
     *
     * @param offerTiles offer tiles to render
     * @return panel lines, including an empty message when no tiles exist
     * @throws NullPointerException if the offer-tile list is null
     */
    private List<String> renderOfferTrackPanel(List<OfferTileSummary> offerTiles) {
        if (offerTiles.isEmpty()) {
            return createDoubleBox(List.of("OFFER TILES", "No tiles available."), 24, null);
        }

        int splitIndex = offerTiles.size() <= 4 ? offerTiles.size() : (offerTiles.size() + 1) / 2;
        int widestRow = Math.max(splitIndex, offerTiles.size() - splitIndex);
        int offerTileWidth = offerTileWidthFor(widestRow);
        List<String> firstRow = mergeColumns(
                offerTiles.subList(0, splitIndex).stream().map(tile -> createOfferTileBox(tile, offerTileWidth)).toList(),
                " "
        );
        List<String> body = new ArrayList<>(firstRow);
        if (splitIndex < offerTiles.size()) {
            body.add("");
            body.addAll(mergeColumns(
                    offerTiles.subList(splitIndex, offerTiles.size()).stream()
                            .map(tile -> createOfferTileBox(tile, offerTileWidth))
                            .toList(),
                    " "
            ));
        }
        return body;
    }

    /**
     * Appends one market row, arranging cards into shelves that fit the board
     * width.
     *
     * @param builder destination builder
     * @param title row title
     * @param cards cards visible in the row
     * @param rowToken command token used for card selection
     * @param boardWidth target board width
     * @throws NullPointerException if the builder or cards list is null
     */
    private void appendRow(StringBuilder builder, String title, List<CardSummary> cards, String rowToken, int boardWidth) {
        appendLines(builder, createSectionHeader(title));
        if (cards.isEmpty()) {
            appendLines(builder, createAutoDoubleBox(List.of("No visible cards."), boardWidth, null));
            return;
        }

        int cardsPerShelf = preferredCardsPerShelf(cards.size(), boardWidth);
        int cardWidth = cardWidthFor(cardsPerShelf, boardWidth);
        for (int start = 0; start < cards.size(); start += cardsPerShelf) {
            int end = Math.min(cards.size(), start + cardsPerShelf);
            appendLines(builder, mergeColumns(
                    cards.subList(start, end).stream().map(card -> createCardBox(card, rowToken, cardWidth)).toList(),
                    COLUMN_GAP
            ));
            if (end < cards.size()) {
                builder.append(NEW_LINE);
            }
        }
        builder.append(NEW_LINE);
    }

    /**
     * Creates the fixed-height text box for a visible card.
     *
     * @param card card summary to render
     * @param rowToken command token for the row containing the card
     * @param cardWidth target card width
     * @return card box lines
     * @throws NullPointerException if the card is null
     */
    private List<String> createCardBox(CardSummary card, String rowToken, int cardWidth) {
        List<String> content = new ArrayList<>();
        content.add(cardBadge(card));
        content.add("Name: " + card.type());
        content.add("Era " + card.era());
        content.add("Cost " + valueOrDash(card.foodCost()) + " | PP " + valueOrDash(card.prestigePoints()));
        content.addAll(wrap(card.details().isBlank() ? "No additional details." : card.details(), cardWidth - boxFixedContentWidth()));

        List<String> body = fitLines(content, CARD_BODY_HEIGHT - 1, cardWidth - boxFixedContentWidth());
        body.add("card " + rowToken + " " + card.selectionIndex());
        return createDoubleBox(body, cardWidth, cardColor(card));
    }

    /**
     * Chooses how many cards should be placed on one visual shelf.
     *
     * @param cardCount number of cards to render
     * @param boardWidth target board width
     * @return at least one column and, for positive card counts, never more
     *         than the number of cards
     */
    private int preferredCardsPerShelf(int cardCount, int boardWidth) {
        int maxColumns = Math.max(1, (boardWidth + COLUMN_GAP.length()) / (MIN_CARD_WIDTH + COLUMN_GAP.length()));
        int preferredColumns = Math.max(1, (boardWidth + COLUMN_GAP.length()) / (PREFERRED_CARD_WIDTH + COLUMN_GAP.length()));
        return Math.clamp(cardCount, 1, Math.min(maxColumns, preferredColumns));
    }

    /**
     * Computes card width for the selected shelf layout.
     *
     * @param cardsPerShelf cards rendered on one shelf
     * @param boardWidth target board width
     * @return width clamped between the minimum and maximum card widths
     */
    private int cardWidthFor(int cardsPerShelf, int boardWidth) {
        int availableWidth = boardWidth - ((cardsPerShelf - 1) * COLUMN_GAP.length());
        int computedWidth = availableWidth / cardsPerShelf;
        return clamp(computedWidth, MIN_CARD_WIDTH, MAX_CARD_WIDTH);
    }

    /**
     * Chooses offer-tile box width for a row of offer tiles.
     *
     * @param tilesInRow number of tiles in the rendered row
     * @return width clamped between the minimum and maximum offer-tile widths
     */
    private int offerTileWidthFor(int tilesInRow) {
        if (tilesInRow <= 0) {
            return MIN_OFFER_TILE_WIDTH;
        }
        int targetWidth;
        if (tilesInRow >= 5) {
            targetWidth = 26;
        } else if (tilesInRow == 4) {
            targetWidth = 28;
        } else {
            targetWidth = 30;
        }
        return clamp(targetWidth, MIN_OFFER_TILE_WIDTH, MAX_OFFER_TILE_WIDTH);
    }

    /**
     * Creates the text box for one offer tile.
     *
     * @param tile offer tile summary
     * @param tileWidth target tile width
     * @return offer tile box lines
     * @throws NullPointerException if the tile is null
     */
    private List<String> createOfferTileBox(OfferTileSummary tile, int tileWidth) {
        List<String> body = new ArrayList<>();
        body.add("[" + tile.index() + "/" + tile.id() + "] OFFER TILE");
        body.add("Top   : " + tile.topPicks());
        body.add("Bottom: " + tile.bottomPicks());
        body.add("Food  : " + signed(tile.foodGain()));
        body.add(tile.available()
                ? "Status: Free"
                : "Status: " + playerLabel(tile.occupantNickname(), tile.occupantColor()));
        body.add("offer " + tile.index());
        return createDoubleBox(fitLines(body, OFFER_TILE_BODY_HEIGHT, tileWidth - boxFixedContentWidth()), tileWidth, null);
    }

    /**
     * Appends a fixed-width section title and its already-rendered body.
     *
     * @param builder destination builder
     * @param title section title
     * @param body rendered section content
     */
    private void appendSection(StringBuilder builder, String title, List<String> body) {
        appendLines(builder, createSectionHeader(title));
        appendLines(builder, body);
        builder.append(NEW_LINE);
    }

    /**
     * Creates a fixed-width ASCII section header.
     *
     * @param title section title
     * @return section header lines
     */
    private List<String> createSectionHeader(String title) {
        int width = visibleWidth(title) + 2 + (SECTION_HORIZONTAL_PADDING * 2);
        String padding = repeat(" ", SECTION_HORIZONTAL_PADDING);
        return List.of(
                "+" + repeat("-", width - 2) + "+",
                "|" + padding + title + padding + "|",
                "+" + repeat("-", width - 2) + "+"
        );
    }

    /**
     * Creates a double-bordered fixed-width box.
     *
     * @param body body lines to include
     * @param requestedWidth target width
     * @param colorHex optional ANSI truecolor hex color
     * @return double-bordered box lines
     */
    private List<String> createDoubleBox(List<String> body, int requestedWidth, String colorHex) {
        int bodyWidth = body.stream().mapToInt(this::visibleWidth).max().orElse(0);
        int width = Math.max(requestedWidth, bodyWidth + boxFixedContentWidth());
        int contentWidth = Math.max(0, width - boxFixedContentWidth());
        String padding = repeat(" ", BOX_HORIZONTAL_PADDING);
        List<String> lines = new ArrayList<>();
        lines.add(colorize("╔" + repeat("═", width - 2) + "╗", colorHex));
        for (String line : body) {
            lines.add(colorize("║" + padding + padRight(line, contentWidth) + padding + "║", colorHex));
        }
        lines.add(colorize("╚" + repeat("═", width - 2) + "╝", colorHex));
        return lines;
    }

    /**
     * Creates a double-bordered box sized to its contents up to a maximum.
     *
     * @param body body lines to include
     * @param maxWidth maximum target width
     * @param colorHex optional ANSI truecolor hex color
     * @return double-bordered box lines
     */
    private List<String> createAutoDoubleBox(List<String> body, int maxWidth, String colorHex) {
        int bodyWidth = body.stream().mapToInt(this::visibleWidth).max().orElse(0);
        int width = Math.min(maxWidth, bodyWidth + boxFixedContentWidth());
        int contentWidth = Math.max(0, width - boxFixedContentWidth());
        List<String> fitted = body.stream()
                .map(line -> truncate(line, contentWidth))
                .toList();
        return createDoubleBox(fitted, width, colorHex);
    }

    /**
     * Creates the lobby listing box without affecting the prompt text below it.
     *
     * @param lobbySnapshot lobby data to render
     * @param maxWidth maximum box width
     * @return double-bordered lobby box lines
     */
    private List<String> createLobbyBox(LobbySnapshot lobbySnapshot, int maxWidth) {
        int maxContentWidth = Math.max(1, maxWidth - boxFixedContentWidth());
        List<List<String>> sections = new ArrayList<>();
        sections.add(List.of(
                "LOBBY MESOS",
                "Available games: " + lobbySnapshot.openGames().size()
        ));

        if (lobbySnapshot.openGames().isEmpty()) {
            sections.add(wrap("No open games right now. You can create a new one.", maxContentWidth));
        } else {
            for (int playerCount = Globals.MIN_NUM_PLAYERS; playerCount <= Globals.MAX_NUM_PLAYERS; playerCount++) {
                int currentPlayerCount = playerCount;
                List<LobbyGameSummary> gamesForPlayerCount = lobbySnapshot.openGames().stream()
                        .filter(game -> game.playerCount() == currentPlayerCount)
                        .toList();
                if (gamesForPlayerCount.isEmpty()) {
                    continue;
                }

                List<String> section = new ArrayList<>();
                section.add(currentPlayerCount + " players");
                for (LobbyGameSummary game : gamesForPlayerCount) {
                    section.addAll(wrap("  " + game.gameName()
                            + " | lobby " + game.joinedPlayers() + "/" + game.playerCount()
                            + " | free seats " + game.availableSeats(), maxContentWidth));
                }
                sections.add(section);
            }
        }

        int contentWidth = sections.stream()
                .flatMap(List::stream)
                .mapToInt(this::visibleWidth)
                .max()
                .orElse(0);
        int width = Math.min(maxWidth, contentWidth + boxFixedContentWidth());
        int boxContentWidth = Math.max(0, width - boxFixedContentWidth());
        String padding = repeat(" ", BOX_HORIZONTAL_PADDING);
        List<String> lines = new ArrayList<>();
        lines.add("╔" + repeat("═", width - 2) + "╗");
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            if (sectionIndex > 0) {
                lines.add("╟" + repeat("─", width - 2) + "╢");
            }
            for (String line : sections.get(sectionIndex)) {
                lines.add("║" + padding + padRight(line, boxContentWidth) + padding + "║");
            }
        }
        lines.add("╚" + repeat("═", width - 2) + "╝");
        return lines;
    }

    /**
     * Returns the visible line slots required for game-size-bound structures.
     *
     * @param playerCount configured game size
     * @return at least one slot
     */
    private int playerSlots(int playerCount) {
        return Math.max(1, playerCount);
    }

    /**
     * Computes the fixed non-content width added by a double-bordered box.
     *
     * @return border width plus horizontal padding
     */
    private int boxFixedContentWidth() {
        return 2 + (BOX_HORIZONTAL_PADDING * 2);
    }

    /**
     * Creates the fixed-width game information box shown above the board.
     *
     * @param snapshot snapshot to describe
     * @param localNickname nickname of the local player, or null
     * @param boardWidth target width
     * @return game information box lines
     */
    private List<String> createGameInfoBox(GameSnapshot snapshot, String localNickname, int boardWidth) {
        List<String> body = List.of(
                "MESOS | " + snapshot.gameName()
                        + " | Round " + snapshot.currentRound()
                        + " | Era " + snapshot.currentEra()
                        + " | " + statusLabel(snapshot.status()),
                "Players in lobby: " + snapshot.joinedPlayers() + "/" + snapshot.playerCount(),
                sceneMessage(snapshot, localNickname)
        );
        return createAutoDoubleBox(fitLines(body, 3, boardWidth - boxFixedContentWidth()), boardWidth, null);
    }

    /**
     * Fits a list of lines to a fixed count and width.
     * Extra lines are represented with an ellipsis on the last output line;
     * missing lines are padded with blanks.
     *
     * @param lines source lines
     * @param targetLines number of output lines
     * @param width maximum line width
     * @return fitted line list
     * @throws NullPointerException if the source line list is null
     */
    private List<String> fitLines(List<String> lines, int targetLines, int width) {
        List<String> fitted = new ArrayList<>();
        for (String line : lines) {
            if (fitted.size() == targetLines) {
                break;
            }
            fitted.add(truncate(line, width));
        }
        if (fitted.size() == targetLines && lines.size() > targetLines) {
            fitted.set(targetLines - 1, truncate(fitted.getLast(), Math.max(0, width - 3)) + "...");
        }
        while (fitted.size() < targetLines) {
            fitted.add("");
        }
        return fitted;
    }

    /**
     * Merges multiple rendered columns row by row, padding shorter columns.
     *
     * @param columns columns to merge
     * @param gap text inserted between columns
     * @return merged lines, or an empty list when no columns are provided
     * @throws NullPointerException if the columns list or any column is null
     */
    private List<String> mergeColumns(List<List<String>> columns, String gap) {
        if (columns.isEmpty()) {
            return List.of();
        }

        List<Integer> widths = columns.stream().map(this::lineWidth).toList();
        int maxHeight = columns.stream().mapToInt(List::size).max().orElse(0);
        List<String> merged = new ArrayList<>();

        for (int row = 0; row < maxHeight; row++) {
            StringBuilder builder = new StringBuilder();
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                if (columnIndex > 0) {
                    builder.append(gap);
                }
                List<String> column = columns.get(columnIndex);
                String value = row < column.size() ? column.get(row) : "";
                builder.append(padRight(value, widths.get(columnIndex)));
            }
            merged.add(builder.toString());
        }
        return merged;
    }

    /**
     * Appends each line followed by the platform newline.
     *
     * @param builder destination builder
     * @param lines lines to append
     * @throws NullPointerException if the builder or line list is null
     */
    private void appendLines(StringBuilder builder, List<String> lines) {
        for (String line : lines) {
            builder.append(line).append(NEW_LINE);
        }
    }

    /**
     * Wraps text on whitespace within the requested width.
     * Null or blank input returns a single empty line; leading indentation is
     * preserved, and words longer than the width are split.
     *
     * @param text text to wrap
     * @param width maximum line width
     * @return wrapped lines
     */
    private List<String> wrap(String text, int width) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }

        List<String> wrapped = new ArrayList<>();
        int indentEnd = 0;
        while (indentEnd < text.length() && Character.isWhitespace(text.charAt(indentEnd))) {
            indentEnd++;
        }
        String indent = text.substring(0, indentEnd);
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder(indent);

        for (String word : words) {
            String separator = visibleWidth(line.toString()) > visibleWidth(indent) ? " " : "";
            if (visibleWidth(line + separator + word) <= width) {
                line.append(separator).append(word);
                continue;
            }

            if (visibleWidth(line.toString()) > visibleWidth(indent)) {
                wrapped.add(line.toString());
                line.setLength(0);
                line.append(indent);
            }

            String remainingWord = word;
            int wordWidth = Math.max(1, width - visibleWidth(indent));
            while (visibleWidth(indent + remainingWord) > width) {
                String chunk = truncatePlainToWidth(remainingWord, wordWidth);
                if (chunk.isEmpty()) {
                    chunk = remainingWord.substring(0, Character.charCount(remainingWord.codePointAt(0)));
                }
                wrapped.add(indent + chunk);
                remainingWord = remainingWord.substring(chunk.length());
            }
            line.append(remainingWord);
        }

        if (visibleWidth(line.toString()) > visibleWidth(indent) || wrapped.isEmpty()) {
            wrapped.add(line.toString());
        }
        return wrapped;
    }

    /**
     * Finds the maximum width among lines.
     *
     * @param lines lines to measure
     * @return longest line length, or zero for an empty list
     * @throws NullPointerException if the line list is null
     */
    private int lineWidth(List<String> lines) {
        return lines.stream().mapToInt(this::visibleWidth).max().orElse(0);
    }

    /**
     * Builds the compact type label shown at the top of a card.
     *
     * @param card card summary
     * @return uppercase card label with an ASCII category marker
     * @throws NullPointerException if the card is null
     */
    private String cardBadge(CardSummary card) {
        String label = card.type().toUpperCase(Locale.ROOT);
        if (card.event()) {
            return label + " [EVENT]";
        }
        if (card.foodCost() != null) {
            return label + " [BUI]";
        }
        return label + " [TRIBE]";
    }

    /**
     * Chooses the configured display color for a card.
     *
     * @param card card summary
     * @return hex color, or null for unknown card types
     * @throws NullPointerException if the card is null
     */
    private String cardColor(CardSummary card) {
        String type = card.type().toLowerCase(Locale.ROOT);
        if (card.foodCost() != null) {
            return COLOR_BUILDING;
        }
        if (card.event()) {
            if (type.contains("sustenance")) {
                return COLOR_SUSTENANCE;
            }
            if (type.contains("shamanic")) {
                return COLOR_SHAMANIC_RITUAL;
            }
            if (type.contains("cave") && type.contains("painting")) {
                return COLOR_CAVE_PAINTING;
            }
            if (type.contains("hunt")) {
                return COLOR_HUNT;
            }
            return null;
        }
        if (type.contains("inventor")) {
            return COLOR_INVENTOR;
        }
        if (type.contains("gatherer")) {
            return COLOR_GATHERER;
        }
        if (type.contains("shaman")) {
            return COLOR_SHAMAN;
        }
        if (type.contains("builder")) {
            return COLOR_BUILDER;
        }
        if (type.contains("artist")) {
            return COLOR_ARTIST;
        }
        if (type.contains("hunter")) {
            return COLOR_HUNTER;
        }
        return null;
    }

    /**
     * Formats an optional integer value for compact display.
     *
     * @param value value to format
     * @return the value text, or {@code "-"} when the value is null
     */
    private String valueOrDash(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    /**
     * Formats a player nickname with its color when available.
     *
     * @param nickname player nickname
     * @param color player color, or null
     * @return nickname alone when color is null, otherwise nickname plus color
     */
    private String playerLabel(String nickname, Object color) {
        if (color == null) {
            return nickname;
        }
        String label = nickname + " [" + color + "]";
        if ("WHITE".equals(String.valueOf(color).toUpperCase(Locale.ROOT))) {
            return colorizeWhitePlayer(label);
        }
        return makeBold(colorize(label, playerColor(color)));
    }

    /**
     * Chooses the configured display color for a player color.
     *
     * @param color player color
     * @return hex color, or null for unknown values
     */
    private String playerColor(Object color) {
        return switch (String.valueOf(color).toUpperCase(Locale.ROOT)) {
            case "BLACK" -> COLOR_PLAYER_BLACK;
            case "WHITE" -> COLOR_PLAYER_WHITE;
            case "BLUE" -> COLOR_PLAYER_BLUE;
            case "ORANGE" -> COLOR_PLAYER_ORANGE;
            case "YELLOW" -> COLOR_PLAYER_YELLOW;
            default -> null;
        };
    }

    /**
     * Applies bold text to already-styled or plain text.
     *
     * @param value text to style
     * @return bold text when supported, otherwise the original text
     */
    private String makeBold(String value) {
        if (System.console() == null) {
            return value;
        }
        String prefix = "\u001B[1m";
        return prefix + value.replace(ANSI_RESET, ANSI_RESET + prefix) + ANSI_RESET;
    }

    /**
     * Styles white player labels with bold text and a dark background.
     *
     * @param value player label to style
     * @return styled white player label when supported, otherwise the original label
     */
    private String colorizeWhitePlayer(String value) {
        if (System.console() == null) {
            return value;
        }
        String prefix = "\u001B[1m" + trueColor("38", COLOR_PLAYER_WHITE) + trueColor("48", COLOR_PLAYER_BLACK);
        return prefix + value.replace(ANSI_RESET, ANSI_RESET + prefix) + ANSI_RESET;
    }

    /**
     * Converts a game status into an English CLI label.
     *
     * @param status status to format
     * @return display label
     * @throws NullPointerException if the status is null
     */
    private String statusLabel(GameStatus status) {
        return switch (status) {
            case LOADING -> "Loading";
            case LOGIN -> "Lobby";
            case SETUP -> "Setup";
            case ROUND_OFFER -> "Offer tile choice";
            case ROUND_ACTIONS -> "Card choice";
            case ROUND_EVENTS -> "Round events";
            case ROUND_END -> "Round end";
            case FINAL_EVENTS -> "Final events";
            case END_GAME, END_SCREEN -> "Game over";
        };
    }

    /**
     * Formats an integer with an explicit plus sign for non-negative values.
     *
     * @param value value to format
     * @return signed value text
     */
    private String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    /**
     * Pads a value on the right after truncating it to the target width.
     *
     * @param value value to pad, treated as empty when null
     * @param width target width
     * @return padded value; an empty string when width is non-positive
     */
    private String padRight(String value, int width) {
        String safeValue = truncate(value == null ? "" : value, width);
        return safeValue + repeat(" ", width - visibleWidth(safeValue));
    }

    /**
     * Truncates text to the requested width.
     * Widths of three or less use a hard substring without ellipsis.
     *
     * @param value value to truncate
     * @param width maximum width
     * @return truncated text, or an empty string for null values or
     *         non-positive widths
     */
    private String truncate(String value, int width) {
        if (value == null || width <= 0) {
            return "";
        }
        if (visibleWidth(value) <= width) {
            return value;
        }
        String plainValue = stripAnsi(value);
        if (width <= 3) {
            return truncatePlainToWidth(plainValue, width);
        }
        return truncatePlainToWidth(plainValue, width - 3) + "...";
    }

    /**
     * Truncates uncolored text by visible terminal columns.
     *
     * @param value plain text to truncate
     * @param width target visible width
     * @return truncated text
     */
    private String truncatePlainToWidth(String value, int width) {
        StringBuilder builder = new StringBuilder();
        int usedWidth = 0;
        for (int index = 0; index < value.length();) {
            int codePoint = value.codePointAt(index);
            int codePointWidth = codePointWidth(codePoint);
            if (usedWidth + codePointWidth > width) {
                break;
            }
            builder.appendCodePoint(codePoint);
            usedWidth += codePointWidth;
            index += Character.charCount(codePoint);
        }
        return builder.toString();
    }

    /**
     * Applies ANSI truecolor only when running in a real console.
     *
     * @param value text to color
     * @param colorHex hex color in {@code #RRGGBB} format
     * @return colored text when supported, otherwise the original text
     */
    private String colorize(String value, String colorHex) {
        if (System.console() == null || colorHex == null) {
            return value;
        }
        try {
            String prefix = trueColor("38", colorHex);
            return prefix + value.replace(ANSI_RESET, ANSI_RESET + prefix) + ANSI_RESET;
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    /**
     * Creates one ANSI truecolor sequence from a known hex color.
     *
     * @param target {@code 38} for foreground or {@code 48} for background
     * @param colorHex color in {@code #RRGGBB} format
     * @return ANSI truecolor sequence
     */
    private String trueColor(String target, String colorHex) {
        int red = Integer.parseInt(colorHex.substring(1, 3), 16);
        int green = Integer.parseInt(colorHex.substring(3, 5), 16);
        int blue = Integer.parseInt(colorHex.substring(5, 7), 16);
        return "\u001B[" + target + ";2;" + red + ";" + green + ";" + blue + "m";
    }

    /**
     * Measures text without ANSI color escape sequences.
     *
     * @param value text to measure
     * @return visible character count
     */
    private int visibleWidth(String value) {
        return stripAnsi(value == null ? "" : value)
                .codePoints()
                .map(this::codePointWidth)
                .sum();
    }

    /**
     * Removes ANSI color escape sequences.
     *
     * @param value text to clean
     * @return text without ANSI escape sequences
     */
    private String stripAnsi(String value) {
        return ANSI_PATTERN.matcher(value).replaceAll("");
    }

    /**
     * Estimates terminal column width for one code point.
     *
     * @param codePoint Unicode code point
     * @return terminal column width
     */
    private int codePointWidth(int codePoint) {
        if (Character.getType(codePoint) == Character.NON_SPACING_MARK) {
            return 0;
        }
        if ((codePoint >= 0x1100 && codePoint <= 0x115F)
                || (codePoint >= 0x2329 && codePoint <= 0x232A)
                || (codePoint >= 0x2E80 && codePoint <= 0xA4CF)
                || (codePoint >= 0xAC00 && codePoint <= 0xD7A3)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFE10 && codePoint <= 0xFE19)
                || (codePoint >= 0xFE30 && codePoint <= 0xFE6F)
                || (codePoint >= 0xFF00 && codePoint <= 0xFF60)
                || (codePoint >= 0xFFE0 && codePoint <= 0xFFE6)
                || (codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)) {
            return 2;
        }
        return 1;
    }

    /**
     * Repeats a string a non-negative number of times.
     *
     * @param value string to repeat
     * @param times number of repetitions
     * @return repeated string, or an empty string when {@code times <= 0}
     */
    private String repeat(String value, int times) {
        if (times <= 0) {
            return "";
        }
        return value.repeat(times);
    }

    /**
     * Resolves the board width from system property, environment, terminal
     * query, or default width in that order.
     *
     * @return board width clamped to the supported range
     */
    private int resolveBoardWidth() {
        Integer configuredWidth = parsePositiveInt(System.getProperty("mesos.cli.width"));
        if (configuredWidth != null) {
            return clamp(configuredWidth, MIN_BOARD_WIDTH, MAX_BOARD_WIDTH);
        }

        Integer environmentWidth = parsePositiveInt(System.getenv("COLUMNS"));
        if (environmentWidth != null) {
            return clamp(environmentWidth, MIN_BOARD_WIDTH, MAX_BOARD_WIDTH);
        }

        Integer detectedWidth = detectTerminalWidth();
        if (detectedWidth != null) {
            return clamp(detectedWidth, MIN_BOARD_WIDTH, MAX_BOARD_WIDTH);
        }

        return DEFAULT_BOARD_WIDTH;
    }

    /**
     * Attempts to detect the terminal width using common shell commands.
     *
     * @return detected width, or null when detection fails
     */
    private Integer detectTerminalWidth() {
        Integer tputWidth = runTerminalQuery("tput cols");
        if (tputWidth != null) {
            return tputWidth;
        }

        return runTerminalQuery("stty size");
    }

    /**
     * Runs a terminal-width query command and parses its output.
     * The process is forcibly stopped after a short timeout, and failures are
     * treated as unavailable terminal width.
     *
     * @param command command to run
     * @return parsed positive width, or null when the command fails or cannot
     *         be parsed
     */
    private Integer runTerminalQuery(String command) {
        try {
            Process process = new ProcessBuilder("sh", "-lc", command)
                    .redirectErrorStream(true)
                    .start();

            if (!process.waitFor(250, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (output.isBlank()) {
                return null;
            }

            if ("stty size".equals(command)) {
                String[] tokens = output.split("\\s+");
                if (tokens.length < 2) {
                    return null;
                }
                return parsePositiveInt(tokens[1]);
            }
            return parsePositiveInt(output);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    /**
     * Parses a positive integer from text.
     *
     * @param rawValue raw text to parse
     * @return parsed positive integer, or null for null, blank, non-numeric, or
     *         non-positive values
     */
    private Integer parsePositiveInt(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Clamps a value between two bounds.
     *
     * @param value value to clamp
     * @param min lower bound
     * @param max upper bound
     * @return value constrained to the inclusive range
     */
    private int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }
}
