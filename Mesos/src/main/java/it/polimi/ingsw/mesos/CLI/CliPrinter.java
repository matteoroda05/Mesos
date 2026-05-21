package it.polimi.ingsw.mesos.CLI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private static final int MIN_OFFER_TILE_WIDTH = 18;
    private static final int MAX_OFFER_TILE_WIDTH = 24;
    private static final String COLUMN_GAP = "  ";

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
        builder.append(repeat("=", boardWidth)).append(NEW_LINE);
        builder.append("MESOS | ")
                .append(snapshot.gameName())
                .append(" | Round ")
                .append(snapshot.currentRound())
                .append(" | Era ")
                .append(snapshot.currentEra())
                .append(" | ")
                .append(statusLabel(snapshot.status()))
                .append(NEW_LINE);
        builder.append("Giocatori in lobby: ")
                .append(snapshot.joinedPlayers())
                .append("/")
                .append(snapshot.playerCount())
                .append(NEW_LINE);
        builder.append(sceneMessage(snapshot, localNickname)).append(NEW_LINE);
        builder.append(repeat("=", boardWidth)).append(NEW_LINE);

        appendPlayers(builder, snapshot.players(), localNickname, snapshot.finished(), snapshot.currentPlayerNickname());
        appendBoard(builder, snapshot, boardWidth);

        if (snapshot.finished()) {
            appendFinalRanking(builder, snapshot.players());
        }

        builder.append(NEW_LINE).append(commandsHint(snapshot, localNickname));
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
        int boardWidth = Math.min(resolveBoardWidth(), 120);
        StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE);
        builder.append(repeat("=", boardWidth)).append(NEW_LINE);
        builder.append("LOBBY MESOS").append(NEW_LINE);
        builder.append("Partite disponibili: ").append(lobbySnapshot.openGames().size()).append(NEW_LINE);
        builder.append(repeat("=", boardWidth)).append(NEW_LINE);

        if (lobbySnapshot.openGames().isEmpty()) {
            builder.append("Nessuna partita aperta al momento. Puoi crearne una nuova.")
                    .append(NEW_LINE);
        } else {
            for (int playerCount = Globals.MIN_NUM_PLAYERS; playerCount <= Globals.MAX_NUM_PLAYERS; playerCount++) {
                int currentPlayerCount = playerCount;
                List<LobbyGameSummary> gamesForPlayerCount = lobbySnapshot.openGames().stream()
                        .filter(game -> game.playerCount() == currentPlayerCount)
                        .toList();
                if (gamesForPlayerCount.isEmpty()) {
                    continue;
                }

                builder.append(NEW_LINE)
                        .append(currentPlayerCount)
                        .append(" giocatori")
                        .append(NEW_LINE);

                for (LobbyGameSummary game : gamesForPlayerCount) {
                    builder.append("  ")
                            .append(game.gameName())
                            .append(" | lobby ")
                            .append(game.joinedPlayers())
                            .append("/")
                            .append(game.playerCount())
                            .append(" | posti liberi ")
                            .append(game.availableSeats())
                            .append(NEW_LINE);
                }
            }
        }

        builder.append(NEW_LINE)
                .append("Scegli 'join' per entrare in una partita esistente oppure 'create' per crearne una nuova.")
                .append(NEW_LINE)
                .append("Quando fai join devi digitare solo il nome della partita.")
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
                "Comandi disponibili:",
                "  <invio>               aggiorna il tavolo",
                "  show | mostra         ristampa lo snapshot corrente",
                "  offer <indice>        scegli una tessera offerta",
                "  card top <indice>     prendi una carta dalla riga alta",
                "  card bottom <indice>  prendi una carta dalla riga bassa",
                "  wait [secondi]        aspetta un po' e aggiorna",
                "  help | aiuto          mostra questo aiuto",
                "  quit | esci | exit    chiude il client");
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
            return "Lobby aperta: premi Invio per aggiornare mentre gli altri giocatori entrano.";
        }
        if (snapshot.finished()) {
            return "Partita conclusa: usa 'quit' per uscire oppure premi Invio per rivedere il risultato.";
        }
        if (!Objects.equals(snapshot.currentPlayerNickname(), localNickname)) {
            return "Non e' il tuo turno: premi Invio per aggiornare o usa 'wait 2'.";
        }
        return switch (snapshot.status()) {
            case ROUND_OFFER -> "Tocca a te: scegli una tessera con 'offer <indice>'.";
            case ROUND_ACTIONS -> snapshot.passAvailable()
                    ? "Tocca a te: prendi una carta dalla riga alta con 'card top <indice>' oppure usa 'pass'."
                    : "Tocca a te: usa 'card top <indice>' o 'card bottom <indice>'.";
            default -> "Fase automatica in corso: premi Invio per aggiornare.";
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
                    .map(player -> "Tu sei " + playerLabel(player.nickname(), player.color()) + ". ")
                    .orElse("Tu sei " + localNickname + ". ");
        }

        if (!snapshot.started()) {
            return youAre + "La partita non e' ancora iniziata.";
        }
        if (snapshot.finished()) {
            return youAre + "La partita e' terminata. Controlla la classifica finale qui sotto.";
        }
        if (snapshot.currentPlayerNickname() == null) {
            return youAre + "Il tavolo e' in aggiornamento.";
        }
        if (Objects.equals(snapshot.currentPlayerNickname(), localNickname)) {
            return switch (snapshot.status()) {
                case ROUND_OFFER -> youAre + "E' il tuo turno: scegli la tessera offerta per preparare il round.";
                case ROUND_ACTIONS -> snapshot.passAvailable()
                        ? youAre + "E' il tuo turno: puoi prendere la carta extra opzionale dalla riga alta oppure usare 'pass'."
                        : youAre + "E' il tuo turno: raccogli le carte che la tua tessera ti permette di prendere.";
                default -> youAre + "E' il tuo turno, ma il gioco sta risolvendo una fase automatica.";
            };
        }

        String currentPlayer = playerLabel(snapshot.currentPlayerNickname(), snapshot.currentPlayerColor());
        if (snapshot.status() == GameStatus.ROUND_ACTIONS) {
            return youAre + "Sta giocando " + currentPlayer
                    + ". Scelte rimaste: alto=" + snapshot.remainingTopChoices()
                    + ", basso=" + snapshot.remainingBottomChoices() + ".";
        }
        return youAre + "Sta giocando " + currentPlayer + ".";
    }

    /**
     * Appends the player summary list.
     * Finished games are sorted by prestige, while active games keep the
     * snapshot order.
     *
     * @param builder destination builder
     * @param players player summaries to render
     * @param localNickname nickname to mark as local
     * @param finished whether the game is finished
     * @param currentPlayerNickname nickname to mark as active
     * @throws NullPointerException if the builder or player list is null
     */
    private void appendPlayers(
            StringBuilder builder,
            List<PlayerSummary> players,
            String localNickname,
            boolean finished,
            String currentPlayerNickname
    ) {
        builder.append(NEW_LINE).append("Clan in partita").append(NEW_LINE);
        if (players.isEmpty()) {
            builder.append("  Nessun giocatore presente.").append(NEW_LINE);
            return;
        }

        List<PlayerSummary> orderedPlayers = finished
                ? players.stream()
                .sorted(Comparator.comparingInt(PlayerSummary::prestigePoints).reversed())
                .toList()
                : players;

        for (PlayerSummary player : orderedPlayers) {
            builder.append("  ");
            if (player.nickname().equals(currentPlayerNickname)) {
                builder.append("> ");
            } else {
                builder.append("  ");
            }
            if (player.nickname().equals(localNickname)) {
                builder.append("* ");
            } else {
                builder.append("  ");
            }
            builder.append(playerLabel(player.nickname(), player.color()))
                    .append(" | cibo ")
                    .append(player.food())
                    .append(" | prestigio ")
                    .append(player.prestigePoints())
                    .append(" | tribu ")
                    .append(player.characterCount())
                    .append(" | edifici ")
                    .append(player.buildingCount())
                    .append(NEW_LINE);
        }
    }

    /**
     * Appends the board rows, turn order, and offer track to the output.
     *
     * @param builder destination builder
     * @param snapshot snapshot containing board data
     * @param boardWidth target board width
     * @throws NullPointerException if the builder, snapshot, or required board
     *                              lists are null
     */
    private void appendBoard(StringBuilder builder, GameSnapshot snapshot, int boardWidth) {
        builder.append(NEW_LINE).append(repeat("-", boardWidth)).append(NEW_LINE);
        builder.append("Tavolo di gioco").append(NEW_LINE);
        builder.append(repeat("-", boardWidth)).append(NEW_LINE);
        appendRow(builder, "RIGA ALTA", snapshot.topRow(), "top", boardWidth);
        appendCenterArea(builder, snapshot.turnOrder(), snapshot.offerTiles(), boardWidth);
        appendRow(builder, "RIGA BASSA", snapshot.bottomRow(), "bottom", boardWidth);
    }

    /**
     * Appends the turn-order and offer-track panels side by side when they fit,
     * otherwise stacks them vertically.
     *
     * @param builder destination builder
     * @param turnOrder turn-order spaces
     * @param offerTiles offer tiles
     * @param boardWidth available board width
     * @throws NullPointerException if the builder or either list is null
     */
    private void appendCenterArea(
            StringBuilder builder,
            List<TurnOrderSpaceSummary> turnOrder,
            List<OfferTileSummary> offerTiles,
            int boardWidth
    ) {
        List<String> turnOrderPanel = renderTurnOrderPanel(turnOrder);
        List<String> offerTrackPanel = renderOfferTrackPanel(offerTiles);

        if (lineWidth(turnOrderPanel) + lineWidth(offerTrackPanel) + 3 <= boardWidth) {
            appendLines(builder, mergeColumns(List.of(turnOrderPanel, offerTrackPanel), "   "));
            return;
        }

        appendLines(builder, turnOrderPanel);
        builder.append(NEW_LINE);
        appendLines(builder, offerTrackPanel);
    }

    /**
     * Renders the turn-order panel.
     *
     * @param turnOrder turn-order spaces to render
     * @return panel lines, including an empty message when no spaces exist
     * @throws NullPointerException if the turn-order list is null
     */
    private List<String> renderTurnOrderPanel(List<TurnOrderSpaceSummary> turnOrder) {
        List<String> body = new ArrayList<>();
        for (TurnOrderSpaceSummary space : turnOrder) {
            String occupant = space.occupantNickname() == null
                    ? "vuoto"
                    : playerLabel(space.occupantNickname(), space.occupantColor());
            body.add("[" + space.index() + "] bonus " + signed(space.foodBonus()) + " -> " + occupant);
        }
        if (body.isEmpty()) {
            body.add("Nessuno spazio turno disponibile.");
        }
        return createPanel("ORDINE DI TURNO", body, 34);
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
            return createPanel("TESSERE OFFERTA", List.of("Nessuna tessera disponibile."), 24);
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
        return createPanel("TESSERE OFFERTA", body, lineWidth(body) + 4);
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
        builder.append(NEW_LINE).append(title).append(NEW_LINE);
        if (cards.isEmpty()) {
            builder.append("  Nessuna carta visibile.").append(NEW_LINE);
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
    }

    /**
     * Appends the final ranking sorted by prestige descending.
     *
     * @param builder destination builder
     * @param players players to rank
     * @throws NullPointerException if the builder or player list is null
     */
    private void appendFinalRanking(StringBuilder builder, List<PlayerSummary> players) {
        builder.append(NEW_LINE).append("Classifica finale").append(NEW_LINE);
        List<PlayerSummary> ranking = players.stream()
                .sorted(Comparator.comparingInt(PlayerSummary::prestigePoints).reversed())
                .toList();
        for (int index = 0; index < ranking.size(); index++) {
            PlayerSummary player = ranking.get(index);
            builder.append("  ")
                    .append(index + 1)
                    .append(". ")
                    .append(playerLabel(player.nickname(), player.color()))
                    .append(" -> ")
                    .append(player.prestigePoints())
                    .append(" prestigio")
                    .append(NEW_LINE);
        }
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
        content.add(card.type());
        content.add(cardKind(card) + " | Era " + card.era());
        content.add("Costo " + valueOrDash(card.foodCost()) + " | PP " + valueOrDash(card.prestigePoints()));
        content.addAll(wrap(card.details().isBlank() ? "Nessun dettaglio aggiuntivo." : card.details(), cardWidth - 4));

        List<String> body = fitLines(content, CARD_BODY_HEIGHT - 1, cardWidth - 4);
        body.add("card " + rowToken + " " + card.selectionIndex());
        return createPanel(
                "[" + card.selectionIndex() + "]",
                body,
                cardWidth
        );
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
        return Math.max(1, Math.min(cardCount, Math.min(maxColumns, preferredColumns)));
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
        int targetWidth = 21;
        if (tilesInRow >= 5) {
            targetWidth = 18;
        } else if (tilesInRow == 4) {
            targetWidth = 19;
        } else if (tilesInRow <= 3) {
            targetWidth = 22;
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
        body.add("Alto  : " + tile.topPicks());
        body.add("Basso : " + tile.bottomPicks());
        body.add("Cibo  : " + signed(tile.foodGain()));
        body.add(tile.available()
                ? "Stato : Libera"
                : "Stato : " + playerLabel(tile.occupantNickname(), tile.occupantColor()));
        body.add("offer " + tile.index());
        return createPanel("[" + tile.index() + "/" + tile.id() + "]", body, tileWidth);
    }

    /**
     * Creates a bordered panel and pads each body line to the panel width.
     *
     * @param title panel title
     * @param body body lines to include
     * @param requestedWidth preferred panel width
     * @return panel lines with a width large enough for the title
     * @throws NullPointerException if the body list is null
     */
    private List<String> createPanel(String title, List<String> body, int requestedWidth) {
        int width = Math.max(requestedWidth, title.length() + 4);
        List<String> lines = new ArrayList<>();
        lines.add("+" + repeat("-", width - 2) + "+");
        lines.add("| " + padRight(title, width - 4) + " |");
        lines.add("+" + repeat("-", width - 2) + "+");
        for (String line : body) {
            lines.add("| " + padRight(line, width - 4) + " |");
        }
        lines.add("+" + repeat("-", width - 2) + "+");
        return lines;
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
     * Null or blank input returns a single empty line; words longer than the
     * width are truncated.
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
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (word.length() > width) {
                if (!line.isEmpty()) {
                    wrapped.add(line.toString());
                    line.setLength(0);
                }
                wrapped.add(truncate(word, width));
                continue;
            }

            if (line.isEmpty()) {
                line.append(word);
                continue;
            }

            if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                wrapped.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
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
        return lines.stream().mapToInt(String::length).max().orElse(0);
    }

    /**
     * Returns the broad display category for a card.
     *
     * @param card card summary
     * @return event, building, or tribe label
     * @throws NullPointerException if the card is null
     */
    private String cardKind(CardSummary card) {
        if (card.event()) {
            return "Evento";
        }
        if (card.foodCost() != null) {
            return "Edificio";
        }
        return "Tribu";
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
        return nickname + " [" + color + "]";
    }

    /**
     * Converts a game status into an Italian CLI label.
     *
     * @param status status to format
     * @return display label
     * @throws NullPointerException if the status is null
     */
    private String statusLabel(GameStatus status) {
        return switch (status) {
            case LOADING -> "Caricamento";
            case LOGIN -> "Lobby";
            case SETUP -> "Setup";
            case ROUND_OFFER -> "Scelta tessere";
            case ROUND_ACTIONS -> "Scelta carte";
            case ROUND_EVENTS -> "Eventi di round";
            case ROUND_END -> "Fine round";
            case FINAL_EVENTS -> "Eventi finali";
            case END_GAME, END_SCREEN -> "Fine partita";
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
        return safeValue + repeat(" ", width - safeValue.length());
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
        if (value.length() <= width) {
            return value;
        }
        if (width <= 3) {
            return value.substring(0, width);
        }
        return value.substring(0, width - 3) + "...";
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

        Integer sttyWidth = runTerminalQuery("stty size");
        if (sttyWidth != null) {
            return sttyWidth;
        }
        return null;
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
        return Math.max(min, Math.min(max, value));
    }
}
