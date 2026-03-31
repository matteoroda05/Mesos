package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;

abstract public class GameState {
    protected final ModelController controller;

    public GameState(ModelController controller) {
        this.controller = controller;
    }

    abstract public GameState chooseCard(Color player_color, int card, boolean is_top);

    abstract public GameState chooseOffer(Color player_color, int offer_tile);

    public GameState process() { return this; }

    public abstract GameStatus getStatus();

    public Color getActivePlayer() {
        return null;
    }

    public int getRemainingTopChoices() {
        return 0;
    }

    public int getRemainingBottomChoices() {
        return 0;
    }
}
