package it.polimi.ingsw.mesos.View;

import it.polimi.ingsw.mesos.Controller.GameFlowController;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.GameSetup;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Controller.States.ActionState;
import it.polimi.ingsw.mesos.Controller.States.ChooseOfferState;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.GathererCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ShamanCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.MarketRow;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Tribe;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

public class ViewLocalCLI {
    private final Scanner scanner;
    private GameContext game_context;
    private GameFlowController game_flow;

    public ViewLocalCLI() {
        this(new Scanner(System.in));
    }

    ViewLocalCLI(Scanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("scanner cannot be null");
        }
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        new ViewLocalCLI().run();
    }

    public void run() {
        setupGame();

        while (!game_flow.isGameFinished()) {
            renderGame();
            handleTurn();
        }

        renderGame();
        printFinalScores();
    }

    private void setupGame() {
        printBanner("MESOS LOCAL CLI");
        String game_name = readNonBlankLine("Game name");
        int player_count = readInt("Number of players [2-5]", 2, 5);
        List<String> nicknames = readNicknames(player_count);

        GameSetup setup = new GameSetup();
        try {
            game_context = setup.createGame(game_name, player_count, CardLoader.load());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load cards", e);
        }

        setup.createMapOfPlayers(game_context, nicknames);
        game_flow = new GameFlowController(game_context);
    }

    private void handleTurn() {
        GameState state = game_flow.getCurrentState();
        try {
            if (state instanceof ChooseOfferState) {
                handleChooseOffer();
                return;
            }
            if (state instanceof ActionState) {
                handleChooseCard();
                return;
            }
        } catch (RuntimeException e) {
            System.out.println("Action rejected: " + e.getMessage());
            return;
        }

        throw new IllegalStateException("Unexpected interactive state: " + describeState(state));
    }

    private void handleChooseOffer() {
        ModelController controller = new ModelController(game_context);
        Color player = controller.getNextPlayerTurn();
        if (player == null) throw new IllegalStateException("No player available for offer selection");
        List<Integer> available_indices = getAvailableOfferIndices();

        printSubHeader("Offer Phase");
        System.out.println("Current player : " + player);
        System.out.println("Available tiles: " + available_indices);
        int choice = readChoiceFromList("Choose offer tile index", available_indices);
        game_flow.chooseOffer(player, choice);
    }

    private void handleChooseCard() {
        ModelController controller = new ModelController(game_context);
        OfferTile tile = controller.getNextPlayerOfferTile();
        if (tile == null) throw new IllegalStateException("No player available for card selection");

        Color player = tile.getOccupant();
        Player current_player = game_context.getPlayer(player);
        printSubHeader("Action Phase");
        System.out.println("Current player : " + player);
        System.out.println("Offer tile     : " + tile.getOfferTileId()
                + " | top=" + tile.getTopPicks()
                + " | bottom=" + tile.getBottomPicks()
                + " | food=" + tile.getFoodGain());

        List<Integer> top_choices = tile.getTopPicks() > 0
                ? getValidCardIndices(game_context.getGameTable().getTopRow(), current_player)
                : List.of();
        List<Integer> bottom_choices = tile.getBottomPicks() > 0
                ? getValidCardIndices(game_context.getGameTable().getBottomRow(), current_player)
                : List.of();

        boolean choose_top = readRowChoice(tile, top_choices, bottom_choices);
        MarketRow row = choose_top ? game_context.getGameTable().getTopRow() : game_context.getGameTable().getBottomRow();
        List<Integer> valid_choices = choose_top ? top_choices : bottom_choices;
        if (valid_choices.isEmpty()) {
            throw new IllegalStateException("No valid cards can be chosen from the selected row");
        }

        System.out.println("Available cards: " + valid_choices);
        int card_index = readChoiceFromList("Choose card index", valid_choices);
        game_flow.chooseCard(player, card_index, choose_top);
    }

    private boolean readRowChoice(OfferTile tile, List<Integer> top_choices, List<Integer> bottom_choices) {
        if (tile.getTopPicks() > 0 && tile.getBottomPicks() <= 0) {
            return true;
        }
        if (tile.getBottomPicks() > 0 && tile.getTopPicks() <= 0) {
            return false;
        }
        if (top_choices.isEmpty() && !bottom_choices.isEmpty()) {
            System.out.println("Top row has no valid picks for this player.");
            return false;
        }
        if (bottom_choices.isEmpty() && !top_choices.isEmpty()) {
            System.out.println("Bottom row has no valid picks for this player.");
            return true;
        }

        while (true) {
            System.out.print("Choose row [top/bottom] ");
            System.out.print("(top " + top_choices + ", bottom " + bottom_choices + "): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if ("top".equals(input)) {
                if (top_choices.isEmpty()) {
                    System.out.println("Top row has no valid cards for this player.");
                    continue;
                }
                return true;
            }
            if ("bottom".equals(input)) {
                if (bottom_choices.isEmpty()) {
                    System.out.println("Bottom row has no valid cards for this player.");
                    continue;
                }
                return false;
            }
            System.out.println("Please type 'top' or 'bottom'.");
        }
    }

    private void renderGame() {
        printBanner("GAME VIEW");
        System.out.println("Game  : " + game_context.getGameName());
        System.out.println("Round : " + game_context.getRoundNumber());
        System.out.println("State : " + describeState(game_flow.getCurrentState()));
        renderTurnOrder();
        renderOfferTrack();
        renderRow("Top row", game_context.getGameTable().getTopRow());
        renderRow("Bottom row", game_context.getGameTable().getBottomRow());
        renderPlayers();
        printRule();
    }

    private void renderTurnOrder() {
        printSubHeader("Turn Order");
        List<TurnOrderSpace> spaces = game_context.getGameTable().getTurnOrderTile().getSpaces();
        for (int i = 0; i < spaces.size(); i++) {
            TurnOrderSpace space = spaces.get(i);
            String occupant = space.getOccupant() == null ? "-" : space.getOccupant().name();
            System.out.printf("  [%d] bonus=%-2s occupant=%s%n", i, formatSigned(space.getBonus()), occupant);
        }
    }

    private void renderOfferTrack() {
        printSubHeader("Offer Track");
        for (int i = 0; i < game_context.getGameSize() + 2; i++) {
            OfferTile tile = game_context.getGameTable().getOfferTrack().getTile(i);
            String occupant = tile.isAvailable() ? "-" : tile.getOccupant().name();
            System.out.printf(
                    "  [%d] tile=%s  top=%d  bottom=%d  food=%d  occupant=%s%n",
                    i,
                    tile.getOfferTileId(),
                    tile.getTopPicks(),
                    tile.getBottomPicks(),
                    tile.getFoodGain(),
                    occupant
            );
        }
    }

    private void renderRow(String title, MarketRow row) {
        printSubHeader(title);

        List<TribeCard> tribe_cards = row.getTribeCards();
        for (int i = 0; i < tribe_cards.size(); i++) {
            System.out.printf("  [%d] %s%n", i, describeCard(tribe_cards.get(i)));
        }

        int offset = tribe_cards.size();
        List<BuildingCard> building_cards = row.getBuildingCards();
        for (int i = 0; i < building_cards.size(); i++) {
            System.out.printf("  [%d] %s%n", offset + i, describeCard(building_cards.get(i)));
        }
    }

    private void renderPlayers() {
        printSubHeader("Players");
        List<Player> players = game_context.getPlayers().stream()
                .sorted(Comparator.comparing(Player::getNickname))
                .toList();

        for (Player player : players) {
            System.out.printf(
                    "  %-12s color=%-6s food=%-3d prestige=%-3d %s%n",
                    player.getNickname(),
                    player.getColor(),
                    player.getFood(),
                    player.getPrestigePoints(),
                    summarizeTribe(player)
            );
            renderPlayerCharacters(player);
        }
    }

    private void printFinalScores() {
        printBanner("FINAL SCORES");
        List<Player> ranking = game_context.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getPrestigePoints).reversed())
                .toList();

        for (Player player : ranking) {
            System.out.printf(
                    "  %-12s color=%-6s score=%d%n",
                    player.getNickname(),
                    player.getColor(),
                    player.getPrestigePoints()
            );
        }
    }

    private String summarizeTribe(Player player) {
        Map<Class<? extends CharacterCard>, Integer> characters = player.getTribe().getNumCharactersOfEachType();
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<Class<? extends CharacterCard>, Integer> entry : characters.entrySet()) {
            if (entry.getValue() > 0) {
                joiner.add(entry.getKey().getSimpleName().replace("Card", "") + "=" + entry.getValue());
            }
        }
        joiner.add("Buildings=" + player.getTribe().getNumBuildings());
        return joiner.toString();
    }

    private void renderPlayerCharacters(Player player) {
        Tribe tribe = player.getTribe();
        Map<Class<? extends CharacterCard>, List<CharacterCard>> characters = tribe.getCharactersByType();
        List<String> descriptions = new ArrayList<>();

        addCharacterDescriptions(descriptions, characters.get(ShamanCard.class));
        addCharacterDescriptions(descriptions, characters.get(BuilderCard.class));
        addCharacterDescriptions(descriptions, characters.get(GathererCard.class));
        addCharacterDescriptions(descriptions, characters.get(HunterCard.class));
        addCharacterDescriptions(descriptions, characters.get(InventorCard.class));
        addCharacterDescriptions(descriptions, characters.get(ArtistCard.class));

        if (descriptions.isEmpty()) {
            System.out.println("    characters: none");
            return;
        }

        System.out.println("    characters: " + String.join(" | ", descriptions));
    }

    private void addCharacterDescriptions(List<String> descriptions, List<CharacterCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }

        for (CharacterCard card : cards) {
            descriptions.add(describeOwnedCharacter(card));
        }
    }

    private String describeOwnedCharacter(CharacterCard card) {
        if (card instanceof ShamanCard shaman) {
            return "Shaman(stars=" + shaman.getStars() + ")";
        }
        if (card instanceof BuilderCard builder) {
            return "Builder(discount=" + builder.getBuildingDiscount()
                    + ", prestige=" + builder.getPrestigePoints() + ")";
        }
        if (card instanceof GathererCard gatherer) {
            return "Gatherer(discount=" + gatherer.getDiscount() + ")";
        }
        if (card instanceof HunterCard hunter) {
            return "Hunter(active=" + hunter.isActiveHunter() + ")";
        }
        if (card instanceof InventorCard inventor) {
            return "Inventor(icon=" + inventor.getInventionIcon() + ")";
        }
        if (card instanceof ArtistCard) {
            return "Artist(pairScore=10)";
        }
        return card.getClass().getSimpleName().replace("Card", "");
    }

    private String describeCharacterTraits(CharacterCard card) {
        if (card instanceof ShamanCard shaman) {
            return "stars=" + shaman.getStars();
        }
        if (card instanceof BuilderCard builder) {
            return "discount=" + builder.getBuildingDiscount()
                    + " prestige=" + builder.getPrestigePoints();
        }
        if (card instanceof GathererCard gatherer) {
            return "discount=" + gatherer.getDiscount();
        }
        if (card instanceof HunterCard hunter) {
            return "active=" + hunter.isActiveHunter();
        }
        if (card instanceof InventorCard inventor) {
            return "icon=" + inventor.getInventionIcon();
        }
        if (card instanceof ArtistCard) {
            return "pairScore=10";
        }
        return "";
    }

    private String describeState(GameState state) {
        if (state == null) {
            return "FINISHED";
        }
        return state.getClass().getSimpleName();
    }

    private String describeCard(Card card) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%-18s", card.getClass().getSimpleName().replace("Card", "")));
        builder.append(" era=").append(card.getEra());

        if (card instanceof EventCard) {
            builder.append(" [event]");
        } else if (card instanceof CharacterCard character) {
            String traits = describeCharacterTraits(character);
            if (!traits.isBlank()) {
                builder.append(" ").append(traits);
            }
        } else if (card instanceof BuildingCard building) {
            builder.append(" cost=").append(building.getFoodCost());
            builder.append(" prestige=").append(building.getPrestigePoints());
            builder.append(" trigger=").append(building.getTrigger());
        }

        return builder.toString();
    }

    private List<String> readNicknames(int player_count) {
        java.util.ArrayList<String> nicknames = new java.util.ArrayList<>();
        for (int i = 1; i <= player_count; i++) {
            nicknames.add (readNonBlankLine("Nickname for player " + i));
        }
        return nicknames;
    }

    private String readNonBlankLine(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = scanner.nextLine().trim();
            if (!input.isBlank()) {
                return input;
            }
            System.out.println("Input cannot be blank.");
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " [" + min + "-" + max + "]: ");
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    System.out.println("Value out of range.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private String formatSigned(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private List<Integer> getAvailableOfferIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < game_context.getGameSize() + 2; i++) {
            if (game_context.getGameTable().getOfferTrack().getTile(i).isAvailable()) {
                indices.add(i);
            }
        }
        return indices;
    }

    private List<Integer> getValidCardIndices(MarketRow row, Player player) {
        List<Integer> indices = new ArrayList<>();
        List<TribeCard> tribe_cards = row.getTribeCards();
        for (int i = 0; i < tribe_cards.size(); i++) {
            if (!tribe_cards.get(i).isEvent()) {
                indices.add(i);
            }
        }

        int effective_discount = player.getTribe().buildersDiscount();
        List<BuildingCard> building_cards = row.getBuildingCards();
        for (int i = 0; i < building_cards.size(); i++) {
            int cost = Math.max(0, building_cards.get(i).getFoodCost() - effective_discount);
            if (cost <= player.getFood()) {
                indices.add(tribe_cards.size() + i);
            }
        }

        return indices;
    }

    private int readChoiceFromList(String prompt, List<Integer> valid_choices) {
        if (valid_choices.isEmpty()) {
            throw new IllegalArgumentException("valid_choices cannot be empty");
        }

        while (true) {
            int value = readInt(prompt, valid_choices.getFirst(), valid_choices.getLast());
            if (valid_choices.contains(value)) {
                return value;
            }
            System.out.println("Please choose one of " + valid_choices + ".");
        }
    }

    private void printBanner(String title) {
        System.out.println();
        printRule();
        System.out.println(centerTitle(title));
        printRule();
    }

    private void printSubHeader(String title) {
        System.out.println();
        System.out.println("-- " + title + " --");
    }

    private void printRule() {
        System.out.println("============================================================");
    }

    private String centerTitle(String title) {
        int width = 60;
        if (title.length() >= width) {
            return title;
        }
        int leftPadding = (width - title.length()) / 2;
        return " ".repeat(leftPadding) + title;
    }
}
