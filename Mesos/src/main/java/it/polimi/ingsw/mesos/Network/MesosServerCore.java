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

public class MesosServerCore implements MesosServerService {
    private final CardLoader.CardData cardData;
    private final GameSetup gameSetup;
    private final Map<String, GameSession> sessions;
    private final Set<String> takenNicknames;
    private final Map<String, String> disconnectedGameByNickname;
    private final Map<String, Long> lastHeartbeatByNickname;
    private final ScheduledExecutorService resilienceTimer;

    public MesosServerCore() throws IOException {
        this(CardLoader.load());
    }

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

    private void markConnected(String nickname) {
        lastHeartbeatByNickname.put(nickname, System.currentTimeMillis());
        disconnectedGameByNickname.remove(nickname);
        GameSession session = getSessionByNickname(nickname);
        if (session != null) {
            session.markPlayerConnected(nickname);
        }
    }

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

    private boolean hasNicknameJoined (String nickname) {
        return sessions.values().stream().anyMatch(session -> session.containsNickname(nickname));
    }

    private GameSession getSessionByNickname(String nickname) {
        return sessions.values().stream()
                .filter(session -> session.containsNickname(nickname))
                .findFirst()
                .orElse(null);
    }

    private LobbySnapshot toLobbySnapshot() {
        return new LobbySnapshot(
                sessions.values().stream()
                        .filter(GameSession::isJoinable)
                        .map(GameSession::toLobbySummary)
                        .toList()
        );
    }

    private final class GameSession {
        private final GameContext context;
        private GameFlowController controller;
        private final List<String> joinedNicknames;
        private final Map<String, Color> colorsByNickname;
        private final Set<GameUpdateSink> subscribers;
        private long singleConnectedSince;
        private String forcedWinnerNickname;

        private GameSession(GameContext context) {
            this.context = context;
            this.joinedNicknames = new ArrayList<>();
            this.colorsByNickname = new LinkedHashMap<>();
            this.subscribers = new LinkedHashSet<>();
            this.singleConnectedSince = -1;
        }

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

        private GameOperationResult rejoin(String nickname) {
            markConnected(nickname);
            applyResilienceRules();
            return GameOperationResult.success(
                    "Giocatore ricollegato",
                    publishAndSnapshot("Giocatore ricollegato: " + nickname)
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

        private GameOperationResult unsubscribe(String nickname, GameUpdateSink updateSink) {
            if (updateSink != null) {
                subscribers.remove(updateSink);
            }
            return GameOperationResult.success(
                    "Iscrizione agli aggiornamenti disattivata",
                    toSnapshot()
            );
        }

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

        private boolean isConnected(String nickname) {
            return lastHeartbeatByNickname.containsKey(nickname);
        }

        private int connectedPlayers() {
            int count = 0;
            for (String nickname : joinedNicknames) {
                if (isConnected(nickname)) {
                    count++;
                }
            }
            return count;
        }

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

        private boolean isSuspended() {
            return started() && !finished() && connectedPlayers() == 1;
        }

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
            return forcedWinnerNickname != null || started() && controller.isGameFinished();
        }

        private GameState currentState() {
            return !started() || forcedWinnerNickname != null ? null : controller.getCurrentState();
        }

        private GameStatus currentStatus() {
            if (!started()) {
                return GameStatus.LOGIN;
            }
            if (forcedWinnerNickname != null || controller.isGameFinished()) {
                return GameStatus.END_GAME;
            }
            return controller.getCurrentState().getStatus();
        }

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
                            player.getTribe().getNumBuildings()
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
}
