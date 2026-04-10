# Software engineering project – Polytechnic University of Milan, 2025/2026

### Question list:
- `newEra()` method only in model or in controller as well?
- **Error handling**
- Where do we handle "build" errors?


### Todo list:
- **Test classes**
- **Error handling**
- [Cards](Mesos/src/main/resources/Cards.json)
    - correct cards
- [BuildingCard](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/BuildingCard.java)
    - conclude building effects under [Buildings](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/Buildings) package
        * [BuildingEndRound](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/Buildings/BuildingEndRound.java): add visitor that can allow extra card pick
- [GameSetup](Mesos/src/main/java/it/polimi/ingsw/mesos/Controller/GameSetup.java)
    - Select the correct amount of buildings for different player counts
    - Put final events at the end of the deck
- [ModelController](Mesos/src/main/java/it/polimi/ingsw/mesos/Controller/ModelController.java)
    - add methods to get needed variables from `GameContext`and remove `getGameContext()`
- [MarketRow](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/MarketRow.java)
    - remove method `takeCard()` after fixing its tests
- [ErrorStates](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Enums/ErrorStates.java)
    - Complete the enum
- [OfferTrack](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/OfferTrack.java)
    - method `getAvailableTiles()` could be removed
    - method `ensureTilesInitialized()` is useless

### NOT NEEDED TESTS:
- [TribeCard](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/TribeCard.java), [CharacterCard](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/CharacterCard.java) > already tested for in [CardTest](Mesos/src/test/java/it/polimi/ingsw/mesos/Model/Cards/CardTest.java)
- [EventCard](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/EventCard.java) > test single event classes
- [ArtistCard](Mesos/src/main/java/it/polimi/ingsw/mesos/Model/Cards/Characters/ArtistCard.java) > nothing to test for
