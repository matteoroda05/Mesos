# Software engineering project – Polytechnic University of Milan, 2025/2026

### Question list:
- `BuildingEffect`
    - decide wether or not to use an interface, if so decide how to implement it
- `TurnOrderTile` and `TurnOrderSpace`
- `instanceOf` and `switch` for polymorfism:
    - how often can we use them
    - possible alternatives


### Todo list:
- **Test classes**
- Loader for every card/tile
    - tool Jackson to load every card
    - adjust constructors of `GameContext` and every other container to receive cards and tiles
- `OfferTrack`
    - populate offer track with correct tiles–from json–and check correct number
- `BuildingDeck`
    - initialise deck with cards from json file
- `Tribe`
    - fix/implement function that computes points from building effects
    - downcast issue
- `TribeDeck`
    - use type enum instead of instanceof
    - initialise cards from json file
    -removed, we add those points in controller
- [BuildingCard](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/BuildingCard.java)
    - conclude building effects under [Buildings](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/Buildings) package
        * [BuildingEndRound](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/Buildings/BuildingEndRound.java): add visitor that can allow extra card pick
- [GameSetup](Mesos/src/main/java/it/polimi/ingsw/mesos/Controller/GameSetup.java)
  - initialise first order of players in `TurnOrderTile`
  - initial food already present in [Globals](Mesos/src/main/java/it/polimi/ingsw/mesos/Globals.java)
- [ModelController](Mesos/src/main/java/it/polimi/ingsw/mesos/Controller/ModelController.java)
    - add methods to get needed variables from `GameContext`and remove `getGameContext()`
- [MarketRow](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/MarketRow.java)
    - remove method `takeCard()` after fixing its tests
- [ErrorStates](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Enums/ErrorStates.java)
  - Complete the enum
- [OfferTrack](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/OfferTrack.java)
  - method `getAvailableTiles()` could be removed
  - method `ensureTilesInitialized()` is useless
