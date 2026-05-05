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
            appendFinalRanking(builder, snapshot.players(), snapshot.winnerNickname());
        }

        builder.append(NEW_LINE).append(commandsHint(snapshot, localNickname));
        return builder.toString();
    }

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

    private String commandsHint(GameSnapshot snapshot, String localNickname) {
        if (!snapshot.started()) {
            return "Lobby aperta: premi Invio per aggiornare mentre gli altri giocatori entrano.";
        }
        if (snapshot.finished()) {
            return "Partita conclusa: usa 'quit' per uscire oppure premi Invio per rivedere il risultato.";
        }
        if (snapshot.status() == GameStatus.SUSPENDED) {
            return "Partita sospesa: premi Invio per aggiornare o usa 'wait 2'.";
        }
        if (!Objects.equals(snapshot.currentPlayerNickname(), localNickname)) {
            return "Non e' il tuo turno: premi Invio per aggiornare o usa 'wait 2'.";
        }
        return switch (snapshot.status()) {
            case ROUND_OFFER -> "Tocca a te: scegli una tessera con 'offer <indice>'.";
            case ROUND_ACTIONS -> "Tocca a te: usa 'card top <indice>' o 'card bottom <indice>'.";
            default -> "Fase automatica in corso: premi Invio per aggiornare.";
        };
    }

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
            if (snapshot.winnerNickname() != null) {
                return youAre + "La partita e' terminata. Vince " + snapshot.winnerNickname() + ".";
            }
            return youAre + "La partita e' terminata. Controlla la classifica finale qui sotto.";
        }
        if (snapshot.status() == GameStatus.SUSPENDED) {
            return youAre + "La partita e' sospesa in attesa che un altro giocatore si riconnetta.";
        }
        if (snapshot.currentPlayerNickname() == null) {
            return youAre + "Il tavolo e' in aggiornamento.";
        }
        if (Objects.equals(snapshot.currentPlayerNickname(), localNickname)) {
            return switch (snapshot.status()) {
                case ROUND_OFFER -> youAre + "E' il tuo turno: scegli la tessera offerta per preparare il round.";
                case ROUND_ACTIONS -> youAre + "E' il tuo turno: raccogli le carte che la tua tessera ti permette di prendere.";
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
                    .append(player.online() ? " | online" : " | offline")
                    .append(NEW_LINE);
        }
    }

    private void appendBoard(StringBuilder builder, GameSnapshot snapshot, int boardWidth) {
        builder.append(NEW_LINE).append(repeat("-", boardWidth)).append(NEW_LINE);
        builder.append("Tavolo di gioco").append(NEW_LINE);
        builder.append(repeat("-", boardWidth)).append(NEW_LINE);
        appendRow(builder, "RIGA ALTA", snapshot.topRow(), "top", boardWidth);
        appendCenterArea(builder, snapshot.turnOrder(), snapshot.offerTiles(), boardWidth);
        appendRow(builder, "RIGA BASSA", snapshot.bottomRow(), "bottom", boardWidth);
    }

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

    private void appendFinalRanking(StringBuilder builder, List<PlayerSummary> players, String winnerNickname) {
        if (winnerNickname != null) {
            builder.append(NEW_LINE).append("Vincitore: ").append(winnerNickname).append(NEW_LINE);
        }
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

    private int preferredCardsPerShelf(int cardCount, int boardWidth) {
        int maxColumns = Math.max(1, (boardWidth + COLUMN_GAP.length()) / (MIN_CARD_WIDTH + COLUMN_GAP.length()));
        int preferredColumns = Math.max(1, (boardWidth + COLUMN_GAP.length()) / (PREFERRED_CARD_WIDTH + COLUMN_GAP.length()));
        return Math.max(1, Math.min(cardCount, Math.min(maxColumns, preferredColumns)));
    }

    private int cardWidthFor(int cardsPerShelf, int boardWidth) {
        int availableWidth = boardWidth - ((cardsPerShelf - 1) * COLUMN_GAP.length());
        int computedWidth = availableWidth / cardsPerShelf;
        return clamp(computedWidth, MIN_CARD_WIDTH, MAX_CARD_WIDTH);
    }

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

    private void appendLines(StringBuilder builder, List<String> lines) {
        for (String line : lines) {
            builder.append(line).append(NEW_LINE);
        }
    }

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

    private int lineWidth(List<String> lines) {
        return lines.stream().mapToInt(String::length).max().orElse(0);
    }

    private String cardKind(CardSummary card) {
        if (card.event()) {
            return "Evento";
        }
        if (card.foodCost() != null) {
            return "Edificio";
        }
        return "Tribu";
    }

    private String valueOrDash(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String playerLabel(String nickname, Object color) {
        if (color == null) {
            return nickname;
        }
        return nickname + " [" + color + "]";
    }

    private String statusLabel(GameStatus status) {
        return switch (status) {
            case LOADING -> "Caricamento";
            case LOGIN -> "Lobby";
            case SETUP -> "Setup";
            case ROUND_OFFER -> "Scelta tessere";
            case ROUND_ACTIONS -> "Scelta carte";
            case SUSPENDED -> "Sospesa";
            case ROUND_EVENTS -> "Eventi di round";
            case ROUND_END -> "Fine round";
            case FINAL_EVENTS -> "Eventi finali";
            case END_GAME, END_SCREEN -> "Fine partita";
        };
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    private String padRight(String value, int width) {
        String safeValue = truncate(value == null ? "" : value, width);
        return safeValue + repeat(" ", width - safeValue.length());
    }

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

    private String repeat(String value, int times) {
        if (times <= 0) {
            return "";
        }
        return value.repeat(times);
    }

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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
