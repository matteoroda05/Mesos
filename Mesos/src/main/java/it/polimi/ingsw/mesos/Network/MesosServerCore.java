package it.polimi.ingsw.mesos.Network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameFlowController;
import it.polimi.ingsw.mesos.Controller.GameSetup;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingBonusEvent;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndTurn;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingOnCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.Characters.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.GathererCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ShamanCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.CavePaintingEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.ShamanicRitualEvent;
import it.polimi.ingsw.mesos.Model.Cards.Events.SustenanceEvent;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.MarketRow;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

public class MesosServerCore implements MesosServerService, AutoCloseable {
    private final CardLoader.CardData cardData;
    private final GameSetup gameSetup;
    private final Map<String, GameSession> sessions;
    private final Map<String, ReconnectTarget> lastReconnectTargetByAddress;
    private final long heartbeatTimeoutMillis;
    private final long suspensionTimeoutMillis;
    private final LongSupplier clock;
    private final ScheduledExecutorService heartbeatMonitor;

    public MesosServerCore() throws IOException {
        this(CardLoader.load());
    }

    public MesosServerCore(CardLoader.CardData cardData) {
        this(
                cardData,
                NetworkDefaults.HEARTBEAT_TIMEOUT_MILLIS,
                NetworkDefaults.DISCONNECTION_WIN_TIMEOUT_MILLIS,
                NetworkDefaults.HEARTBEAT_CHECK_INTERVAL_MILLIS,
                System::currentTimeMillis,
                true
        );
    }

    MesosServerCore(
            CardLoader.CardData cardData,
            long heartbeatTimeoutMillis,
            long suspensionTimeoutMillis,
            long heartbeatCheckIntervalMillis,
            LongSupplier clock,
            boolean startHeartbeatMonitor
    ) {
        if (cardData == null) {
            throw new IllegalArgumentException("cardData cannot be null");
        }
        if (heartbeatTimeoutMillis <= 0) {
            throw new IllegalArgumentException("heartbeatTimeoutMillis must be positive");
        }
        if (suspensionTimeoutMillis <= 0) {
            throw new IllegalArgumentException("suspensionTimeoutMillis must be positive");
        }
        if (heartbeatCheckIntervalMillis <= 0) {
            throw new IllegalArgumentException("heartbeatCheckIntervalMillis must be positive");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        this.cardData = cardData;
        this.gameSetup = new GameSetup();
        this.sessions = new LinkedHashMap<>();
        this.lastReconnectTargetByAddress = new LinkedHashMap<>();
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
        this.suspensionTimeoutMillis = suspensionTimeoutMillis;
        this.clock = clock;
        if (startHeartbeatMonitor) {
            this.heartbeatMonitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "mesos-heartbeat-monitor");
                thread.setDaemon(true);
                return thread;
            });
            this.heartbeatMonitor.scheduleAtFixedRate(
                    this::safeCheckConnections,
                    heartbeatCheckIntervalMillis,
                    heartbeatCheckIntervalMillis,
                    TimeUnit.MILLISECONDS
            );
        } else {
            this.heartbeatMonitor = null;
        }
    }

    @Override
    public synchronized GameOperationResult getLobby() {
        return GameOperationResult.successLobby("Lobby aggiornata", toLobbySnapshot());
    }

    @Override
    public synchronized GameOperationResult createGame(CreateGameRequest request) {
        GameSession existing = sessions.get(request.gameName());
        if (existing != null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' esiste gia'",
                    OperationErrorCode.GAME_NAME_TAKEN,
                    null,
                    toLobbySnapshot()
            );
        }

        GameContext context = gameSetup.createGame(request.gameName(), request.playerCount(), cardData);
        GameSession session = new GameSession(context);
        sessions.put(request.gameName(), session);
        return GameOperationResult.success("Partita creata", session.toSnapshot());
    }

    @Override
    public synchronized GameOperationResult joinGame(JoinGameRequest request) {
        return joinGame(request, null);
    }

    @Override
    public synchronized GameOperationResult joinGame(JoinGameRequest request, String clientAddress) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null,
                    toLobbySnapshot()
            );
        }
        if (!session.isJoinable()) {
            return GameOperationResult.failure(
                    "La lobby e' gia' piena",
                    OperationErrorCode.GAME_FULL,
                    null,
                    toLobbySnapshot()
            );
        }
        if (isNicknameTaken(request.nickname())) {
            return GameOperationResult.failure(
                    "Il giocatore '" + request.nickname() + "' esiste gia'",
                    OperationErrorCode.PLAYER_NAME_TAKEN,
                    null,
                    toLobbySnapshot()
            );
        }
        return session.join(request.nickname(), gameSetup, normalizeAddress(clientAddress), clock.getAsLong());
    }

    @Override
    public synchronized GameOperationResult getSnapshot(SnapshotRequest request) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return GameOperationResult.success("Snapshot aggiornato", session.toSnapshot());
    }

    @Override
    public synchronized GameOperationResult chooseOffer(ChooseOfferRequest request) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.chooseOffer(request.nickname(), request.offerTileIndex());
    }

    @Override
    public synchronized GameOperationResult chooseCard(ChooseCardRequest request) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.chooseCard(request.nickname(), request.cardIndex(), request.topRow());
    }

    @Override
    public synchronized GameOperationResult subscribe(SubscribeGameRequest request, GameUpdateSink updateSink) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.subscribe(request.nickname(), updateSink);
    }

    @Override
    public synchronized GameOperationResult unsubscribe(UnsubscribeGameRequest request, GameUpdateSink updateSink) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.unsubscribe(request.nickname(), updateSink);
    }

    @Override
    public synchronized GameOperationResult heartbeat(HeartbeatRequest request, String clientAddress) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.heartbeat(request.nickname(), normalizeAddress(clientAddress), clock.getAsLong());
    }

    @Override
    public synchronized GameOperationResult getReconnectProposal(ReconnectProbeRequest request, String clientAddress) {
        ReconnectTarget target = reconnectTargetFor(clientAddress);
        if (target == null) {
            return GameOperationResult.successReconnect("Nessuna partita da riconnettere", null);
        }
        return GameOperationResult.successReconnect(
                "Riconnessione disponibile",
                new ReconnectProposal(target.gameName(), target.nickname())
        );
    }

    @Override
    public synchronized GameOperationResult reconnectGame(ReconnectGameRequest request, String clientAddress) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.reconnect(request.nickname(), normalizeAddress(clientAddress), clock.getAsLong());
    }

    @Override
    public synchronized GameOperationResult declineReconnect(DeclineReconnectRequest request, String clientAddress) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.declineReconnect(request.nickname(), normalizeAddress(clientAddress), clock.getAsLong());
    }

    @Override
    public void close() {
        if (heartbeatMonitor != null) {
            heartbeatMonitor.shutdownNow();
        }
    }

    synchronized void checkConnectionsForTesting() {
        checkConnections();
    }

    private void safeCheckConnections() {
        try {
            synchronized (this) {
                checkConnections();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void checkConnections() {
        long now = clock.getAsLong();
        for (GameSession session : sessions.values()) {
            session.checkConnections(now);
        }
    }

    private String normalizeAddress(String clientAddress) {
        if (clientAddress == null || clientAddress.isBlank()) {
            return null;
        }
        return clientAddress.strip();
    }

    private void rememberReconnectTarget(String clientAddress, String gameName, String nickname) {
        String normalizedAddress = normalizeAddress(clientAddress);
        if (normalizedAddress == null) {
            return;
        }
        lastReconnectTargetByAddress.put(normalizedAddress, new ReconnectTarget(gameName, nickname));
    }

    private void forgetReconnectTarget(String clientAddress, String gameName, String nickname) {
        String normalizedAddress = normalizeAddress(clientAddress);
        if (normalizedAddress == null) {
            return;
        }
        ReconnectTarget target = lastReconnectTargetByAddress.get(normalizedAddress);
        if (target != null && target.gameName().equals(gameName) && target.nickname().equals(nickname)) {
            lastReconnectTargetByAddress.remove(normalizedAddress);
        }
    }

    private ReconnectTarget reconnectTargetFor(String clientAddress) {
        String normalizedAddress = normalizeAddress(clientAddress);
        if (normalizedAddress == null) {
            return null;
        }
        ReconnectTarget target = lastReconnectTargetByAddress.get(normalizedAddress);
        if (target == null) {
            return null;
        }
        GameSession session = sessions.get(target.gameName());
        if (session == null || !session.canReconnect(target.nickname(), normalizedAddress)) {
            return null;
        }
        return target;
    }

    private boolean isNicknameTaken(String nickname) {
        return sessions.values().stream().anyMatch(session -> session.containsNickname(nickname));
    }

    private LobbySnapshot toLobbySnapshot() {
        return new LobbySnapshot(sessions.values().stream()
                .filter(GameSession::isJoinable)
                .map(GameSession::toLobbySummary)
                .toList());
    }

    private final class GameSession {
        private final GameContext context;
        private final List<String> joinedNicknames;
        private final Map<String, Color> colorsByNickname;
        private final Map<String, PlayerConnectionState> connectionStatesByNickname;
        private final Set<GameUpdateSink> subscribers;
        private GameFlowController controller;
        private boolean suspended;
        private long suspensionDeadlineMillis;
        private String forcedWinnerNickname;

        private GameSession(GameContext context) {
            this.context = context;
            this.joinedNicknames = new ArrayList<>();
            this.colorsByNickname = new LinkedHashMap<>();
            this.connectionStatesByNickname = new LinkedHashMap<>();
            this.subscribers = new LinkedHashSet<>();
        }

        private GameOperationResult join(String nickname, GameSetup gameSetup, String clientAddress, long now) {
            joinedNicknames.add(nickname);
            connectionStatesByNickname.put(nickname, new PlayerConnectionState(clientAddress, now));
            rememberReconnectTarget(clientAddress, context.getGameName(), nickname);

            if (joinedNicknames.size() == context.getGameSize()) {
                gameSetup.createMapOfPlayers(context, joinedNicknames);
                controller = new GameFlowController(context);
                colorsByNickname.clear();
                for (Player player : context.getPlayers()) {
                    colorsByNickname.put(player.getNickname(), player.getColor());
                }
            }

            return GameOperationResult.success(
                    started() ? "Giocatore entrato: la partita inizia" : "Giocatore entrato nella lobby",
                    publishAndSnapshot(started() ? "Giocatore entrato: la partita inizia" : "Giocatore entrato nella lobby")
            );
        }

        private boolean containsNickname(String nickname) {
            return joinedNicknames.contains(nickname);
        }

        private boolean isJoinable() {
            return !started() && !isFull();
        }

        private boolean isFull() {
            return joinedNicknames.size() >= context.getGameSize();
        }

        private LobbyGameSummary toLobbySummary() {
            return new LobbyGameSummary(
                    context.getGameName(),
                    context.getGameSize(),
                    joinedNicknames.size(),
                    context.getGameSize() - joinedNicknames.size()
            );
        }

        private GameOperationResult chooseOffer(String nickname, int offerTileIndex) {
            if (!started()) {
                return GameOperationResult.failure("La partita non e' ancora iniziata", toSnapshot());
            }
            GameOperationResult availabilityFailure = availabilityFailure(nickname);
            if (availabilityFailure != null) {
                return availabilityFailure;
            }

            Color playerColor = colorsByNickname.get(nickname);
            if (playerColor == null) {
                return GameOperationResult.failure(
                        "Giocatore sconosciuto '" + nickname + "'",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        toSnapshot()
                );
            }

            try {
                controller.chooseOffer(playerColor, offerTileIndex);
                rebalanceForActivePlayers(clock.getAsLong(), "Partita ripresa: almeno due giocatori sono di nuovo online");
                return GameOperationResult.success(
                        "Tessera offerta scelta",
                        publishAndSnapshot("Tessera offerta scelta")
                );
            } catch (RuntimeException exception) {
                return GameOperationResult.failure(exception.getMessage(), toSnapshot());
            }
        }

        private GameOperationResult chooseCard(String nickname, int cardIndex, boolean topRow) {
            if (!started()) {
                return GameOperationResult.failure("La partita non e' ancora iniziata", toSnapshot());
            }
            GameOperationResult availabilityFailure = availabilityFailure(nickname);
            if (availabilityFailure != null) {
                return availabilityFailure;
            }

            Color playerColor = colorsByNickname.get(nickname);
            if (playerColor == null) {
                return GameOperationResult.failure(
                        "Giocatore sconosciuto '" + nickname + "'",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        toSnapshot()
                );
            }

            try {
                controller.chooseCard(playerColor, cardIndex, topRow);
                rebalanceForActivePlayers(clock.getAsLong(), "Partita ripresa: almeno due giocatori sono di nuovo online");
                return GameOperationResult.success(
                        "Carta scelta",
                        publishAndSnapshot("Carta scelta")
                );
            } catch (RuntimeException exception) {
                return GameOperationResult.failure(exception.getMessage(), toSnapshot());
            }
        }

        private GameOperationResult subscribe(String nickname, GameUpdateSink updateSink) {
            if (updateSink == null) {
                return GameOperationResult.failure(
                        "Listener di aggiornamento non valido",
                        OperationErrorCode.INVALID_LISTENER,
                        toSnapshot()
                );
            }
            if (!joinedNicknames.contains(nickname)) {
                return GameOperationResult.failure(
                        "Il giocatore '" + nickname + "' non e' presente nella partita",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        toSnapshot()
                );
            }

            subscribers.add(updateSink);
            return GameOperationResult.success(
                    "Iscrizione agli aggiornamenti attiva",
                    toSnapshot()
            );
        }

        private GameOperationResult unsubscribe(String nickname, GameUpdateSink updateSink) {
            if (updateSink != null) {
                subscribers.remove(updateSink);
            }
            return GameOperationResult.success(
                    "Iscrizione agli aggiornamenti disattivata",
                    toSnapshot()
            );
        }

        private GameOperationResult heartbeat(String nickname, String clientAddress, long now) {
            PlayerConnectionState state = connectionStatesByNickname.get(nickname);
            if (state == null) {
                return GameOperationResult.failure(
                        "Il giocatore '" + nickname + "' non e' presente nella partita",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        toSnapshot()
                );
            }
            if (!state.online()) {
                return GameOperationResult.failure(
                        "Il giocatore '" + nickname + "' deve riconnettersi prima di inviare heartbeat",
                        OperationErrorCode.RECONNECT_NOT_AVAILABLE,
                        toSnapshot()
                );
            }
            state.recordHeartbeat(clientAddress, now);
            rememberReconnectTarget(clientAddress, context.getGameName(), nickname);
            return GameOperationResult.success("Heartbeat ricevuto", toSnapshot());
        }

        private boolean canReconnect(String nickname, String clientAddress) {
            PlayerConnectionState state = connectionStatesByNickname.get(nickname);
            return state != null
                    && started()
                    && !finished()
                    && !state.online()
                    && !state.reconnectDeclined()
                    && sameKnownAddress(state.clientAddress(), clientAddress);
        }

        private GameOperationResult reconnect(String nickname, String clientAddress, long now) {
            if (!canReconnect(nickname, clientAddress)) {
                return GameOperationResult.failure(
                        "Nessuna riconnessione disponibile per '" + nickname + "'",
                        OperationErrorCode.RECONNECT_NOT_AVAILABLE,
                        toSnapshot()
                );
            }

            PlayerConnectionState state = connectionStatesByNickname.get(nickname);
            state.reconnect(clientAddress, now);
            rememberReconnectTarget(clientAddress, context.getGameName(), nickname);
            rebalanceForActivePlayers(now, "Partita ripresa: almeno due giocatori sono di nuovo online");
            return GameOperationResult.success(
                    nickname + " si e' riconnesso alla partita",
                    publishAndSnapshot(nickname + " si e' riconnesso alla partita")
            );
        }

        private GameOperationResult declineReconnect(String nickname, String clientAddress, long now) {
            PlayerConnectionState state = connectionStatesByNickname.get(nickname);
            if (state == null || !sameKnownAddress(state.clientAddress(), clientAddress)) {
                return GameOperationResult.failure(
                        "Nessuna riconnessione disponibile per '" + nickname + "'",
                        OperationErrorCode.RECONNECT_NOT_AVAILABLE,
                        toSnapshot()
                );
            }

            state.declineReconnect();
            forgetReconnectTarget(clientAddress, context.getGameName(), nickname);
            rebalanceForActivePlayers(now, "Partita ripresa: almeno due giocatori sono di nuovo online");
            return GameOperationResult.success(
                    nickname + " non rientrera' nella partita",
                    publishAndSnapshot(nickname + " non rientrera' nella partita")
            );
        }

        private void checkConnections(long now) {
            if (!started() || finished()) {
                return;
            }

            boolean disconnected = false;
            for (Map.Entry<String, PlayerConnectionState> entry : connectionStatesByNickname.entrySet()) {
                PlayerConnectionState state = entry.getValue();
                if (state.shouldTimeout(now, heartbeatTimeoutMillis)) {
                    state.disconnect(now);
                    rememberReconnectTarget(state.clientAddress(), context.getGameName(), entry.getKey());
                    publish(new GameViewUpdate(
                            context.getGameName(),
                            entry.getKey() + " si e' disconnesso dalla partita",
                            toSnapshot()
                    ));
                    disconnected = true;
                }
            }

            if (disconnected) {
                rebalanceForActivePlayers(now, "Partita ripresa: almeno due giocatori sono di nuovo online");
            }
            checkSuspensionDeadline(now);
        }

        private void rebalanceForActivePlayers(long now, String resumeMessage) {
            if (!started() || forcedWinnerNickname != null || controller.isGameFinished()) {
                return;
            }

            int onlinePlayers = onlinePlayerCount();
            if (onlinePlayers == 0) {
                suspended = true;
                suspensionDeadlineMillis = 0;
                return;
            }
            if (onlinePlayers == 1) {
                boolean newlySuspended = !suspended || suspensionDeadlineMillis == 0;
                suspended = true;
                suspensionDeadlineMillis = now + suspensionTimeoutMillis;
                if (newlySuspended) {
                    publishAndSnapshot("Partita sospesa: rimane online solo " + onlyOnlineNickname()
                            + ". Timeout vittoria tra 60 secondi");
                }
                return;
            }

            boolean wasSuspended = suspended;
            suspended = false;
            suspensionDeadlineMillis = 0;
            skipOfflineActivePlayers();
            if (wasSuspended) {
                publishAndSnapshot(resumeMessage);
            }
        }

        private void skipOfflineActivePlayers() {
            while (started() && !controller.isGameFinished() && !suspended) {
                String activeNickname = nicknameForColor(currentState().getActivePlayer());
                if (activeNickname == null || isOnline(activeNickname)) {
                    return;
                }
                controller.skipTurn();
                publishAndSnapshot("Turno saltato: " + activeNickname + " e' offline");
            }
        }

        private void checkSuspensionDeadline(long now) {
            if (!suspended || suspensionDeadlineMillis == 0 || onlinePlayerCount() != 1 || now < suspensionDeadlineMillis) {
                return;
            }
            forcedWinnerNickname = onlyOnlineNickname();
            suspended = false;
            suspensionDeadlineMillis = 0;
            publishAndSnapshot(forcedWinnerNickname + " vince: timeout di riconnessione scaduto");
        }

        private GameOperationResult availabilityFailure(String nickname) {
            if (forcedWinnerNickname != null) {
                return GameOperationResult.failure(
                        "La partita e' gia' terminata",
                        OperationErrorCode.GAME_SUSPENDED,
                        toSnapshot()
                );
            }
            if (suspended) {
                return GameOperationResult.failure(
                        "La partita e' sospesa in attesa di una riconnessione",
                        OperationErrorCode.GAME_SUSPENDED,
                        toSnapshot()
                );
            }
            if (!isOnline(nickname)) {
                return GameOperationResult.failure(
                        "Il giocatore '" + nickname + "' risulta offline",
                        OperationErrorCode.RECONNECT_NOT_AVAILABLE,
                        toSnapshot()
                );
            }
            return null;
        }

        private boolean sameKnownAddress(String expected, String actual) {
            return expected != null && actual != null && expected.equals(actual);
        }

        private boolean isOnline(String nickname) {
            PlayerConnectionState state = connectionStatesByNickname.get(nickname);
            return state != null && state.online();
        }

        private int onlinePlayerCount() {
            int count = 0;
            for (PlayerConnectionState state : connectionStatesByNickname.values()) {
                if (state.online()) {
                    count++;
                }
            }
            return count;
        }

        private String onlyOnlineNickname() {
            String onlyOnline = null;
            for (Map.Entry<String, PlayerConnectionState> entry : connectionStatesByNickname.entrySet()) {
                if (!entry.getValue().online()) {
                    continue;
                }
                if (onlyOnline != null) {
                    return null;
                }
                onlyOnline = entry.getKey();
            }
            return onlyOnline;
        }

        private GameSnapshot publishAndSnapshot(String message) {
            GameSnapshot snapshot = toSnapshot();
            publish(new GameViewUpdate(context.getGameName(), message, snapshot));
            return snapshot;
        }

        private void publish(GameViewUpdate update) {
            List<GameUpdateSink> failedSubscribers = new ArrayList<>();
            for (GameUpdateSink subscriber : subscribers) {
                try {
                    subscriber.accept(update);
                } catch (IOException exception) {
                    failedSubscribers.add(subscriber);
                }
            }
            subscribers.removeAll(failedSubscribers);
        }

        private GameSnapshot toSnapshot() {
            GameState currentState = currentState();
            Color activeColor = currentState == null ? null : currentState.getActivePlayer();
            String activeNickname = nicknameForColor(activeColor);

            return new GameSnapshot(
                    context.getGameName(),
                    context.getGameSize(),
                    joinedNicknames.size(),
                    started(),
                    finished(),
                    winnerNickname(),
                    currentStatus(),
                    context.getRoundNumber(),
                    context.getCurrentEra(),
                    activeNickname,
                    activeColor,
                    currentState == null ? 0 : currentState.getRemainingTopChoices(),
                    currentState == null ? 0 : currentState.getRemainingBottomChoices(),
                    summarizePlayers(),
                    summarizeOfferTiles(),
                    summarizeTurnOrder(),
                    summarizeRow(context.getGameTable().getTopRow()),
                    summarizeRow(context.getGameTable().getBottomRow())
            );
        }

        private boolean started() {
            return controller != null;
        }

        private boolean finished() {
            return forcedWinnerNickname != null || (started() && controller.isGameFinished());
        }

        private GameState currentState() {
            return !started() ? null : controller.getCurrentState();
        }

        private GameStatus currentStatus() {
            if (!started()) {
                return GameStatus.LOGIN;
            }
            if (forcedWinnerNickname != null || controller.isGameFinished()) {
                return GameStatus.END_GAME;
            }
            if (suspended) {
                return GameStatus.SUSPENDED;
            }
            return controller.getCurrentState().getStatus();
        }

        private String winnerNickname() {
            if (forcedWinnerNickname != null) {
                return forcedWinnerNickname;
            }
            if (!started() || !controller.isGameFinished()) {
                return null;
            }
            return context.getPlayers().stream()
                    .max((first, second) -> Integer.compare(first.getPrestigePoints(), second.getPrestigePoints()))
                    .map(Player::getNickname)
                    .orElse(null);
        }

        private List<PlayerSummary> summarizePlayers() {
            if (!started()) {
                return joinedNicknames.stream()
                        .map(nickname -> new PlayerSummary(nickname, null, 0, 0, 0, 0, isOnline(nickname)))
                        .toList();
            }

            return context.getPlayers().stream()
                    .map(player -> new PlayerSummary(
                            player.getNickname(),
                            player.getColor(),
                            player.getFood(),
                            player.getPrestigePoints(),
                            player.getTribe().getNumCharacters(),
                            player.getTribe().getNumBuildings(),
                            isOnline(player.getNickname())
                    ))
                    .toList();
        }

        private List<OfferTileSummary> summarizeOfferTiles() {
            List<OfferTileSummary> summaries = new ArrayList<>();
            for (int index = 0; index < context.getGameSize() + 2; index++) {
                OfferTile tile = context.getGameTable().getOfferTrack().getTile(index);
                summaries.add(new OfferTileSummary(
                        index,
                        tile.getOfferTileId(),
                        tile.getTopPicks(),
                        tile.getBottomPicks(),
                        tile.getFoodGain(),
                        tile.isAvailable(),
                        tile.getOccupant(),
                        nicknameForColor(tile.getOccupant())
                ));
            }
            return summaries;
        }

        private List<TurnOrderSpaceSummary> summarizeTurnOrder() {
            List<TurnOrderSpaceSummary> summaries = new ArrayList<>();
            List<TurnOrderSpace> spaces = context.getGameTable().getTurnOrderTile().spaces();
            for (int index = 0; index < spaces.size(); index++) {
                TurnOrderSpace space = spaces.get(index);
                summaries.add(new TurnOrderSpaceSummary(
                        index,
                        space.getBonus(),
                        space.getOccupant(),
                        nicknameForColor(space.getOccupant())
                ));
            }
            return summaries;
        }

        private List<CardSummary> summarizeRow(MarketRow row) {
            List<CardSummary> summaries = new ArrayList<>();
            int selectionIndex = 0;

            for (TribeCard card : row.getTribeCards()) {
                summaries.add(summarizeCard(card, selectionIndex));
                selectionIndex++;
            }
            for (BuildingCard card : row.getBuildingCards()) {
                summaries.add(summarizeCard(card, selectionIndex));
                selectionIndex++;
            }

            return summaries;
        }

        private CardSummary summarizeCard(Card card, int selectionIndex) {
            return switch (card) {
                case BuildingCard buildingCard -> new CardSummary(
                        selectionIndex,
                        formatName(buildingCard.getClass().getSimpleName()),
                        buildingCard.getEra(),
                        false,
                        describeCard(buildingCard),
                        buildingCard.getFoodCost(),
                        buildingCard.getPrestigePoints()
                );
                case EventCard eventCard -> new CardSummary(
                        selectionIndex,
                        formatName(eventCard.getClass().getSimpleName()),
                        eventCard.getEra(),
                        true,
                        describeCard(eventCard),
                        null,
                        null
                );
                case CharacterCard characterCard -> new CardSummary(
                        selectionIndex,
                        formatName(characterCard.getClass().getSimpleName()),
                        characterCard.getEra(),
                        false,
                        describeCard(characterCard),
                        null,
                        null
                );
                default -> throw new IllegalStateException("Unsupported card type: " + card.getClass().getSimpleName());
            };
        }

        private String describeCard(Card card) {
            return switch (card) {
                case BuilderCard builderCard ->
                        "sconto edifici " + builderCard.getBuildingDiscount()
                                + ", fine partita +" + builderCard.getPrestigePoints() + " pp";
                case GathererCard gathererCard ->
                        "sconto sostentamento " + gathererCard.getDiscount() + " cibo";
                case HunterCard hunterCard ->
                        hunterCard.isActiveHunter() ? "cacciatore attivo" : "cacciatore";
                case InventorCard inventorCard ->
                        "icona " + inventorCard.getInventionIcon();
                case ShamanCard shamanCard ->
                        shamanCard.getStars() + " stelle sciamaniche";
                case HuntEvent huntEvent ->
                        "+" + huntEvent.getPrestigePerHunter() + " pp e +" + huntEvent.getFoodPerHunter()
                                + " cibo per cacciatore";
                case CavePaintingEvent cavePaintingEvent ->
                        "con almeno " + cavePaintingEvent.getThreshold() + " artisti: +"
                                + cavePaintingEvent.getPointsPerArtist()
                                + " pp ciascuno, altrimenti " + cavePaintingEvent.getPenalty() + " pp";
                case ShamanicRitualEvent shamanicRitualEvent ->
                        "vincitori +" + shamanicRitualEvent.getWinnerBonus()
                                + " pp, sconfitti " + shamanicRitualEvent.getLoserPenalty() + " pp";
                case SustenanceEvent sustenanceEvent ->
                        sustenanceEvent.getFoodPerCharacter() + " cibo per personaggio, ogni deficit costa "
                                + sustenanceEvent.getPointPenalty() + " pp";
                case BuildingBonusEvent buildingBonusEvent ->
                        "su " + formatTrigger(buildingBonusEvent.getTrigger())
                                + ": " + formatName(buildingBonusEvent.getCharacterType().getSimpleName())
                                + " da' " + signed(buildingBonusEvent.getFoodBonus(), " cibo")
                                + ", +" + buildingBonusEvent.getPrestigeBonus() + " pp";
                case BuildingEndTurn buildingEndTurn ->
                        "fine turno: +" + buildingEndTurn.getFoodBonus() + " cibo con bonus turno positivo";
                case BuildingOnCard buildingOnCard ->
                        buildingOnCard.isSetCompletion()
                                ? "quando prendi carte: +" + buildingOnCard.getFoodBonus() + " cibo se completi un set"
                                : "coppia inventori: +" + buildingOnCard.getFoodBonus() + " cibo con icona duplicata";
                case BuildingShamanicRitual buildingShamanicRitual ->
                        buildingShamanicRitual.getStarBonus() != 0
                                ? "rituale: +" + buildingShamanicRitual.getStarBonus() + " stelle prima del punteggio"
                                : buildingShamanicRitual.getWinMultiplier() != 0
                                        ? "rituale: il vincitore moltiplica x" + buildingShamanicRitual.getWinMultiplier()
                                        : "rituale: annulla la tua penalita' se perdi";
                case BuildingEndGame buildingEndGame ->
                        describeEndGameBuilding(buildingEndGame);
                default -> "";
            };
        }

        private String describeEndGameBuilding(BuildingEndGame buildingEndGame) {
            if (buildingEndGame.isDoubleBuildersPrestige()) {
                return "fine partita: raddoppia il prestigio dei builder";
            }
            if (buildingEndGame.getCharacterSetBonusPrestige() != 0) {
                return "fine partita: +" + buildingEndGame.getCharacterSetBonusPrestige() + " pp per set completo di personaggi";
            }
            if (buildingEndGame.getCharacterMultiplier() != 0 && buildingEndGame.getCharacterType() != null) {
                return "fine partita: +" + buildingEndGame.getCharacterMultiplier() + " pp per "
                        + formatName(buildingEndGame.getCharacterType().getSimpleName());
            }
            if (buildingEndGame.getPrestigeBonus() != 0) {
                return "fine partita: +" + buildingEndGame.getPrestigeBonus() + " pp";
            }
            return "";
        }

        private String formatName(String rawName) {
            String baseName = rawName
                    .replace("Card", "")
                    .replace("Event", " Event");

            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < baseName.length(); index++) {
                char character = baseName.charAt(index);
                if (index > 0 && Character.isUpperCase(character) && baseName.charAt(index - 1) != ' ') {
                    builder.append(' ');
                }
                builder.append(character);
            }
            return builder.toString().trim();
        }

        private String formatTrigger(it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger trigger) {
            return trigger.name()
                    .replace("ON_EVENT_", "")
                    .replace('_', ' ')
                    .toLowerCase();
        }

        private String signed(int value, String suffix) {
            if (value >= 0) {
                return "+" + value + suffix;
            }
            return value + suffix;
        }

        private String nicknameForColor(Color color) {
            if (color == null) {
                return null;
            }
            return colorsByNickname.entrySet().stream()
                    .filter(entry -> entry.getValue() == color)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }
    }

    private record ReconnectTarget(String gameName, String nickname) {
    }

    private static final class PlayerConnectionState {
        private String clientAddress;
        private long lastMessageAtMillis;
        private boolean online;
        private boolean reconnectDeclined;

        private PlayerConnectionState(String clientAddress, long now) {
            this.clientAddress = clientAddress;
            this.lastMessageAtMillis = now;
            this.online = true;
            this.reconnectDeclined = false;
        }

        private String clientAddress() {
            return clientAddress;
        }

        private boolean online() {
            return online;
        }

        private boolean reconnectDeclined() {
            return reconnectDeclined;
        }

        private void recordHeartbeat(String clientAddress, long now) {
            if (clientAddress != null) {
                this.clientAddress = clientAddress;
            }
            this.lastMessageAtMillis = now;
        }

        private void reconnect(String clientAddress, long now) {
            if (clientAddress != null) {
                this.clientAddress = clientAddress;
            }
            this.lastMessageAtMillis = now;
            this.online = true;
            this.reconnectDeclined = false;
        }

        private void disconnect(long now) {
            this.lastMessageAtMillis = now;
            this.online = false;
        }

        private void declineReconnect() {
            this.reconnectDeclined = true;
        }

        private boolean shouldTimeout(long now, long timeoutMillis) {
            return online && clientAddress != null && now - lastMessageAtMillis > timeoutMillis;
        }
    }
}
