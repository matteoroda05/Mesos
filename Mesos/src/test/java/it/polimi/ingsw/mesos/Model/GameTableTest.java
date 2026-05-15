package it.polimi.ingsw.mesos.Model;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.EventCard;
import it.polimi.ingsw.mesos.Model.Cards.TribeCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingAfterEveryone;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Events.HuntEvent;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.Tiles.OfferTile;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderSpace;
import it.polimi.ingsw.mesos.Model.Tiles.TurnOrderTile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTableTest {

	@Test
	void constructorThrowsWhenOfferTrackIsNull() {
		TurnOrderTile turn_order_tile = createTurnOrderTile();
		BuildingDeck building_deck = createBuildingDeck(List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));
		TribeDeck tribe_deck = createTribeDeck(createHunters(Era.FIRST, 9));

		assertThrows(IllegalArgumentException.class,
				() -> new GameTable(null, turn_order_tile, building_deck, tribe_deck));
	}

	@Test
	void constructorThrowsWhenTurnOrderTileIsNull() {
		OfferTrack offer_track = createOfferTrack(2);
		BuildingDeck building_deck = createBuildingDeck(List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));
		TribeDeck tribe_deck = createTribeDeck(createHunters(Era.FIRST, 9));

		assertThrows(IllegalArgumentException.class,
				() -> new GameTable(offer_track, null, building_deck, tribe_deck));
	}

	@Test
	void constructorThrowsWhenBuildingDeckIsNull() {
		OfferTrack offer_track = createOfferTrack(2);
		TurnOrderTile turn_order_tile = createTurnOrderTile();
		TribeDeck tribe_deck = createTribeDeck(createHunters(Era.FIRST, 9));

		assertThrows(IllegalArgumentException.class,
				() -> new GameTable(offer_track, turn_order_tile, null, tribe_deck));
	}

	@Test
	void constructorThrowsWhenTribeDeckIsNull() {
		OfferTrack offer_track = createOfferTrack(2);
		TurnOrderTile turn_order_tile = createTurnOrderTile();
		BuildingDeck building_deck = createBuildingDeck(List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));

		assertThrows(IllegalArgumentException.class,
				() -> new GameTable(offer_track, turn_order_tile, building_deck, null));
	}

	@Test
	void gettersReturnExpectedComponents() {
		OfferTrack offer_track = createOfferTrack(2);
		TurnOrderTile turn_order_tile = createTurnOrderTile();
		BuildingDeck building_deck = createBuildingDeck(List.of(
				new BuildingAfterEveryone(Era.FIRST, 0, 1, 1),
				new BuildingAfterEveryone(Era.SECOND, 0, 2, 2)));
		TribeDeck tribe_deck = createTribeDeck(createHunters(Era.FIRST, 12));

		GameTable game_table = new GameTable(offer_track, turn_order_tile, building_deck, tribe_deck);

		assertSame(offer_track, game_table.getOfferTrack());
		assertSame(turn_order_tile, game_table.getTurnOrderTile());
		assertNotNull(game_table.getTopRow());
		assertNotNull(game_table.getBottomRow());
	}

	@Test
	void placeTotemOnOfferTrackThrowsWhenPlayerIsNull() {
		GameTable game_table = createGameTable(
				createHunters(Era.FIRST, 12),
				List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));

		assertThrows(IllegalArgumentException.class,
				() -> game_table.placeTotemOnOfferTrack(null, 0));
	}

	@Test
	void placeTotemOnOfferTrackThrowsWhenOfferTileIdIsOutOfBounds() {
		GameTable game_table = createGameTable(
				createHunters(Era.FIRST, 12),
				List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));
		Player player = new Player("alice", Color.BLUE, 2);

		assertThrows(IndexOutOfBoundsException.class,
				() -> game_table.placeTotemOnOfferTrack(player, 10));
	}

	@Test
	void placeTotemOnOfferTrackThrowsWhenOfferTileIsNotAvailable() {
		GameTable game_table = createGameTable(
				createHunters(Era.FIRST, 12),
				List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));
		Player first_player = new Player("alice", Color.BLUE, 2);
		Player second_player = new Player("bob", Color.YELLOW, 2);

		game_table.placeTotemOnOfferTrack(first_player, 0);

		assertThrows(IllegalStateException.class,
				() -> game_table.placeTotemOnOfferTrack(second_player, 0));
	}

	@Test
	void placeTotemOnOfferTrackOccupiesSelectedTileWithPlayerColor() {
		GameTable game_table = createGameTable(
				createHunters(Era.FIRST, 12),
				List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));
		Player player = new Player("alice", Color.BLUE, 2);

		game_table.placeTotemOnOfferTrack(player, 1);

		assertEquals(Color.BLUE, game_table.getOfferTrack().getTile(1).getOccupant());
		assertFalse(game_table.getOfferTrack().getTile(1).isAvailable());
	}

	@Test
	void beginNewEraMovesTopBuildingsToBottomClearsBottomAndRefillsTop() {
		BuildingAfterEveryone old_bottom_building = new BuildingAfterEveryone(Era.FIRST, 0, 4, 1);
		BuildingAfterEveryone first_era_top_one = new BuildingAfterEveryone(Era.FIRST, 0, 1, 1);
		BuildingAfterEveryone first_era_top_two = new BuildingAfterEveryone(Era.FIRST, 0, 2, 2);
		BuildingAfterEveryone second_era_top_one = new BuildingAfterEveryone(Era.SECOND, 0, 3, 3);
		BuildingAfterEveryone second_era_top_two = new BuildingAfterEveryone(Era.SECOND, 0, 5, 4);

		GameTable game_table = createGameTable(
				createHunters(Era.FIRST, 12),
				List.of(first_era_top_one, first_era_top_two, second_era_top_one, second_era_top_two));

		game_table.getBottomRow().placeCard(old_bottom_building);

		game_table.beginNewEra();

		assertIterableEquals(
				List.of(first_era_top_one, first_era_top_two),
				game_table.getBottomRow().getBuildingCards());
		assertIterableEquals(
				List.of(second_era_top_one, second_era_top_two),
				game_table.getTopRow().getBuildingCards());
	}

	@Test
	void visibleEventsReturnsOnlyBottomRowEventsWhenTopRowIsExcluded() {
		GameTable game_table = createGameTable(
				createHunters(Era.FIRST, 12),
				List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));
		HuntEvent bottom_event = new HuntEvent(Era.FIRST, 1, 1);
		HuntEvent top_event = new HuntEvent(Era.SECOND, 2, 1);

		game_table.getBottomRow().placeCard(bottom_event);
		game_table.getTopRow().placeCard(top_event);

		List<EventCard> visible_events = game_table.visibleEvents(false);

		assertIterableEquals(List.of(bottom_event), visible_events);
	}

	@Test
	void visibleEventsIncludesTopRowEventsWhenRequested() {
		GameTable game_table = createGameTable(
				createHunters(Era.FIRST, 12),
				List.of(new BuildingAfterEveryone(Era.FIRST, 0, 1, 1)));
		HuntEvent bottom_event = new HuntEvent(Era.FIRST, 1, 1);
		HuntEvent top_event = new HuntEvent(Era.SECOND, 2, 1);

		game_table.getBottomRow().placeCard(bottom_event);
		game_table.getTopRow().placeCard(top_event);

		List<EventCard> visible_events = game_table.visibleEvents(true);

		assertIterableEquals(List.of(bottom_event, top_event), visible_events);
	}

	@Test
	void newRoundMovesTopTribesToBottomRefillsTopAndReturnsFalseWhenEraDoesNotChange() {
		List<TribeCard> first_era_tribes = new ArrayList<>(createHunters(Era.FIRST, 15));
		GameTable game_table = createGameTable(
				first_era_tribes,
				List.of(
						new BuildingAfterEveryone(Era.FIRST, 0, 1, 1),
						new BuildingAfterEveryone(Era.SECOND, 0, 2, 2)));

		List<TribeCard> expected_moved_to_bottom = List.of(
				first_era_tribes.get(14),
				first_era_tribes.get(13),
				first_era_tribes.get(12),
				first_era_tribes.get(11),
				first_era_tribes.get(10),
				first_era_tribes.get(9));
		List<TribeCard> expected_new_top = List.of(
				first_era_tribes.get(5),
				first_era_tribes.get(4),
				first_era_tribes.get(3),
				first_era_tribes.get(2),
				first_era_tribes.get(1),
				first_era_tribes.get(0));

		boolean new_era_started = game_table.newRound(Era.FIRST);

		assertFalse(new_era_started);
		assertIterableEquals(expected_moved_to_bottom, game_table.getBottomRow().getTribeCards());
		assertIterableEquals(expected_new_top, game_table.getTopRow().getTribeCards());
	}

	@Test
	void newRoundReturnsTrueWhenTopRowRestorationDrawsDifferentEra() {
		List<TribeCard> mixed_tribes = new ArrayList<>();
		mixed_tribes.add(new HunterCard(Era.SECOND, 2, true));
		mixed_tribes.addAll(createHunters(Era.FIRST, 14));

		GameTable game_table = createGameTable(
				mixed_tribes,
				List.of(
						new BuildingAfterEveryone(Era.FIRST, 0, 1, 1),
						new BuildingAfterEveryone(Era.SECOND, 0, 2, 2),
						new BuildingAfterEveryone(Era.THIRD, 0, 3, 3)));

		boolean new_era_started = game_table.newRound(Era.FIRST);

		assertTrue(new_era_started);
	}

	private GameTable createGameTable(List<? extends TribeCard> tribe_cards, List<? extends BuildingCard> building_cards) {
		OfferTrack offer_track = createOfferTrack(2);
		TurnOrderTile turn_order_tile = createTurnOrderTile();
		BuildingDeck building_deck = createBuildingDeck(building_cards);
		TribeDeck tribe_deck = createTribeDeck(tribe_cards);
		return new GameTable(offer_track, turn_order_tile, building_deck, tribe_deck);
	}

	private OfferTrack createOfferTrack(int num_players) {
		List<OfferTile> offer_tiles = List.of(
				new OfferTile('B', 0, 1, 0, 2),
				new OfferTile('C', 1, 0, 0, 2),
				new OfferTile('E', 1, 1, 0, 2),
				new OfferTile('F', 2, 0, 0, 2));
		return new OfferTrack(offer_tiles, num_players);
	}

	private TurnOrderTile createTurnOrderTile() {
		return new TurnOrderTile(List.of(new TurnOrderSpace(1), new TurnOrderSpace(0), new TurnOrderSpace(-1)));
	}

	private TribeDeck createTribeDeck(List<? extends TribeCard> tribe_cards) {
		return new TribeDeck(2, new ArrayList<>(tribe_cards));
	}

	private BuildingDeck createBuildingDeck(List<? extends BuildingCard> building_cards) {
		return new BuildingDeck(new ArrayList<>(building_cards));
	}

	private List<HunterCard> createHunters(Era era, int count) {
		List<HunterCard> output = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			output.add(new HunterCard(era, 2, i % 2 == 0));
		}
		return output;
	}
}
