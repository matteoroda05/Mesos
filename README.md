# Software engineering project – Polytechnic University of Milan, 2025/2026

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
    - create OfferTile class
        - **decide wether to have it as a public class or an inner class of OfferTrack**
    - populate offer track with correct number of tiles
    - finish methods
- BuildingDeck
    - initialise deck with cards from json file
- BuildingEffect
    - decide wether or not to use an interface, if so decide how to implement it
- Card
    - use Integer/final int for id instead of UUID
- GameContext
    - take out decks and put them in GameTable
    - add getters
    - add computeFinalScores method
- Tribe
    - fix/implement function that computes points from building effects
    - downcast issue
- TribeDeck 
    - use type enum instead of instanceof



