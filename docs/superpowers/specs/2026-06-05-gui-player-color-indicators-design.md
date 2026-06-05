# GUI Player Color Indicators Design

## Goal

Improve the game board HUD so the two bottom information panels start at the far left of the tabletop area, while still showing the local player's color clearly. Replace the standalone totem image beside the panels with compact color-dot indicators.

## Scope

This change covers only the current JavaFX GUI board in `Mesos/src/main/java/it/polimi/ingsw/mesos/gui/MesosGameBoardView.java` and any needed stylesheet support in `Mesos/src/main/resources/it/polimi/ingsw/mesos/gui/mesos-gui.css`.

The later task of showing all player-owned cards as cards is out of scope for this design.

## Layout

The bottom information bar will remove the standalone `totemWrapper` column. Its children will become the main compact info panel followed by the owned-cards summary panel. This moves both panels left by the width previously reserved for the totem image.

The main info panel title row will show a small colored circle followed by the game name. The subtitle remains below that row.

## Color Indicators

A reusable color-dot builder will map the model color enum to a JavaFX fill color. The dot will be small, fixed-size, and circular so it behaves predictably inside scaled board layouts.

The info panel dot will use the local player's color when the local player exists in the snapshot. If that cannot be found, it will fall back to the snapshot's current player color when available.

Each occupied turn-order entry will show the occupant's color dot beside the nickname. Empty turn-order entries will stay text-only as `Empty`.

## Data Flow

`updateSnapshot` will continue to update labels and rebuild dynamic sections. The color dot in the info panel will be a reusable node container that is refreshed when the snapshot changes. Turn-order dots will be created as part of each rebuilt turn-order entry.

No network, model, or controller changes are required.

## Error Handling

If the snapshot is unavailable or a color is missing, the info panel color indicator will be hidden or replaced with a neutral fallback. Missing player data must not prevent the board from rendering.

## Testing

Add focused tests where the existing test structure supports GUI view construction. The tests should verify that:

- the bottom bar no longer includes the standalone totem column;
- the info panel includes a player-color indicator;
- occupied turn-order entries include color indicators;
- empty turn-order entries do not show player color dots.

If JavaFX test setup makes structural tests impractical, verify with Maven compilation/tests and a manual GUI run or screenshot check.
