# Software engineering project – Polytechnic University of Milan, 2025/2026

### Question list:
- `newEra()` method only in model or in controller as well?
- `TurnOrderTile` and `TurnOrderSpace`


### Todo list:
- **Error handling**
- **Test classes**
- `CardLoader`
    - fix issues with loading gatherer cards from JSON file
- `Tribe`
    - fix/implement function that computes points from building effects
- `BuildingCard`
    - conclude building effects under `Buildings` package
        * `BuildingEndRound`: add visitor that can allow extra card pick
- `PlayerController`
    - implement actual methods
- `GameSetup`
  - initialise first order of players in `TurnOrderTile`
- `ModelController`
    - add methods to get needed variables from `GameContext`and remove `getGameContext()`
- `MarketRow`
    - remove method `takeCard()` after fixing its tests
