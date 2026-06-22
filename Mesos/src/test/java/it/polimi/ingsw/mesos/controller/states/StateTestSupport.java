package it.polimi.ingsw.mesos.controller.states;

import it.polimi.ingsw.mesos.controller.GameSetup;
import it.polimi.ingsw.mesos.controller.ModelController;
import it.polimi.ingsw.mesos.model.CardLoader;
import it.polimi.ingsw.mesos.model.GameContext;
import it.polimi.ingsw.mesos.model.Player;
import it.polimi.ingsw.mesos.model.enums.Color;

import java.io.IOException;
import java.util.List;

public class StateTestSupport {
    protected GameContext game_context;
    protected ModelController model_controller;
    protected List<Color> player_colors;

    public void setUp() {
        GameSetup game_setup = new GameSetup();
        try {
            game_context = game_setup.createGame("TestGame", 3, CardLoader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> player_names = List.of("Nicola", "Mathis", "Elia");
        game_setup.createMapOfPlayers(game_context, player_names);
        model_controller = new ModelController(game_context);
        
        player_colors = game_context.getPlayers().stream().map(Player::getColor).toList();
    }
}
