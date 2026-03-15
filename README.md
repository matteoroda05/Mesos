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
    - populate offer track with correct number of tiles
- `BuildingDeck`
    - initialise deck with cards from json file
- `Tribe`
    - fix/implement function that computes points from building effects
    - downcast issue
- `Card`
    - use Integer/final int for id instead of UUID
- `TribeDeck`
    - use type enum instead of instanceof