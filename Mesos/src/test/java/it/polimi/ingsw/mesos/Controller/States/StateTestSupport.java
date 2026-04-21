package it.polimi.ingsw.mesos.Controller.States;

import it.polimi.ingsw.mesos.Controller.GameSetup;
import it.polimi.ingsw.mesos.Controller.ModelController;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.GameContext;
import it.polimi.ingsw.mesos.Model.Player;
import it.polimi.ingsw.mesos.Model.Enums.Color;

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
