package it.polimi.ingsw.mesos.Network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class MesosServerCore implements MesosServerService {
    private final CardLoader.CardData cardData;
    private final GameSetup gameSetup;
    private final Map<String, GameSession> sessions;

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

    private boolean isNicknameTaken(String nickname) {
        return sessions.values().stream().anyMatch(session -> session.containsNickname(nickname));
    }

    private LobbySnapshot toLobbySnapshot() {
        return new LobbySnapshot(sessions.values().stream()
                .filter(GameSession::isJoinable)
                .map(GameSession::toLobbySummary)
                .toList());
    }

    private static final class GameSession {
        private final GameContext context;
        private final List<String> joinedNicknames;
        private final Map<String, Color> colorsByNickname;
        private final Set<GameUpdateSink> subscribers;
        private GameFlowController controller;

        private GameSession(GameContext context) {
            this.context = context;
            this.joinedNicknames = new ArrayList<>();
            this.colorsByNickname = new LinkedHashMap<>();
            this.subscribers = new LinkedHashSet<>();
        }

        private GameOperationResult join(String nickname, GameSetup gameSetup) {
            joinedNicknames.add(nickname);

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
            return started() && controller.isGameFinished();
        }

        private GameState currentState() {
            return !started() ? null : controller.getCurrentState();
        }

        private GameStatus currentStatus() {
            if (!started()) {
                return GameStatus.LOGIN;
            }
            if (controller.isGameFinished()) {
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

            return context.getPlayers().stream()
                    .map(player -> new PlayerSummary(
                            player.getNickname(),
                            player.getColor(),
                            player.getFood(),
                            player.getPrestigePoints(),
                            player.getTribe().getNumCharacters(),
                            player.getTribe().getNumBuildings(),
                            summarizeOwnedCards(player.getTribe().getCharacterCards()),
                            summarizeOwnedCards(player.getTribe().getBuildingCards())
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
