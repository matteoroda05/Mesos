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
    private enum GameLoopExit {
        QUIT_CLIENT,
        RETURN_TO_LOBBY
    }

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
            String host = promptRequired("Server host");

            try {
                GameClient client = GameClientFactory.connect(connectionType, host);
                String transport = connectionType.name().toLowerCase(Locale.ROOT);
                printLine("[ok] " + transport + " connection active to " + host + ".");
                return client;
            } catch (IOException exception) {
                printLine("[error] Connection failed: " + describeConnectionError(exception));
                printLine("Check the server address and try again.");
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

                if (promptYesNo("A disconnected session exists for this nickname. Do you want to reconnect?")) {
                    GameOperationResult rejoinResult =
                            client.joinGame(new JoinGameRequest(null, nickname, true));

                    if (rejoinResult.success() && rejoinResult.snapshot() != null) {
                        handleResult(rejoinResult);
                        gameName = rejoinResult.snapshot().gameName();
                        return enterGame(client) == GameLoopExit.QUIT_CLIENT;
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
        if (promptYesNo("The " + nameLabel + " is already in use. Do you want to keep it with an automatic suffix")) {
            int suffix = 1;
            while (true) {
                String candidate = appendNumericSuffix(nameBase, suffix++);
                printLine("Trying automatically with " + nameLabel + " '" + candidate + "'.");
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
            Boolean createGame = promptLobbyAction();
            if (createGame == null) {
                continue;
            }

            String requestedGameName;
            if (createGame) {
                requestedGameName = promptCreatableGameName();
            } else {
                while (true) {
                    requestedGameName = promptRequired("Game name");
                    if (!isRefreshCommand(requestedGameName)) {
                        break;
                    }
                    lobby = fetchLobby(client);
                }
            }

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
     * @return true if the client should quit after the created game, false if
     *         creation was abandoned or the client should return to the lobby
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
     * @return true if the client should quit after the joined game, false
     *         otherwise
     * @throws IOException if prompt input or network calls fail
     */
    private boolean handleJoinFlow(GameClient client, LobbySnapshot lobby, String requestedGameName) throws IOException {
        if (lobby.openGames().stream().noneMatch(game -> game.gameName().equals(requestedGameName))) {
            printLine("Game '" + requestedGameName + "' does not appear among the available games.");
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
     * @return true if the client should quit after the selected game, false if
     *         the flow should return to the lobby
     * @throws IOException if prompt input or network calls fail
     */
    private boolean joinSelectedGame( GameClient client, String requestedGameName, boolean offerCreateFallback) throws IOException {
        String selectedGameName = requestedGameName;

        while (true) {
            GameOperationResult joinResult = client.joinGame(new JoinGameRequest(selectedGameName, nickname));
            if (joinResult.success()) {
                gameName = selectedGameName;
                handleResult(joinResult);
                return enterGame(client) == GameLoopExit.QUIT_CLIENT;
            }

            printLine("[error] " + joinResult.message());
            switch (joinResult.errorCode()) {
                case GAME_NOT_FOUND -> {
                    if (offerCreateFallback
                            && promptYesNo("The game does not exist. Do you want to create it with this name")) {
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
                    printLine("Choose another game from the lobby.");
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
            while (isRefreshCommand(candidateGameName)) {
                printLine("The game name 'refresh' is reserved for lobby refresh.");
                candidateGameName = promptCreatableGameName();
            }

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

            candidateGameName = handleTakenName(candidateGameName, "game name");
        }
    }

    /**
     * Subscribes to game updates, enters the in-game command loop, then
     * unsubscribes when the loop exits.
     *
     * @param client connected game client
     * @return selected exit action after the in-game loop
     * @throws IOException if subscription, unsubscription, or command I/O fails
     */
    private GameLoopExit enterGame(GameClient client) throws IOException {
        handleResult(client.subscribe(new SubscribeGameRequest(gameName, nickname), this::handleRemoteUpdate), false);
        try {
            return mainLoop(client);
        } finally {
            client.unsubscribe(new UnsubscribeGameRequest(gameName, nickname));
        }
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
     * Prompts until the user chooses whether to join, create, or refresh the lobby.
     *
     * @return true for create, false for join, null for refresh
     * @throws IOException if prompt input fails
     */
    private Boolean promptLobbyAction() throws IOException {
        while (true) {
            String raw = promptRequired("Do you want to join or create");
            if (isRefreshCommand(raw)) {
                return null;
            }

            switch (raw.toLowerCase(Locale.ROOT)) {
                case "join", "j", "entra" -> {
                    return false;
                }
                case "create", "c", "crea" -> {
                    return true;
                }
                default -> printLine("Choose 'join' or 'create'. Type 'refresh' or 'r' to refresh the lobby.");
            }
        }
    }

    /**
     * Prompts for a game name that can be used to create a new game.
     *
     * @return non-reserved game name
     * @throws IOException if prompt input fails
     */
    private String promptCreatableGameName() throws IOException {
        while (true) {
            String requestedGameName = promptRequired("Game name");
            if (!isRefreshCommand(requestedGameName)) {
                return requestedGameName;
            }
            printLine("The game name 'refresh' is reserved for lobby refresh.");
        }
    }

    /**
     * Checks whether the input is a refresh command.
     *
     * @param value raw prompt value
     * @return true for refresh/r, ignoring case and surrounding spaces
     */
    private boolean isRefreshCommand(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("refresh") || normalized.equals("r");
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
     * @return selected exit action
     * @throws IOException if command input or network calls fail
     */
    private GameLoopExit mainLoop(GameClient client) throws IOException {
        printSnapshotIfAvailable();

        while (true) {
            if (snapshot != null && snapshot.finished()) {
                return promptFinishedGameExit();
            }

            String command = promptRaw("mesos> ").trim();
            if (snapshot != null && snapshot.finished()) {
                return promptFinishedGameExit();
            }
            if (command.isEmpty()) {
                printLine("Command cannot be empty.");
                continue;
            }
            if (isRefreshCommand(command) || command.equalsIgnoreCase("poll")) {
                refreshSnapshot(client);
                printSnapshotIfAvailable();
                continue;
            }
            if (command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("esci")) {
                printLine("Closing the CLI client.");
                return GameLoopExit.QUIT_CLIENT;
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
                printLine("No snapshot is currently available. Press Enter to refresh.");
                continue;
            }

            try {
                handleGameCommand(client, command);
            } catch (IllegalArgumentException exception) {
                printLine("Invalid command: " + exception.getMessage());
                printLine(printer.renderHelp());
            }
        }
    }

    /**
     * Prompts for the next action when the current game has finished.
     *
     * @return selected exit action
     * @throws IOException if prompt input fails
     */
    private GameLoopExit promptFinishedGameExit() throws IOException {
        if (promptYesNo("The game is over. Do you want to return to the lobby for another game")) {
            gameName = null;
            snapshot = null;
            return GameLoopExit.RETURN_TO_LOBBY;
        }

        printLine("Closing the CLI client.");
        return GameLoopExit.QUIT_CLIENT;
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
                    throw new IllegalArgumentException("use: offer <index>");
                }
                int offerIndex = parseInt(tokens[1], "the offer index");
                handleResult(client.chooseOffer(new ChooseOfferRequest(gameName, nickname, offerIndex)));
            }
            case "card", "carta" -> {
                if (tokens.length != 3) {
                    throw new IllegalArgumentException("use: card top <index> or card bottom <index>");
                }
                boolean topRow = parseRow(tokens[1]);
                int cardIndex = parseInt(tokens[2], "the card index");
                handleResult(client.chooseCard(new ChooseCardRequest(gameName, nickname, cardIndex, topRow)));
            }
            case "pass", "passa" -> {
                if (tokens.length != 1) {
                    throw new IllegalArgumentException("use: pass");
                }
                handleResult(client.pass(new PassRequest(gameName, nickname)));
            }
            default -> throw new IllegalArgumentException("unknown command '" + tokens[0] + "'");
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
            default -> throw new IllegalArgumentException("the row must be 'top' or 'bottom'");
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
            throw new IllegalArgumentException(label + " must be a number");
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
            seconds = parseInt(tokens[1], "seconds");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds cannot be negative");
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
        System.out.println("Start the server in a separate terminal with MesosServerLauncher.");
        System.out.println("Choose socket or rmi, then enter only the server address.");
        System.out.println("The client automatically uses the default ports and, for RMI, the MesosGameService service.");
        System.out.println("Inside the client, use 'help' or 'aiuto' to see all commands.");
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
            String raw = promptRequired("Connection type (socket/rmi)").toLowerCase(Locale.ROOT);
            switch (raw) {
                case "socket", "s" -> {
                    return ConnectionType.SOCKET;
                }
                case "rmi", "r" -> {
                    return ConnectionType.RMI;
                }
                default -> printLine("Choose 'socket' or 'rmi'.");
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
            printLine(label + " cannot be empty.");
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
                printLine(label + " must be a valid number.");
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
            int value = promptInt("Number of players");
            if (value >= Globals.MIN_NUM_PLAYERS && value <= Globals.MAX_NUM_PLAYERS) {
                return value;
            }
            printLine("Number of players must be between "
                    + Globals.MIN_NUM_PLAYERS + " and " + Globals.MAX_NUM_PLAYERS + ".");
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
                printLine(label + " cannot be empty.");
                continue;
            }
            if (value.equals("y") || value.equals("yes")) {
                return true;
            }
            if (value.equals("n") || value.equals("no")) {
                return false;
            }
            printLine("Answer with yes/no or y/n.");
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
