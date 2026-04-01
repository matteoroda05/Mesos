package it.polimi.ingsw.mesos.CLI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import it.polimi.ingsw.mesos.Network.ChooseCardRequest;
import it.polimi.ingsw.mesos.Network.ChooseOfferRequest;
import it.polimi.ingsw.mesos.Network.ConnectionType;
import it.polimi.ingsw.mesos.Network.CreateGameRequest;
import it.polimi.ingsw.mesos.Network.GameClient;
import it.polimi.ingsw.mesos.Network.GameClientFactory;
import it.polimi.ingsw.mesos.Network.GameOperationResult;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.GameViewUpdate;
import it.polimi.ingsw.mesos.Network.JoinGameRequest;
import it.polimi.ingsw.mesos.Network.NetworkDefaults;
import it.polimi.ingsw.mesos.Network.ServerEndpoint;
import it.polimi.ingsw.mesos.Network.SnapshotRequest;
import it.polimi.ingsw.mesos.Network.SubscribeGameRequest;
import it.polimi.ingsw.mesos.Network.UnsubscribeGameRequest;

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

        ConnectionType connectionType = promptConnectionType();
        String host = promptWithDefault("Host del server", NetworkDefaults.DEFAULT_RMI_HOST);
        int port = promptInt(
                "Porta",
                connectionType == ConnectionType.SOCKET
                        ? NetworkDefaults.DEFAULT_SOCKET_PORT
                        : NetworkDefaults.DEFAULT_RMI_REGISTRY_PORT
        );
        String serviceName = connectionType == ConnectionType.RMI
                ? promptWithDefault("Servizio RMI", NetworkDefaults.DEFAULT_RMI_SERVICE_NAME)
                : NetworkDefaults.DEFAULT_RMI_SERVICE_NAME;

        nickname = promptRequired("Nickname");
        gameName = promptRequired("Nome partita");
        boolean createGame = promptYesNo("Vuoi creare una nuova partita", false);

        try (GameClient client = GameClientFactory.connect(new ServerEndpoint(connectionType, host, port, serviceName))) {
            if (createGame) {
                int playerCount = promptIntInRange("Numero giocatori", 2, 5, 2);
                handleResult(client.createGame(new CreateGameRequest(gameName, playerCount)));
            }

            handleResult(client.joinGame(new JoinGameRequest(gameName, nickname)));
            handleResult(client.subscribe(new SubscribeGameRequest(gameName, nickname), this::handleRemoteUpdate), false);
            mainLoop(client);
            client.unsubscribe(new UnsubscribeGameRequest(gameName, nickname));
        }
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
        System.out.println("Per giocare da un altro computer usa l'IP del server al posto di 127.0.0.1.");
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

    private int promptIntInRange(String label, int min, int max, int defaultValue) throws IOException {
        while (true) {
            int value = promptInt(label, defaultValue);
            if (value >= min && value <= max) {
                return value;
            }
            printLine(label + " deve essere compreso tra " + min + " e " + max + ".");
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
        return reader.readLine();
    }
}
