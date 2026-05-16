package it.polimi.ingsw.mesos.CLI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameClientFactory;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;

public final class MesosCliClient {
    private final BufferedReader reader;
    private final CliPrinter printer;
    private final Object consoleLock;

    private String nickname;
    private String gameName;
    private GameSnapshot snapshot;

    /**
     * Creates a CLI client bound to standard input and a terminal printer.
     */
    public MesosCliClient() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.printer = new CliPrinter();
        this.consoleLock = new Object();
    }

    /**
     * Starts the CLI client.
     *
     * @param args ignored command-line arguments
     * @throws Exception if connection setup or interactive I/O fails
     */
    public static void main(String[] args) throws Exception {
        new MesosCliClient().run();
    }

    /**
     * Runs connection setup, nickname selection, and lobby/game interaction.
     * If nickname selection rejoins an active game, the lobby flow is skipped.
     *
     * @throws Exception if the client cannot complete required I/O
     */
    private void run() throws Exception {
        printBanner();

        try (GameClient client = connectWithRetry()) {
            if (!chooseNickname(client)) {
                lobbyLoop(client);
            }
        }
    }

    /**
     * Prompts for transport and host until a connection succeeds.
     *
     * @return connected game client
     * @throws IOException if reading prompt input fails
     */
    private GameClient connectWithRetry() throws IOException {
        while (true) {
            ConnectionType connectionType = promptConnectionType();
            String host = promptRequired("Host del server");

            try {
                GameClient client = GameClientFactory.connect(connectionType, host);
                String transport = connectionType.name().toLowerCase(Locale.ROOT);
                printLine("[ok] Connessione " + transport + " attiva verso " + host + ".");
                return client;
            } catch (IOException exception) {
                printLine("[error] Connessione fallita: " + describeConnectionError(exception));
                printLine("Controlla l'indirizzo del server e riprova.");
            }
        }
    }

    /**
     * Registers a nickname, handling duplicate-name retries and optional
     * rejoin of an existing disconnected session.
     *
     * @param client connected game client
     * @return true if the method already entered a rejoined game, false when
     *         the caller should continue to the lobby
     * @throws IOException if prompt input or network calls fail
     */
    private boolean chooseNickname(GameClient client) throws IOException {
        while (true) {
            String candidateNickname = promptRequired("Nickname");

            while (true) {
                GameOperationResult nicknameResult =
                        client.nickname(new NicknameRequest(candidateNickname));

                if (!nicknameResult.success()) {
                    if (nicknameResult.errorCode() == OperationErrorCode.PLAYER_NAME_TAKEN) {
                        candidateNickname = handleTakenName(candidateNickname, "Nickname");
                        continue;
                    }

                    printLine("[error] " + nicknameResult.message());
                    break;
                }

                nickname = candidateNickname;

                boolean rejoinAvailable = nicknameResult.rejoinAvailable();

                if (!rejoinAvailable) {
                    return false;
                }

                if (promptYesNo("Esiste una sessione disconnessa con questo nickname. Vuoi riconnetterti?")) {
                    GameOperationResult rejoinResult =
                            client.joinGame(new JoinGameRequest(null, nickname, true));

                    if (rejoinResult.success() && rejoinResult.snapshot() != null) {
                        handleResult(rejoinResult);
                        gameName = rejoinResult.snapshot().gameName();
                        enterGame(client);
                        return true;
                    } else if (rejoinResult.success() && rejoinResult.lobbySnapshot() != null) {
                        return false; //Go to lobby with recovered nickname
                    }

                    printLine("[error] " + rejoinResult.message());
                }

                nickname = null;
                break;
            }
        }
    }

    /**
     * Handles a duplicate nickname or game name by either appending a numeric
     * suffix or prompting for a replacement.
     *
     * @param nameBase name that was rejected
     * @param nameLabel label used in prompts
     * @return next candidate name
     * @throws IOException if prompt input fails
     */
    private String handleTakenName(String nameBase, String nameLabel) throws IOException {
        if (promptYesNo("Il " + nameLabel + " e' gia' in uso. Vuoi mantenerlo con un suffisso automatico")) {
            int suffix = 1;
            while (true) {
                String candidate = appendNumericSuffix(nameBase, suffix++);
                printLine("Provo automaticamente con il " + nameLabel + " '" + candidate + "'.");
                return candidate;
            }
        }

        return promptRequired(nameLabel);
    }

    /**
     * Repeatedly shows the lobby and prompts the user to join or create a game
     * until a game is entered.
     *
     * @param client connected game client
     * @throws IOException if prompt input or network calls fail
     */
    private void lobbyLoop(GameClient client) throws IOException {
        while (true) {
            LobbySnapshot lobby = fetchLobby(client);
            boolean createGame = promptLobbyAction();
            String requestedGameName = promptRequired("Nome partita");

            if (createGame
                    ? handleCreateFlow(client, requestedGameName)
                    : handleJoinFlow(client, lobby, requestedGameName)) {
                return;
            }
        }
    }

    /**
     * Handles the create-game path and joins the newly created game.
     *
     * @param client connected game client
     * @param requestedGameName requested game name
     * @return true if the client entered the created game, false if creation
     *         was abandoned
     * @throws IOException if prompt input or network calls fail
     */
    private boolean handleCreateFlow(GameClient client, String requestedGameName) throws IOException {
        int playerCount = promptPlayerCount();
        String createdGameName = createGameWithUniqueName(client, requestedGameName, playerCount);
        if (createdGameName == null) {
            return false;
        }

        return joinSelectedGame(client, createdGameName, false);
    }

    /**
     * Handles the join-game path and optionally warns when the requested game
     * is not present in the latest lobby snapshot.
     *
     * @param client connected game client
     * @param lobby latest lobby snapshot
     * @param requestedGameName requested game name
     * @return true if the client entered a game, false otherwise
     * @throws IOException if prompt input or network calls fail
     */
    private boolean handleJoinFlow(GameClient client, LobbySnapshot lobby, String requestedGameName) throws IOException {
        if (lobby.openGames().stream().noneMatch(game -> game.gameName().equals(requestedGameName))) {
            printLine("La partita '" + requestedGameName + "' non compare tra quelle disponibili.");
        }

        return joinSelectedGame(client, requestedGameName, true);
    }

    /**
     * Attempts to join a selected game, optionally offering to create it when
     * the server reports that it does not exist.
     *
     * @param client connected game client
     * @param requestedGameName game name to join
     * @param offerCreateFallback whether to offer creation on missing game
     * @return true if the client entered a game, false if the flow should
     *         return to the lobby
     * @throws IOException if prompt input or network calls fail
     */
    private boolean joinSelectedGame( GameClient client, String requestedGameName, boolean offerCreateFallback) throws IOException {
        String selectedGameName = requestedGameName;

        while (true) {
            GameOperationResult joinResult = client.joinGame(new JoinGameRequest(selectedGameName, nickname));
            if (joinResult.success()) {
                gameName = selectedGameName;
                handleResult(joinResult);
                enterGame(client);
                return true;
            }

            printLine("[error] " + joinResult.message());
            switch (joinResult.errorCode()) {
                case GAME_NOT_FOUND -> {
                    if (offerCreateFallback
                            && promptYesNo("La partita non esiste. Vuoi crearla con questo nome")) {
                        int playerCount = promptPlayerCount();
                        String createdGameName = createGameWithUniqueName(client, selectedGameName, playerCount);
                        if (createdGameName == null) {
                            return false;
                        }
                        selectedGameName = createdGameName;
                        continue;
                    }
                    return false;
                }
                case GAME_FULL -> {
                    printLine("Scegli un'altra partita dalla lobby.");
                    return false;
                }
                default -> {
                    return false;
                }
            }
        }
    }

    /**
     * Creates a game, retrying with new names when the requested name is taken.
     *
     * @param client connected game client
     * @param requestedGameName initial game name
     * @param playerCount requested player count
     * @return created game name, or null if creation fails for a non-duplicate
     *         reason
     * @throws IOException if prompt input or network calls fail
     */
    private String createGameWithUniqueName(GameClient client, String requestedGameName, int playerCount) throws IOException {
        String candidateGameName = requestedGameName;

        while (true) {
            GameOperationResult createResult = client.createGame(new CreateGameRequest(candidateGameName, playerCount));
            if (createResult.success()) {
                gameName = candidateGameName;
                handleResult(createResult);
                return candidateGameName;
            }

            printLine("[error] " + createResult.message());
            if (createResult.errorCode() != OperationErrorCode.GAME_NAME_TAKEN) {
                return null;
            }

            candidateGameName = handleTakenName(candidateGameName, "Nome partita");
        }
    }

    /**
     * Subscribes to game updates, enters the in-game command loop, then
     * unsubscribes when the loop exits.
     *
     * @param client connected game client
     * @throws IOException if subscription, unsubscription, or command I/O fails
     */
    private void enterGame(GameClient client) throws IOException {
        handleResult(client.subscribe(new SubscribeGameRequest(gameName, nickname), this::handleRemoteUpdate), false);
        mainLoop(client);
        client.unsubscribe(new UnsubscribeGameRequest(gameName, nickname));
    }

    /**
     * Fetches and prints the lobby snapshot.
     *
     * @param client connected game client
     * @return lobby snapshot, or an empty lobby snapshot when the response is
     *         unsuccessful or missing lobby data
     * @throws IOException if the network call fails
     */
    private LobbySnapshot fetchLobby(GameClient client) throws IOException {
        GameOperationResult lobbyResult = client.getLobby();
        if (!lobbyResult.success()) {
            printLine("[error] " + lobbyResult.message());
            return new LobbySnapshot(null);
        }

        LobbySnapshot lobbySnapshot = lobbyResult.lobbySnapshot();
        if (lobbySnapshot == null) {
            return new LobbySnapshot(null);
        }
        printLine(printer.renderLobby(lobbySnapshot));
        return lobbySnapshot;
    }

    /**
     * Prompts until the user chooses whether to join or create a game.
     *
     * @return true for create, false for join
     * @throws IOException if prompt input fails
     */
    private boolean promptLobbyAction() throws IOException {
        while (true) {
            String raw = promptRequired("Vuoi join o create").toLowerCase(Locale.ROOT);
            switch (raw) {
                case "join", "j", "entra" -> {
                    return false;
                }
                case "create", "c", "crea" -> {
                    return true;
                }
                default -> printLine("Scegli 'join' oppure 'create'.");
            }
        }
    }

    /**
     * Appends a numeric suffix to a base name.
     *
     * @param baseName base name
     * @param suffix numeric suffix
     * @return suffixed name
     */
    private String appendNumericSuffix(String baseName, int suffix) {
        return baseName + "-" + suffix;
    }

    /**
     * Produces a readable connection error.
     *
     * @param exception connection exception
     * @return exception message, or the exception class name when the message
     *         is null or blank
     */
    private String describeConnectionError(IOException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    /**
     * Runs the in-game command prompt until the user quits.
     * Empty commands are rejected in this loop even though the help text
     * describes refresh behavior for Enter.
     *
     * @param client connected game client
     * @throws IOException if command input or network calls fail
     */
    private void mainLoop(GameClient client) throws IOException {
        printSnapshotIfAvailable();

        while (true) {
            String command = promptRaw("mesos> ").trim();
            if (command.isEmpty()) {
                printLine("Comando non puo' essere vuoto.");
                continue;
            }
            if (command.equalsIgnoreCase("refresh") || command.equalsIgnoreCase("poll")) {
                refreshSnapshot(client);
                printSnapshotIfAvailable();
                continue;
            }
            if (command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("esci")) {
                printLine("Chiusura del client CLI.");
                return;
            }
            if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("aiuto")) {
                printLine(printer.renderHelp());
                continue;
            }
            if (command.equalsIgnoreCase("show")
                    || command.equalsIgnoreCase("status")
                    || command.equalsIgnoreCase("mostra")
                    || command.equalsIgnoreCase("stato")) {
                printSnapshotIfAvailable();
                continue;
            }
            if (command.toLowerCase(Locale.ROOT).startsWith("wait")
                    || command.toLowerCase(Locale.ROOT).startsWith("attendi")) {
                handleWait(command);
                continue;
            }

            if (snapshot == null) {
                printLine("Nessuno snapshot disponibile al momento. Premi Invio per aggiornare.");
                continue;
            }

            try {
                handleGameCommand(client, command);
            } catch (IllegalArgumentException exception) {
                printLine("Comando non valido: " + exception.getMessage());
                printLine(printer.renderHelp());
            }
        }
    }

    /**
     * Parses and dispatches one in-game command.
     *
     * @param client connected game client
     * @param command raw command text
     * @throws IOException if the network command fails
     * @throws IllegalArgumentException if the command is unknown or malformed
     */
    private void handleGameCommand(GameClient client, String command) throws IOException {
        String[] tokens = command.split("\\s+");
        if (tokens.length == 0) {
            return;
        }

        switch (tokens[0].toLowerCase(Locale.ROOT)) {
            case "offer", "offerta" -> {
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("usa: offer <indice>");
                }
                int offerIndex = parseInt(tokens[1], "l'indice offerta");
                handleResult(client.chooseOffer(new ChooseOfferRequest(gameName, nickname, offerIndex)));
            }
            case "card", "carta" -> {
                if (tokens.length != 3) {
                    throw new IllegalArgumentException("usa: card top <indice> oppure card bottom <indice>");
                }
                boolean topRow = parseRow(tokens[1]);
                int cardIndex = parseInt(tokens[2], "l'indice carta");
                handleResult(client.chooseCard(new ChooseCardRequest(gameName, nickname, cardIndex, topRow)));
            }
            case "pass", "passa" -> {
                if (tokens.length != 1) {
                    throw new IllegalArgumentException("usa: pass");
                }
                handleResult(client.pass(new PassRequest(gameName, nickname)));
            }
            default -> throw new IllegalArgumentException("comando sconosciuto '" + tokens[0] + "'");
        }
    }

    /**
     * Parses a market-row token.
     *
     * @param token row token entered by the user
     * @return true for the top row, false for the bottom row
     * @throws IllegalArgumentException if the token is not a supported row name
     */
    private boolean parseRow(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "top", "alto" -> true;
            case "bottom", "basso" -> false;
            default -> throw new IllegalArgumentException("la riga deve essere 'top' oppure 'bottom'");
        };
    }

    /**
     * Parses an integer command argument.
     *
     * @param token raw numeric token
     * @param label label used in the validation error
     * @return parsed integer
     * @throws IllegalArgumentException if the token is not numeric
     */
    private int parseInt(String token, String label) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " deve essere un numero");
        }
    }

    /**
     * Handles a wait command by sleeping for the requested number of seconds.
     * Missing seconds default to two.
     *
     * @param command raw wait command
     * @throws IllegalArgumentException if the seconds argument is non-numeric
     *                                  or negative
     */
    private void handleWait(String command) {
        String[] tokens = command.split("\\s+");
        int seconds = 2;
        if (tokens.length == 2) {
            seconds = parseInt(tokens[1], "i secondi");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("i secondi non possono essere negativi");
        }
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Requests a fresh game snapshot from the server and stores it if present.
     *
     * @param client connected game client
     * @throws IOException if the network call fails
     */
    private void refreshSnapshot(GameClient client) throws IOException {
        handleResult(client.getSnapshot(new SnapshotRequest(gameName)), false);
    }

    /**
     * Handles a game operation result and prints its message.
     *
     * @param result operation result, ignored when null
     */
    private void handleResult(GameOperationResult result) {
        handleResult(result, true);
    }

    /**
     * Handles a game operation result, optionally printing successful messages,
     * and updates the cached snapshot when the result contains one.
     *
     * @param result operation result, ignored when null
     * @param printMessage whether to print successful messages; errors are
     *                     always printed
     */
    private void handleResult(GameOperationResult result, boolean printMessage) {
        if (result == null) {
            return;
        }
        if (printMessage || !result.success()) {
            printLine((result.success() ? "[ok] " : "[error] ") + result.message());
        }
        if (result.snapshot() != null) {
            snapshot = result.snapshot();
        }
    }

    /**
     * Handles an asynchronous server view update.
     * Null updates are ignored.
     *
     * @param update remote update payload
     */
    private void handleRemoteUpdate(GameViewUpdate update) {
        if (update == null) {
            return;
        }
        snapshot = update.snapshot();
        synchronized (consoleLock) {
            System.out.println();
            System.out.println("[update] " + update.message());
            System.out.println(printer.renderSnapshot(snapshot, nickname));
            System.out.print("mesos> ");
            System.out.flush();
        }
    }

    /**
     * Prints the cached snapshot when one is available.
     */
    private void printSnapshotIfAvailable() {
        if (snapshot == null) {
            return;
        }
        printLine(printer.renderSnapshot(snapshot, nickname));
    }

    /**
     * Prints one line while holding the console lock.
     *
     * @param text text to print, forwarded to {@link System#out}
     */
    private void printLine(String text) {
        synchronized (consoleLock) {
            System.out.println(text);
        }
    }

    /**
     * Prints the CLI startup banner.
     */
    private void printBanner() {
        System.out.println("MESOS CLI");
        System.out.println("Avvia il server in un terminale separato con MesosServerLauncher.");
        System.out.println("Scegli socket o rmi, poi inserisci solo l'indirizzo del server.");
        System.out.println("Il client usa automaticamente le porte di default e, per RMI, il servizio MesosGameService.");
        System.out.println("Dentro il client usa 'help' oppure 'aiuto' per vedere tutti i comandi.");
        System.out.println();
    }

    /**
     * Prompts until the user chooses socket or RMI transport.
     *
     * @return selected connection type
     * @throws IOException if prompt input fails
     */
    private ConnectionType promptConnectionType() throws IOException {
        while (true) {
            String raw = promptRequired("Tipo connessione (socket/rmi)").toLowerCase(Locale.ROOT);
            switch (raw) {
                case "socket", "s" -> {
                    return ConnectionType.SOCKET;
                }
                case "rmi", "r" -> {
                    return ConnectionType.RMI;
                }
                default -> printLine("Scegli 'socket' oppure 'rmi'.");
            }
        }
    }

    /**
     * Prompts until a non-blank value is entered.
     *
     * @param label prompt label
     * @return trimmed non-blank input
     * @throws IOException if input cannot be read
     */
    private String promptRequired(String label) throws IOException {
        while (true) {
            String value = promptRaw(label + ": ").trim();
            if (!value.isBlank()) {
                return value;
            }
            printLine(label + " non puo' essere vuoto.");
        }
    }

    /**
     * Prompts until a valid integer is entered.
     *
     * @param label prompt label
     * @return parsed integer
     * @throws IOException if input cannot be read
     */
    private int promptInt(String label) throws IOException {
        while (true) {
            String value = promptRequired(label);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                printLine(label + " deve essere un numero valido.");
            }
        }
    }

    /**
     * Prompts until a player count within the supported range is entered.
     *
     * @return valid player count
     * @throws IOException if input cannot be read
     */
    private int promptPlayerCount() throws IOException {
        while (true) {
            int value = promptInt("Numero giocatori");
            if (value >= Globals.MIN_NUM_PLAYERS && value <= Globals.MAX_NUM_PLAYERS) {
                return value;
            }
            printLine("Numero giocatori deve essere compreso tra "
                    + Globals.MIN_NUM_PLAYERS + " e " + Globals.MAX_NUM_PLAYERS + ".");
        }
    }

    /**
     * Prompts until the user answers yes/no.
     * Blank answers are rejected.
     *
     * @param label prompt label
     * @return true for yes/y, false for no/n
     * @throws IOException if input cannot be read
     */
    private boolean promptYesNo(String label) throws IOException {
        while (true) {
            String value = promptRaw(label + ": ").trim().toLowerCase(Locale.ROOT);
            if (value.isBlank()) {
                printLine(label + " non puo' essere vuoto.");
                continue;
            }
            if (value.equals("y") || value.equals("yes")) {
                return true;
            }
            if (value.equals("n") || value.equals("no")) {
                return false;
            }
            printLine("Rispondi con yes/no oppure y/n.");
        }
    }

    /**
     * Prints a prompt and reads one raw line from standard input.
     *
     * @param prompt prompt text
     * @return line read from the input stream
     * @throws IOException if the input stream is closed or cannot be read
     */
    private String promptRaw(String prompt) throws IOException {
        synchronized (consoleLock) {
            System.out.print(prompt);
        }
        String value = reader.readLine();
        if (value == null) {
            throw new IOException("Input stream closed");
        }
        return value;
    }
}
