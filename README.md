# Software engineering project – Polytechnic University of Milan, 2025/2026

### Question list:
- `BuildingEffect`
    - decide wether or not to use an interface, if so decide how to implement it
- `TurnOrderTile` and `TurnOrderSpace`


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
- `TribeDeck`
    - initialise cards from json file
- `BuildingCard`
    - conclude building effects under `Buildings` package
        * `BuildingEndRound`: add visitor that can allow extra card pick
- `PlayerController`
    - implement actual methods