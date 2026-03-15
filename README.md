# Software engineering project – Polytechnic University of Milan, 2025/2026

### Question list:
- BuildingEffect
    - decide wether or not to use an interface, if so decide how to implement it
- TurnOrderTile and TurnOrderSpace

### Todo list:
- **Test classes**
- GameTabele
    - add getters and methods
    - add tribe and building decks in class (from GameContext)
    - fix MarketRow constructor call
    - create TurnOrder class 
- MarketRow
    - add getters and methods
- OfferTrack
    - populate offer track with correct number of tiles
    - finish methods
- BuildingDeck
    - initialise deck with cards from json file
- Tribe
    - fix/implement function that computes points from building effects
    - downcast issue
- Card
    - use Integer/final int for id instead of UUID
- GameContext
    - take out decks and put them in GameTable
    - add getters
    - add computeFinalScores method
- TribeDeck 
    - use type enum instead of instanceof