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

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameFlowController;
import it.polimi.ingsw.mesos.Controller.GameSetup;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
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
import it.polimi.ingsw.mesos.Model.Enums.ConnectionStatus;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.MarketRow;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Network.Requests.*;
import it.polimi.ingsw.mesos.Network.Messages.GameOperationResult;
import it.polimi.ingsw.mesos.Network.Messages.GameViewUpdate;

/**
 * Thread-safe in-memory implementation of the network server service.
 * It owns active sessions, nickname reservations and heartbeat-based
 * reconnection state.
 */
public class MesosServerCore implements MesosServerService {
    private final CardLoader.CardData cardData;
    private final GameSetup gameSetup;
    private final Map<String, GameSession> sessions;
    private final Set<String> takenNicknames;
    private final Map<String, String> disconnectedGameByNickname;
    private final Map<String, Long> lastHeartbeatByNickname;
    private final ScheduledExecutorService resilienceTimer;

    /**
     * Creates a server core using card data loaded from default resources.
     *
     * @throws IOException if card data cannot be loaded
     */
    public MesosServerCore() throws IOException {
        this(CardLoader.load());
    }

    /**
     * Creates a server core with explicit card data and starts the resilience
     * timer.
     *
     * @param cardData card definitions used for newly created games
     * @throws IllegalArgumentException if cardData is null
     */
    public MesosServerCore(CardLoader.CardData cardData) {
        if (cardData == null) {
            throw new IllegalArgumentException("cardData cannot be null");
        }
        this.cardData = cardData;
        this.gameSetup = new GameSetup();
        this.sessions = new LinkedHashMap<>();
        this.disconnectedGameByNickname = new LinkedHashMap<>();
        this.takenNicknames = new LinkedHashSet<>();
        this.lastHeartbeatByNickname = new LinkedHashMap<>();

        this.resilienceTimer = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "mesos-server-resilience");
            thread.setDaemon(true);
            return thread;
        });
        this.resilienceTimer.scheduleAtFixedRate(
                this::checkDisconnectedClients,
                1_000,
                1_000,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Reserves or validates a nickname before lobby/game operations.
     * Existing nicknames are rejected unless they belong to a disconnected
     * session, in which case the result advertises rejoin availability.
     *
     * @param request nickname request
     * @return operation result with rejoin availability when applicable
     */
    @Override
    public synchronized GameOperationResult nickname(NicknameRequest request) {
        if (takenNicknames.contains(request.nickname())
                && !disconnectedGameByNickname.containsKey(request.nickname())) {
            return GameOperationResult.failure(
                    "Il giocatore '" + request.nickname() + "' esiste gia'",
                    OperationErrorCode.PLAYER_NAME_TAKEN,
                    null,
                    null
            );
        }

        boolean rejoinAvailable = disconnectedGameByNickname.containsKey(request.nickname());

        if (!rejoinAvailable) {
            takenNicknames.add(request.nickname());
        }

        return GameOperationResult.successNickname(
                "Nickname accettato " + request.nickname(),
                rejoinAvailable
        );
    }

    /**
     * Returns the current lobby snapshot.
     *
     * @return successful lobby update containing joinable games only
     */
    @Override
    public synchronized GameOperationResult getLobby() {
        return GameOperationResult.successLobby("Lobby aggiornata", toLobbySnapshot());
    }

    /**
     * Creates a new game session.
     * Duplicate game names are rejected and return the current lobby snapshot.
     *
     * @param request create-game request
     * @return operation result containing the new game snapshot on success
     */
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

    /**
     * Joins a player to a game or restores a disconnected session.
     * Rejoin requests resolve the game from the disconnected nickname map, so
     * the request game name may be null in that path.
     *
     * @param request join or rejoin request
     * @return operation result describing the join outcome
     */
    @Override
    public synchronized GameOperationResult joinGame(JoinGameRequest request) {
        if (request.rejoin()) {
            if (!disconnectedGameByNickname.containsKey(request.nickname())){
                return GameOperationResult.failure(
                        "Il nickname '" + request.nickname() + "' non e' riconnettibile",
                        OperationErrorCode.NICKNAME_DISCONNECTED,
                        null,
                        toLobbySnapshot()
                );
            }
            String gameName = disconnectedGameByNickname.get(request.nickname());
            if (gameName == null) {
                markConnected(request.nickname());
                return GameOperationResult.successLobby("Lobby aggiornata", toLobbySnapshot());
            }

            GameSession session = sessions.get(gameName);
            if (session == null || !session.containsNickname(request.nickname())) {
                disconnectedGameByNickname.remove(request.nickname());
                return GameOperationResult.failure(
                        "La partita precedente non e' piu' disponibile",
                        OperationErrorCode.GAME_NOT_FOUND,
                        null,
                        toLobbySnapshot()
                );
            }

            return session.rejoin(request.nickname());
        }

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

        if (disconnectedGameByNickname.containsKey(request.nickname())) {
            return GameOperationResult.failure(
                    "Il nickname '" + request.nickname() + "' appartiene a una sessione disconnessa",
                    OperationErrorCode.NICKNAME_DISCONNECTED,
                    null,
                    toLobbySnapshot()
            );
        }

        return session.join(request.nickname(), gameSetup);
    }

    /**
     * Retrieves a snapshot for a known game.
     *
     * @param request snapshot request
     * @return success with the game snapshot, or GAME_NOT_FOUND when absent
     */
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

    /**
     * Applies an offer choice to a game session.
     *
     * @param request offer-choice request
     * @return operation result, including GAME_NOT_FOUND for missing games
     */
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

    /**
     * Applies a card choice to a game session.
     *
     * @param request card-choice request
     * @return operation result, including GAME_NOT_FOUND for missing games
     */
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

    /**
     * Applies a pass command to a game session.
     *
     * @param request pass request
     * @return operation result, including GAME_NOT_FOUND for missing games
     */
    @Override
    public synchronized GameOperationResult pass(PassRequest request) {
        GameSession session = sessions.get(request.gameName());
        if (session == null) {
            return GameOperationResult.failure(
                    "La partita '" + request.gameName() + "' non esiste",
                    OperationErrorCode.GAME_NOT_FOUND,
                    null
            );
        }
        return session.pass(request.nickname());
    }

    /**
     * Registers an update sink for a game.
     *
     * @param request subscription request
     * @param updateSink sink receiving asynchronous updates; null is rejected
     *                   by the session
     * @return operation result
     */
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

    /**
     * Removes an update sink from a game.
     * A null sink is accepted by the session and results in no local removal.
     *
     * @param request unsubscription request
     * @param updateSink sink to remove, or null
     * @return operation result
     */
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

    /**
     * Records client liveness and applies resilience rules.
     * Joined heartbeats must reference an existing game and player; lobby
     * heartbeats must refer to a reserved nickname that has not joined a game.
     *
     * @param request heartbeat request
     * @return operation result describing whether the heartbeat was accepted
     */
    @Override
    public synchronized GameOperationResult heartbeat(HeartbeatRequest request) {
        if (request.joined()) {
            GameSession session = sessions.get(request.gameName());
            if (session == null) {
                return GameOperationResult.failure(
                        "La partita '" + request.gameName() + "' non esiste",
                        OperationErrorCode.GAME_NOT_FOUND,
                        null
                );
            }

            if (!session.joinedNicknames.contains(request.nickname()) ||
                    !request.gameName().equals(session.context.getGameName())){
                return GameOperationResult.failure(
                        "Il player '" + request.nickname() + "' non esiste nella partita" + request.gameName(),
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        null
                );
            }
        } else {
            if (!takenNicknames.contains(request.nickname()) ||
                    hasNicknameJoined(request.nickname())) {
                return GameOperationResult.failure(
                        "Il player '" + request.nickname() + "' non esiste o è gia entrato in una partita",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        null
                );
            }
        }

        markConnected(request.nickname());

        if (request.joined()) {
            sessions.get(request.gameName()).applyResilienceRules();
        }

        return GameOperationResult.success("Sono attivo", null);
    }

    /**
     * Marks a nickname as connected and clears pending disconnected state.
     * If the nickname belongs to a started game, the model connection status is
     * also refreshed.
     *
     * @param nickname nickname to mark connected
     */
    private void markConnected(String nickname) {
        lastHeartbeatByNickname.put(nickname, System.currentTimeMillis());
        disconnectedGameByNickname.remove(nickname);
        GameSession session = getSessionByNickname(nickname);
        if (session != null) {
            session.markPlayerConnected(nickname);
        }
    }

    /**
     * Marks a nickname as disconnected after heartbeat timeout.
     * Players in unfinished games are stored with their game name for rejoin;
     * lobby-only reservations are stored with a null game name.
     *
     * @param nickname nickname to mark disconnected
     */
    private void markDisconnected(String nickname) {
        lastHeartbeatByNickname.remove(nickname);
        GameSession session = getSessionByNickname(nickname);
        if (session != null && !session.finished()) {
            disconnectedGameByNickname.put(nickname, session.context.getGameName());
            session.markPlayerDisconnected(nickname);
            session.publishAndSnapshot("Giocatore disconnesso: " + nickname);
        } else if (session == null) {
            disconnectedGameByNickname.put(nickname, null);
        }
    }

    /**
     * Expires stale heartbeats and applies resilience rules to all sessions.
     * Empty heartbeat state skips expiry work but still checks sessions.
     */
    private synchronized void checkDisconnectedClients() {
        if (!lastHeartbeatByNickname.isEmpty()){
            long now = System.currentTimeMillis();

            List<String> expiredNicknames = lastHeartbeatByNickname.entrySet().stream()
                    .filter(entry -> now - entry.getValue() > NetworkDefaults.HEARTBEAT_TIMEOUT_MILLIS)
                    .map(Map.Entry::getKey)
                    .toList();

            expiredNicknames.forEach(this :: markDisconnected);
        }

        for (GameSession session : sessions.values()) {
            session.applyResilienceRules();
        }
    }

    /**
     * Checks whether a nickname is already part of any game session.
     *
     * @param nickname nickname to look up
     * @return true when a session contains the nickname
     */
    private boolean hasNicknameJoined (String nickname) {
        return sessions.values().stream().anyMatch(session -> session.containsNickname(nickname));
    }

    /**
     * Finds the session containing a nickname.
     *
     * @param nickname nickname to look up
     * @return matching session, or null when the nickname is lobby-only or
     *         unknown
     */
    private GameSession getSessionByNickname(String nickname) {
        return sessions.values().stream()
                .filter(session -> session.containsNickname(nickname))
                .findFirst()
                .orElse(null);
    }

    /**
     * Builds a lobby snapshot containing only games that can still be joined.
     *
     * @return current lobby snapshot
     */
    private LobbySnapshot toLobbySnapshot() {
        return new LobbySnapshot(
                sessions.values().stream()
                        .filter(GameSession::isJoinable)
                        .map(GameSession::toLobbySummary)
                        .toList()
        );
    }

    /**
     * Mutable server-side state for one game and its subscribers.
     */
    private final class GameSession {
        private final GameContext context;
        private GameFlowController controller;
        private final List<String> joinedNicknames;
        private final Map<String, Color> colorsByNickname;
        private final Set<GameUpdateSink> subscribers;
        private long singleConnectedSince;
        private String forcedWinnerNickname;

        /**
         * Creates an empty session around a game context.
         *
         * @param context game context created by setup
         */
        private GameSession(GameContext context) {
            this.context = context;
            this.joinedNicknames = new ArrayList<>();
            this.colorsByNickname = new LinkedHashMap<>();
            this.subscribers = new LinkedHashSet<>();
            this.singleConnectedSince = -1;
        }

        /**
         * Adds a player to the session and starts the controller when the game
         * becomes full.
         *
         * @param nickname joining nickname
         * @param gameSetup setup helper used to create players at start
         * @return operation result with a snapshot and update publication
         */
        private GameOperationResult join(String nickname, GameSetup gameSetup) {
            joinedNicknames.add(nickname);
            markConnected(nickname);

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

        /**
         * Restores a disconnected player and refreshes resilience state.
         *
         * @param nickname reconnecting nickname
         * @return successful operation result with a published snapshot
         */
        private GameOperationResult rejoin(String nickname) {
            markConnected(nickname);
            applyResilienceRules();
            return GameOperationResult.success(
                    "Giocatore ricollegato",
                    publishAndSnapshot("Giocatore ricollegato: " + nickname)
            );
        }

        /**
         * Checks whether this session contains a nickname.
         *
         * @param nickname nickname to check
         * @return true when the nickname joined this session
         */
        private boolean containsNickname(String nickname) {
            return joinedNicknames.contains(nickname);
        }

        /**
         * Checks whether this session can accept another player.
         *
         * @return true before the game starts and while seats remain
         */
        private boolean isJoinable() {
            return !started() && !isFull();
        }

        /**
         * Checks whether the joined player count reached the configured size.
         *
         * @return true when no more seats are available
         */
        private boolean isFull() {
            return joinedNicknames.size() >= context.getGameSize();
        }

        /**
         * Builds a lobby summary for this not-yet-started game.
         *
         * @return lobby game summary with available seats
         */
        private LobbyGameSummary toLobbySummary() {
            return new LobbyGameSummary(
                    context.getGameName(),
                    context.getGameSize(),
                    joinedNicknames.size(),
                    context.getGameSize() - joinedNicknames.size()
            );
        }

        /**
         * Applies an offer choice after checking game state, player membership
         * and resilience blocking rules.
         *
         * @param nickname requesting player nickname
         * @param offerTileIndex selected offer tile index
         * @return operation result with updated snapshot or validation failure
         */
        private GameOperationResult chooseOffer(String nickname, int offerTileIndex) {
            if (!started()) {
                return GameOperationResult.failure("La partita non e' ancora iniziata", toSnapshot());
            }

            Color playerColor = colorsByNickname.get(nickname);
            if (playerColor == null) {
                return GameOperationResult.failure(
                        "Giocatore sconosciuto '" + nickname + "'",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        toSnapshot()
                );
            }
            GameOperationResult blocked = updateConnectionBeforeCommand(nickname);
            if (blocked != null) {
                return blocked;
            }

            try {
                controller.chooseOffer(playerColor, offerTileIndex);
                return GameOperationResult.success(
                        "Tessera offerta scelta",
                        publishAndSnapshot("Tessera offerta scelta")
                );
            } catch (RuntimeException exception) {
                return GameOperationResult.failure(exception.getMessage(), toSnapshot());
            }
        }

        /**
         * Applies a card choice after checking game state, player membership and
         * resilience blocking rules.
         *
         * @param nickname requesting player nickname
         * @param cardIndex selected card index
         * @param topRow true for top row, false for bottom row
         * @return operation result with updated snapshot or validation failure
         */
        private GameOperationResult chooseCard(String nickname, int cardIndex, boolean topRow) {
            if (!started()) {
                return GameOperationResult.failure("La partita non e' ancora iniziata", toSnapshot());
            }

            Color playerColor = colorsByNickname.get(nickname);
            if (playerColor == null) {
                return GameOperationResult.failure(
                        "Giocatore sconosciuto '" + nickname + "'",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        toSnapshot()
                );
            }
            GameOperationResult blocked = updateConnectionBeforeCommand(nickname);
            if (blocked != null) {
                return blocked;
            }

            try {
                controller.chooseCard(playerColor, cardIndex, topRow);
                return GameOperationResult.success(
                        "Carta scelta",
                        publishAndSnapshot("Carta scelta")
                );
            } catch (RuntimeException exception) {
                return GameOperationResult.failure(exception.getMessage(), toSnapshot());
            }
        }

        /**
         * Applies a pass command after checking game state, player membership
         * and resilience blocking rules.
         *
         * @param nickname requesting player nickname
         * @return operation result with updated snapshot or validation failure
         */
        private GameOperationResult pass(String nickname) {
            if (!started()) {
                return GameOperationResult.failure("La partita non e' ancora iniziata", toSnapshot());
            }

            Color playerColor = colorsByNickname.get(nickname);
            if (playerColor == null) {
                return GameOperationResult.failure(
                        "Giocatore sconosciuto '" + nickname + "'",
                        OperationErrorCode.PLAYER_NOT_IN_GAME,
                        toSnapshot()
                );
            }
            GameOperationResult blocked = updateConnectionBeforeCommand(nickname);
            if (blocked != null) {
                return blocked;
            }

            try {
                controller.pass(playerColor);
                return GameOperationResult.success(
                        "Turno passato",
                        publishAndSnapshot("Turno passato")
                );
            } catch (RuntimeException exception) {
                return GameOperationResult.failure(exception.getMessage(), toSnapshot());
            }
        }

        /**
         * Adds an update sink to this session.
         * Null sinks and nicknames that are not in the session are rejected.
         *
         * @param nickname subscribing nickname
         * @param updateSink update sink to add
         * @return operation result with current snapshot
         */
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
            markConnected(nickname);
            applyResilienceRules();
            return GameOperationResult.success(
                    "Iscrizione agli aggiornamenti attiva",
                    toSnapshot()
            );
        }

        /**
         * Removes an update sink from this session.
         * A null sink is accepted as a no-op so cleanup calls can be idempotent.
         *
         * @param nickname unsubscribing nickname, currently not validated
         * @param updateSink update sink to remove, or null
         * @return successful operation result with current snapshot
         */
        private GameOperationResult unsubscribe(String nickname, GameUpdateSink updateSink) {
            if (updateSink != null) {
                subscribers.remove(updateSink);
            }
            return GameOperationResult.success(
                    "Iscrizione agli aggiornamenti disattivata",
                    toSnapshot()
            );
        }

        /**
         * Refreshes connection state and checks whether a command should be
         * blocked by finished or suspended game state.
         *
         * @param nickname nickname issuing a command
         * @return failure result when blocked, otherwise null
         */
        private GameOperationResult updateConnectionBeforeCommand(String nickname) {
            markConnected(nickname);
            applyResilienceRules();
            if (finished()) {
                return GameOperationResult.failure("La partita e' gia' terminata", toSnapshot());
            }
            if (isSuspended()) {
                return GameOperationResult.failure(
                        "Partita sospesa: e' rimasto collegato un solo giocatore",
                        toSnapshot()
                );
            }
            return null;
        }

        /**
         * Checks whether a nickname currently has a live heartbeat.
         *
         * @param nickname nickname to check
         * @return true when the heartbeat map contains the nickname
         */
        private boolean isConnected(String nickname) {
            return lastHeartbeatByNickname.containsKey(nickname);
        }

        /**
         * Counts joined players with live heartbeats.
         *
         * @return number of connected players in this session
         */
        private int connectedPlayers() {
            int count = 0;
            for (String nickname : joinedNicknames) {
                if (isConnected(nickname)) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Returns the only connected nickname when exactly one player remains
         * connected.
         *
         * @return single connected nickname, or null when none or many are
         *         connected
         */
        private String onlyConnectedNickname() {
            String only = null;
            for (String nickname : joinedNicknames) {
                if (isConnected(nickname)) {
                    if (only != null) {
                        return null;
                    }
                    only = nickname;
                }
            }
            return only;
        }

        /**
         * Checks whether the game is temporarily suspended for a single
         * connected player.
         *
         * @return true only for started, unfinished games with one connection
         */
        private boolean isSuspended() {
            return started() && !finished() && connectedPlayers() == 1;
        }

        /**
         * Applies disconnection rules.
         * A single connected player starts or advances the forced-win timeout;
         * with at least two connected players, disconnected active players are
         * skipped until a connected active player is found or the game finishes.
         */
        private void applyResilienceRules() {
            if (!started() || finished()) {
                return;
            }

            int connectedPlayers = connectedPlayers();
            if (connectedPlayers == 1) {
                String onlyConnected = onlyConnectedNickname();
                long now = System.currentTimeMillis();
                if (singleConnectedSince < 0) {
                    singleConnectedSince = now;
                    publishAndSnapshot("Partita sospesa: e' rimasto collegato solo " + onlyConnected);
                    return;
                }
                if (now - singleConnectedSince >= NetworkDefaults.SINGLE_PLAYER_WIN_TIMEOUT_MILLIS) {
                    forcedWinnerNickname = onlyConnected;
                    publishAndSnapshot("Timeout scaduto: " + onlyConnected + " vince la partita");
                }
                return;
            }

            singleConnectedSince = -1;
            if (connectedPlayers < 2) {
                return;
            }

            while (!finished()) {
                Color activeColor = currentState() == null ? null : currentState().getActivePlayer();
                String activeNickname = nicknameForColor(activeColor);
                if (activeNickname == null || isConnected(activeNickname)) {
                    return;
                }
                controller.skipCurrentTurn(activeColor);
                new ModelController(context).changePlayerConnectionStatus(activeColor, ConnectionStatus.SKIPPED);
                publishAndSnapshot("Turno saltato: " + activeNickname + " e' disconnesso");
            }
        }

        /**
         * Updates a model player from disconnected/skipped state after a
         * successful heartbeat or rejoin.
         *
         * @param nickname connected nickname
         */
        private void markPlayerConnected(String nickname) {
            Color color = colorsByNickname.get(nickname);
            if (color == null) {
                return;
            }

            ModelController modelController = new ModelController(context);
            ConnectionStatus status = modelController.getPlayerConnectionStatus(color);
            if (status == ConnectionStatus.DISCONNECTED) {
                modelController.changePlayerConnectionStatus(color, ConnectionStatus.CONNECTED);
            } else if (status == ConnectionStatus.SKIPPED) {
                modelController.changePlayerConnectionStatus(color, ConnectionStatus.REJOINED);
            }
        }

        /**
         * Updates a model player to disconnected or skipped state after
         * heartbeat expiry.
         *
         * @param nickname disconnected nickname
         */
        private void markPlayerDisconnected(String nickname) {
            Color color = colorsByNickname.get(nickname);
            if (color == null) {
                return;
            }

            ModelController modelController = new ModelController(context);
            ConnectionStatus status = modelController.getPlayerConnectionStatus(color);
            if (status == ConnectionStatus.CONNECTED) {
                modelController.changePlayerConnectionStatus(color, ConnectionStatus.DISCONNECTED);
            } else if (status == ConnectionStatus.REJOINED) {
                modelController.changePlayerConnectionStatus(color, ConnectionStatus.SKIPPED);
            }
        }

        /**
         * Publishes a message with the current snapshot and returns that
         * snapshot for the operation result.
         *
         * @param message update message
         * @return snapshot sent to subscribers
         */
        private GameSnapshot publishAndSnapshot(String message) {
            GameSnapshot snapshot = toSnapshot();
            publish(new GameViewUpdate(context.getGameName(), message, snapshot));
            return snapshot;
        }

        /**
         * Sends an update to all subscribers and removes sinks that throw
         * {@link IOException}.
         *
         * @param update update to publish
         */
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

        /**
         * Builds a full immutable snapshot of this session.
         * Before the game starts, active player fields and market choices are
         * empty/defaulted; after a forced win, the current state is hidden.
         *
         * @return current game snapshot
         */
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

        /**
         * Checks whether player setup has completed and the controller exists.
         *
         * @return true after the game starts
         */
        private boolean started() {
            return controller != null;
        }

        /**
         * Checks whether the game has ended naturally or through a forced
         * resilience winner.
         *
         * @return true when the game is finished
         */
        private boolean finished() {
            return forcedWinnerNickname != null || started() && controller.isGameFinished();
        }

        /**
         * Returns the current controller state.
         * No state is exposed before start or after a forced winner is set.
         *
         * @return current state, or null when unavailable
         */
        private GameState currentState() {
            return !started() || forcedWinnerNickname != null ? null : controller.getCurrentState();
        }

        /**
         * Computes the externally visible game status.
         *
         * @return LOGIN before start, END_GAME after finish, otherwise the
         *         controller state status
         */
        private GameStatus currentStatus() {
            if (!started()) {
                return GameStatus.LOGIN;
            }
            if (forcedWinnerNickname != null || controller.isGameFinished()) {
                return GameStatus.END_GAME;
            }
            return controller.getCurrentState().getStatus();
        }

        /**
         * Builds player summaries.
         * Before start, players have no color and zero resources; after a
         * forced win, the winner receives a display score above the current max.
         *
         * @return player summaries in model order, or join order before start
         */
        private List<PlayerSummary> summarizePlayers() {
            if (!started()) {
                return joinedNicknames.stream()
                        .map(nickname -> new PlayerSummary(nickname, null, 0, 0, 0, 0))
                        .toList();
            }

            int forcedWinnerScore = forcedWinnerNickname == null
                    ? 0
                    : context.getPlayers().stream().mapToInt(Player::getPrestigePoints).max().orElse(0) + 1;

            return context.getPlayers().stream()
                    .map(player -> new PlayerSummary(
                            player.getNickname(),
                            player.getColor(),
                            player.getFood(),
                            player.getNickname().equals(forcedWinnerNickname) ? forcedWinnerScore : player.getPrestigePoints(),
                            player.getTribe().getNumCharacters(),
                            player.getTribe().getNumBuildings(),
                            summarizeOwnedCards(player.getTribe().getCharacterCards()),
                            summarizeOwnedCards(player.getTribe().getBuildingCards())
                    ))
                    .toList();
        }

        /**
         * Builds summaries for all offer tiles used by the current game size.
         *
         * @return offer tile summaries with occupant nicknames resolved
         */
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

        /**
         * Builds summaries for the turn-order tile spaces.
         *
         * @return turn-order summaries with occupant nicknames resolved
         */
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

        /**
         * Builds card summaries for one market row.
         * Tribe cards are listed before building cards, with selection indexes
         * matching the client command contract.
         *
         * @param row market row to summarize
         * @return card summaries in selectable order
         */
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

        /**
         * Converts a concrete card into its network summary.
         *
         * @param card card to summarize
         * @param selectionIndex client-facing selection index
         * @return card summary
         * @throws IllegalStateException if the card type is not supported by
         *                               the network summary layer
         */
        private CardSummary summarizeCard(Card card, int selectionIndex) {
            return switch (card) {
                case BuildingCard buildingCard -> new CardSummary(
                        selectionIndex,
                        formatName(buildingCard.getClass().getSimpleName()),
                        buildingCard.getEra(),
                        false,
                        describeCard(buildingCard),
                        buildingCard.getFoodCost(),
                        buildingCard.getPrestigePoints(),
                        buildingCard.getPresentationId()
                );
                case EventCard eventCard -> new CardSummary(
                        selectionIndex,
                        formatName(eventCard.getClass().getSimpleName()),
                        eventCard.getEra(),
                        true,
                        describeCard(eventCard),
                        null,
                        null,
                        eventCard.getPresentationId()
                );
                case CharacterCard characterCard -> new CardSummary(
                        selectionIndex,
                        formatName(characterCard.getClass().getSimpleName()),
                        characterCard.getEra(),
                        false,
                        describeCard(characterCard),
                        null,
                        null,
                        characterCard.getPresentationId()
                );
                default -> throw new IllegalStateException("Unsupported card type: " + card.getClass().getSimpleName());
            };
        }

        private List<CardSummary> summarizeOwnedCards(List<? extends Card> cards) {
            List<CardSummary> summaries = new ArrayList<>();
            for (int index = 0; index < cards.size(); index++) {
                summaries.add(summarizeCard(cards.get(index), index));
            }
            return summaries;
        }

        /**
         * Builds a short human-readable description for a known card type.
         * Unknown card types deliberately fall back to an empty description.
         *
         * @param card card to describe
         * @return localized effect summary or an empty string
         */
        private String describeCard(Card card) {
            return switch (card) {
                case BuilderCard builderCard ->
                        "Building discount " + builderCard.getBuildingDiscount()
                                + ", end game +" + builderCard.getPrestigePoints() + " pp";
                case GathererCard gathererCard ->
                        "Sustenance discount " + gathererCard.getDiscount() + " food";
                case HunterCard hunterCard ->
                        hunterCard.isActiveHunter() ? "Active hunter" : "Hunter";
                case InventorCard inventorCard ->
                        "Icon " + inventorCard.getInventionIcon();
                case ShamanCard shamanCard ->
                        shamanCard.getStars() + " shamanic stars";
                case HuntEvent huntEvent ->
                        "+" + huntEvent.getPrestigePerHunter() + " pp and +" + huntEvent.getFoodPerHunter()
                                + " food per hunter";
                case CavePaintingEvent cavePaintingEvent ->
                        "With at least " + cavePaintingEvent.getThreshold() + " artists: +"
                                + cavePaintingEvent.getPointsPerArtist()
                                + " pp each, otherwise " + cavePaintingEvent.getPenalty() + " pp";
                case ShamanicRitualEvent shamanicRitualEvent ->
                        "Winners +" + shamanicRitualEvent.getWinnerBonus()
                                + " pp, losers " + shamanicRitualEvent.getLoserPenalty() + " pp";
                case SustenanceEvent sustenanceEvent ->
                        sustenanceEvent.getFoodPerCharacter() + " food per character, each deficit costs "
                                + sustenanceEvent.getPointPenalty() + " pp";
                case BuildingBonusEvent buildingBonusEvent ->
                        "On " + formatTrigger(buildingBonusEvent.getTrigger())
                                + ": " + formatName(buildingBonusEvent.getCharacterType().getSimpleName())
                                + " gives " + signed(buildingBonusEvent.getFoodBonus(), " food")
                                + ", +" + buildingBonusEvent.getPrestigeBonus() + " pp";
                case BuildingEndTurn buildingEndTurn ->
                        "End turn: +" + buildingEndTurn.getFoodBonus() + " food with a positive turn-order bonus";
                case BuildingOnCard buildingOnCard ->
                        buildingOnCard.isSetCompletion()
                                ? "When taking cards: +" + buildingOnCard.getFoodBonus() + " food if you complete a set"
                                : "Inventor pair: +" + buildingOnCard.getFoodBonus() + " food with a duplicated icon";
                case BuildingShamanicRitual buildingShamanicRitual ->
                        buildingShamanicRitual.getStarBonus() != 0
                                ? "Ritual: +" + buildingShamanicRitual.getStarBonus() + " stars before scoring"
                                : buildingShamanicRitual.getWinMultiplier() != 0
                                        ? "Ritual: the winner multiplies by x" + buildingShamanicRitual.getWinMultiplier()
                                        : "Ritual: cancel your penalty if you lose";
                case BuildingEndGame buildingEndGame ->
                        describeEndGameBuilding(buildingEndGame);
                default -> "";
            };
        }

        /**
         * Builds the specific description for an end-game building.
         *
         * @param buildingEndGame end-game building to describe
         * @return description, or an empty string when the building has no
         *         recognized scoring configuration
         */
        private String describeEndGameBuilding(BuildingEndGame buildingEndGame) {
            if (buildingEndGame.isDoubleBuildersPrestige()) {
                return "End game: double builders' prestige";
            }
            if (buildingEndGame.getCharacterSetBonusPrestige() != 0) {
                return "End game: +" + buildingEndGame.getCharacterSetBonusPrestige() + " pp for each complete character set";
            }
            if (buildingEndGame.getCharacterMultiplier() != 0 && buildingEndGame.getCharacterType() != null) {
                return "End game: +" + buildingEndGame.getCharacterMultiplier() + " pp per "
                        + formatName(buildingEndGame.getCharacterType().getSimpleName());
            }
            if (buildingEndGame.getPrestigeBonus() != 0) {
                return "End game: +" + buildingEndGame.getPrestigeBonus() + " pp";
            }
            return "";
        }

        /**
         * Converts a class-style name into a readable display name.
         *
         * @param rawName raw class or type name
         * @return formatted display name
         */
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

        /**
         * Formats a building trigger enum for display.
         *
         * @param trigger trigger to format
         * @return lowercase trigger label without the event prefix
         * @throws NullPointerException if trigger is null
         */
        private String formatTrigger(it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger trigger) {
            return trigger.name()
                    .replace("ON_EVENT_", "")
                    .replace('_', ' ')
                    .toLowerCase();
        }

        /**
         * Formats a signed numeric value with a suffix.
         *
         * @param value value to format
         * @param suffix suffix appended after the value
         * @return value with a leading plus sign when non-negative
         */
        private String signed(int value, String suffix) {
            if (value >= 0) {
                return "+" + value + suffix;
            }
            return value + suffix;
        }

        /**
         * Resolves a nickname from a player color.
         *
         * @param color player color, or null
         * @return matching nickname, or null when color is null or unknown
         */
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
}
