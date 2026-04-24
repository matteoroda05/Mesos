package it.polimi.ingsw.mesos.CLI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import it.polimi.ingsw.mesos.Globals;
import it.polimi.ingsw.mesos.Network.Requests.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.Requests.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.Requests.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameClientFactory;
import it.polimi.ingsw.mesos.Network.LobbySnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.OperationErrorCode;
import it.polimi.ingsw.mesos.Network.Requests.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;
import it.polimi.ingsw.mesos.Network.Requests.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.Requests.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.Requests.UnsubscribeGameRequest;

public final class MesosCliClient {
    private final BufferedReader reader;
    private final CliPrinter printer;
    private final Object consoleLock;

    private String nickname;
    private String gameName;
    private GameSnapshot snapshot;

    public MesosCliClient() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.printer = new CliPrinter();
        this.consoleLock = new Object();
    }

    public static void main(String[] args) throws Exception {
        new MesosCliClient().run();
    }

    private void run() throws Exception {
        printBanner();

        try (GameClient client = connectWithRetry()) {
            lobbyLoop(client);
        }
    }

    private GameClient connectWithRetry() throws IOException {
        while (true) {
            ConnectionType connectionType = promptConnectionType();
            String host = promptWithDefault("Host del server", NetworkDefaults.DEFAULT_RMI_HOST);

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

    private boolean handleCreateFlow(GameClient client, String requestedGameName) throws IOException {
        int playerCount = promptPlayerCount();
        String createdGameName = createGameWithUniqueName(client, requestedGameName, playerCount);
        if (createdGameName == null) {
            return false;
        }

        String requestedNickname = promptRequired("Nickname");
        return joinSelectedGame(client, createdGameName, requestedNickname, false);
    }

    private boolean handleJoinFlow(GameClient client, LobbySnapshot lobby, String requestedGameName) throws IOException {
        String requestedNickname = promptRequired("Nickname");

        if (lobby.openGames().stream().noneMatch(game -> game.gameName().equals(requestedGameName))) {
            printLine("La partita '" + requestedGameName + "' non compare tra quelle disponibili.");
        }

        return joinSelectedGame(client, requestedGameName, requestedNickname, true);
    }

    private boolean joinSelectedGame(
            GameClient client,
            String requestedGameName,
            String requestedNickname,
            boolean offerCreateFallback
    ) throws IOException {
        String selectedGameName = requestedGameName;
        String nicknameBase = requestedNickname;
        String candidateNickname = requestedNickname;
        boolean autoAppendNickname = false;
        int nicknameSuffix = 1;

        while (true) {
            GameOperationResult joinResult = client.joinGame(new JoinGameRequest(selectedGameName, candidateNickname));
            if (joinResult.success()) {
                gameName = selectedGameName;
                nickname = candidateNickname;
                handleResult(joinResult);
                enterGame(client);
                return true;
            }

            printLine("[error] " + joinResult.message());
            switch (joinResult.errorCode()) {
                case PLAYER_NAME_TAKEN -> {
                    if (autoAppendNickname) {
                        candidateNickname = appendNumericSuffix(nicknameBase, nicknameSuffix++);
                        printLine("Provo automaticamente con il nickname '" + candidateNickname + "'.");
                        continue;
                    }
                    if (promptYesNo("Il nickname e' gia' in uso. Vuoi cambiarlo", true)) {
                        nicknameBase = promptRequired("Nickname");
                        candidateNickname = nicknameBase;
                    } else {
                        autoAppendNickname = true;
                        candidateNickname = appendNumericSuffix(nicknameBase, nicknameSuffix++);
                        printLine("Provo automaticamente con il nickname '" + candidateNickname + "'.");
                    }
                }
                case GAME_NOT_FOUND -> {
                    if (offerCreateFallback
                            && promptYesNo("La partita non esiste. Vuoi crearla con questo nome", false)) {
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

    private String createGameWithUniqueName(GameClient client, String requestedGameName, int playerCount) throws IOException {
        String gameNameBase = requestedGameName;
        String candidateGameName = requestedGameName;
        boolean autoAppendGameName = false;
        int gameNameSuffix = 1;

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

            if (autoAppendGameName) {
                candidateGameName = appendNumericSuffix(gameNameBase, gameNameSuffix++);
                printLine("Provo automaticamente con il nome partita '" + candidateGameName + "'.");
                continue;
            }

            if (promptYesNo("Il nome partita e' gia' in uso. Vuoi cambiarlo", true)) {
                gameNameBase = promptRequired("Nome partita");
                candidateGameName = gameNameBase;
            } else {
                autoAppendGameName = true;
                candidateGameName = appendNumericSuffix(gameNameBase, gameNameSuffix++);
                printLine("Provo automaticamente con il nome partita '" + candidateGameName + "'.");
            }
        }
    }

    private void enterGame(GameClient client) throws IOException {
        handleResult(client.subscribe(new SubscribeGameRequest(gameName, nickname), this::handleRemoteUpdate), false);
        mainLoop(client);
        client.unsubscribe(new UnsubscribeGameRequest(gameName, nickname));
    }

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

    private boolean promptLobbyAction() throws IOException {
        while (true) {
            String raw = promptWithDefault("Vuoi join o create", "join").toLowerCase(Locale.ROOT);
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

    private String appendNumericSuffix(String baseName, int suffix) {
        return baseName + "-" + suffix;
    }

    private String describeConnectionError(IOException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    private void mainLoop(GameClient client) throws IOException {
        printSnapshotIfAvailable();

        while (true) {
            String command = promptRaw("mesos> ").trim();
            if (command.isEmpty() || command.equalsIgnoreCase("refresh") || command.equalsIgnoreCase("poll")) {
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
            default -> throw new IllegalArgumentException("comando sconosciuto '" + tokens[0] + "'");
        }
    }

    private boolean parseRow(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "top", "alto" -> true;
            case "bottom", "basso" -> false;
            default -> throw new IllegalArgumentException("la riga deve essere 'top' oppure 'bottom'");
        };
    }

    private int parseInt(String token, String label) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " deve essere un numero");
        }
    }

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

    private void refreshSnapshot(GameClient client) throws IOException {
        handleResult(client.getSnapshot(new SnapshotRequest(gameName)), false);
    }

    private void handleResult(GameOperationResult result) {
        handleResult(result, true);
    }

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

    private void printSnapshotIfAvailable() {
        if (snapshot == null) {
            return;
        }
        printLine(printer.renderSnapshot(snapshot, nickname));
    }

    private void printLine(String text) {
        synchronized (consoleLock) {
            System.out.println(text);
        }
    }

    private void printBanner() {
        System.out.println("MESOS CLI");
        System.out.println("Avvia il server in un terminale separato con MesosServerLauncher.");
        System.out.println("Scegli socket o rmi, poi inserisci solo l'indirizzo del server.");
        System.out.println("Il client usa automaticamente le porte di default e, per RMI, il servizio MesosGameService.");
        System.out.println("Dentro il client usa 'help' oppure 'aiuto' per vedere tutti i comandi.");
        System.out.println();
    }

    private ConnectionType promptConnectionType() throws IOException {
        while (true) {
            String raw = promptWithDefault("Tipo connessione (socket/rmi)", "socket").toLowerCase(Locale.ROOT);
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

    private String promptRequired(String label) throws IOException {
        while (true) {
            String value = promptRaw(label + ": ").trim();
            if (!value.isBlank()) {
                return value;
            }
            printLine(label + " non puo' essere vuoto.");
        }
    }

    private String promptWithDefault(String label, String defaultValue) throws IOException {
        String value = promptRaw(label + " [" + defaultValue + "]: ").trim();
        return value.isBlank() ? defaultValue : value;
    }

    private int promptInt(String label, int defaultValue) throws IOException {
        while (true) {
            String value = promptWithDefault(label, String.valueOf(defaultValue));
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                printLine(label + " deve essere un numero valido.");
            }
        }
    }

    private int promptPlayerCount() throws IOException {
        while (true) {
            int value = promptInt("Numero giocatori", 2);
            if (value >= Globals.MIN_NUM_PLAYERS && value <= Globals.MAX_NUM_PLAYERS) {
                return value;
            }
            printLine("Numero giocatori deve essere compreso tra "
                    + Globals.MIN_NUM_PLAYERS + " e " + Globals.MAX_NUM_PLAYERS + ".");
        }
    }

    private boolean promptYesNo(String label, boolean defaultValue) throws IOException {
        String defaultToken = defaultValue ? "Y/n" : "y/N";
        while (true) {
            String value = promptRaw(label + " [" + defaultToken + "]: ").trim().toLowerCase(Locale.ROOT);
            if (value.isBlank()) {
                return defaultValue;
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
