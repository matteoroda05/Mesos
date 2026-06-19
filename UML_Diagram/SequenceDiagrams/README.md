# Mesos Sequence Diagrams

These diagrams describe the network and multiplayer situations implemented by
Mesos. They follow the same scenario-oriented style as the previous year's
examples: client/server lifelines, grouped success paths, alternatives for
errors, loops for repeated behavior, and short explanatory notes.

| # | Situation | PlantUML source | Rendered diagram |
|---|---|---|---|
| 1 | Connection and nickname | [01-connection-and-nickname.puml](01-connection-and-nickname.puml) | [01-connection-and-nickname.svg](01-connection-and-nickname.svg) |
| 2 | Lobby and game creation | [02-lobby-and-game-creation.puml](02-lobby-and-game-creation.puml) | [02-lobby-and-game-creation.svg](02-lobby-and-game-creation.svg) |
| 3 | Join, color selection, and start | [03-join-color-and-start.puml](03-join-color-and-start.puml) | [03-join-color-and-start.svg](03-join-color-and-start.svg) |
| 4 | Subscription and live updates | [04-subscription-and-live-updates.puml](04-subscription-and-live-updates.puml) | [04-subscription-and-live-updates.svg](04-subscription-and-live-updates.svg) |
| 5 | Offer phase | [05-offer-phase.puml](05-offer-phase.puml) | [05-offer-phase.svg](05-offer-phase.svg) |
| 6 | Card phase and pass | [06-card-phase-and-pass.puml](06-card-phase-and-pass.puml) | [06-card-phase-and-pass.svg](06-card-phase-and-pass.svg) |
| 7 | Events and round transition | [07-events-and-round-transition.puml](07-events-and-round-transition.puml) | [07-events-and-round-transition.svg](07-events-and-round-transition.svg) |
| 8 | Disconnection and reconnection | [08-disconnection-and-reconnection.puml](08-disconnection-and-reconnection.puml) | [08-disconnection-and-reconnection.svg](08-disconnection-and-reconnection.svg) |
| 9 | Game end and new game | [09-game-end-and-new-game.puml](09-game-end-and-new-game.puml) | [09-game-end-and-new-game.svg](09-game-end-and-new-game.svg) |
| 10 | Multiple simultaneous games | [10-multiple-simultaneous-games.puml](10-multiple-simultaneous-games.puml) | [10-multiple-simultaneous-games.svg](10-multiple-simultaneous-games.svg) |

There is no chat diagram because the current Mesos network API does not
implement chat messages.
