package it.polimi.ingsw.mesos.model.cards.buildings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.mesos.controller.ModelController;
import it.polimi.ingsw.mesos.model.BuildingDeck;
import it.polimi.ingsw.mesos.model.cards.characters.ArtistCard;
import it.polimi.ingsw.mesos.model.cards.characters.BuilderCard;
import it.polimi.ingsw.mesos.model.cards.characters.GathererCard;
import it.polimi.ingsw.mesos.model.cards.characters.HunterCard;
import it.polimi.ingsw.mesos.model.cards.characters.InventorCard;
import it.polimi.ingsw.mesos.model.cards.characters.ShamanCard;
import it.polimi.ingsw.mesos.model.enums.Color;
import it.polimi.ingsw.mesos.model.enums.Era;
import it.polimi.ingsw.mesos.model.enums.InventionIcon;
import it.polimi.ingsw.mesos.model.GameContext;
import it.polimi.ingsw.mesos.model.GameTable;
import it.polimi.ingsw.mesos.model.OfferTrack;
import it.polimi.ingsw.mesos.model.Player;
import it.polimi.ingsw.mesos.model.Tribe;
import it.polimi.ingsw.mesos.model.TribeDeck;
import it.polimi.ingsw.mesos.model.tiles.OfferTile;
import it.polimi.ingsw.mesos.model.tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.model.tiles.TurnOrderTile;

abstract class BuildingTestSupport {

    protected record Fixture(ModelController controller, Player owner, Player other, GameTable table) {}

    protected Fixture createFixture(int... turnOrderBonuses) {
        List<TurnOrderSpace> spaces = new ArrayList<>();
        if (turnOrderBonuses.length == 0) {
            spaces.add(new TurnOrderSpace(0));
            spaces.add(new TurnOrderSpace(0));
        } else {
            for (int bonus : turnOrderBonuses) {
                spaces.add(new TurnOrderSpace(bonus));
            }
        }

        GameTable table = new GameTable(
                new OfferTrack(List.of(
                        new OfferTile('A', 1, 0, 0, 2),
                        new OfferTile('B', 1, 1, 0, 2),
                        new OfferTile('C', 0, 1, 0, 2),
                        new OfferTile('D', 0, 2, 0, 2)), 2),
                new TurnOrderTile(spaces),
                new BuildingDeck(List.of(new BuildingAfterEveryone(Era.FIRST, 2, 1, 1))),
                new TribeDeck(2, List.of(new ArtistCard(Era.FIRST, 2))));

        GameContext context = new GameContext("test", 2, table);
        Player owner = new Player("owner", Color.BLUE, 10);
        Player other = new Player("other", Color.YELLOW, 10);
        Map<Color, Player> players = new EnumMap<>(Color.class);
        players.put(owner.getColor(), owner);
        players.put(other.getColor(), other);
        context.setupPlayers(players);

        return new Fixture(new ModelController(context), owner, other, table);
    }

    protected void addOneOfEachCharacter(Tribe tribe) {
        tribe.addCard(new ArtistCard(Era.FIRST, 2));
        tribe.addCard(new BuilderCard(Era.FIRST, 2, 2, 1));
        tribe.addCard(new GathererCard(Era.FIRST, 2));
        tribe.addCard(new HunterCard(Era.FIRST, 2, true));
        tribe.addCard(new InventorCard(Era.FIRST, 2, InventionIcon.BOAT));
        tribe.addCard(new ShamanCard(Era.FIRST, 2, 2));
    }
}
