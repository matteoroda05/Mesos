# Software engineering project – Polytechnic University of Milan, 2025/2026

### Question list:
- `newEra()` method only in model or in controller as well?
- [TurnOrderTile](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Tiles/TurnOrderTile.java) and [TurnOrderSpace](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Tiles/TurnOrderSpace.java) argument
- **Error handling**


### Todo list:
- **Test classes**
- **Error handling**
- [CardLoader](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/CardLoader.java)
    - fix issues with loading gatherer cards from JSON file
- [Tribe](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Tribe.java)
    - fix/implement function that computes points from building effects
- [BuildingCard](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/BuildingCard.java)
    - conclude building effects under [Buildings](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/Buildings) package
        * [BuildingEndRound](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/Buildings/BuildingEndRound.java): add visitor that can allow extra card pick
- [GameSetup](Mesos/src/main/java/it/polimi/ingsw/mesos/Controller/GameSetup.java)
  - initialise first order of players in `TurnOrderTile`
- [ModelController](Mesos/src/main/java/it/polimi/ingsw/mesos/Controller/ModelController.java)
    - add methods to get needed variables from `GameContext`and remove `getGameContext()`
- [MarketRow](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/MarketRow.java)
    - remove method `takeCard()` after fixing its tests
- [ErrorStates](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Enums/ErrorStates.java)
  - Complete the enum
