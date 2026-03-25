package it.polimi.ingsw.mesos.Controller;

import it.polimi.ingsw.mesos.Model.Enums.Color;

abstract public class GameState {
    // TODO: make controller "static final" to ensure immutability and sharing between instances
    protected final ModelController controller;
    protected static int round = 1;

    public GameState(ModelController controller) {
        this.controller = controller;
    }

    abstract public GameState chooseCard(Color player_color, int card, boolean is_top);

    abstract public GameState chooseOffer(Color player_color, int offer_tile);

    public GameState process() { return this; }
}
