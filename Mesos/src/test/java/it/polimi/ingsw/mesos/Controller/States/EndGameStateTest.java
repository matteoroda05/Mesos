package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Controller.GameState;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EndGameStateTest extends StateTestSupport {

    private EndGameState state;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        state = new EndGameState(model_controller);
    }

    @Test
    void getStatusReturnsEndGame() {
        assertEquals(GameStatus.END_GAME, state.getStatus());
    }

    @Test
    void processReturnsNull() {
        GameState nextState = state.process();
        assertNull(nextState);
    }

    @Test
    void processAppliesTribePrestigeAndEndGameBuildings() {
        CountingEndGameBuilding building = new CountingEndGameBuilding();
        EndGameSpyController spy = new EndGameSpyController(game_context, List.of(building));

        GameState nextState = new EndGameState(spy).process();

        assertNull(nextState);
        assertEquals(player_colors.size(), spy.prestigeChanges.size());
        assertEquals(1, building.calls);
    }

    private static final class EndGameSpyController extends ModelController {
        private final Map<Color, Tribe> tribes;
        private final List<BuildingCard> buildings;
        private final Map<Color, Integer> prestigeChanges = new HashMap<>();

        private EndGameSpyController(GameContext context, List<BuildingCard> buildings) {
            super(context);
            this.tribes = context.getPlayers().stream()
                    .collect(Collectors.toMap(player -> player.getColor(), player -> player.getTribe()));
            this.buildings = buildings;
        }

        @Override
        public Map<Color, Tribe> getTribes() {
            return tribes;
        }

        @Override
        public void playerChangePrestige(int prestige_amount, Color player_color) {
            prestigeChanges.merge(player_color, prestige_amount, Integer::sum);
        }

        @Override
        public List<BuildingCard> getBuildingsWithTrigger(BuildingTrigger trigger) {
            return trigger == BuildingTrigger.ON_END_GAME ? buildings : List.of();
        }
    }

    private static final class CountingEndGameBuilding extends BuildingEndGame {
        private int calls;

        private CountingEndGameBuilding() {
            super(Era.FIRST, 2, 0, 0, false, 0, 0, null, 25);
        }

        @Override
        public void effect(ModelController visitor) {
            calls++;
        }
    }
}
