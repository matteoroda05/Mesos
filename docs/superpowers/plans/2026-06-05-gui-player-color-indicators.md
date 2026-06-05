# GUI Player Color Indicators Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the board information panels to the far left and replace the standalone player totem image with compact color-dot indicators in the info panel and turn order entries.

**Architecture:** Keep the change local to the JavaFX board view. Add structural CSS classes that make the layout testable, use one reusable color-dot helper, and refresh the mutable info-panel indicator whenever a snapshot arrives.

**Tech Stack:** Java 23, JavaFX controls/graphics, JUnit 5, Maven.

---

## File Structure

- Create `Mesos/src/test/java/it/polimi/ingsw/mesos/gui/MesosGameBoardViewTest.java`
  - Owns JavaFX structural tests for the board HUD and turn-order indicators.
- Modify `Mesos/src/main/java/it/polimi/ingsw/mesos/gui/MesosGameBoardView.java`
  - Removes the standalone bottom-bar totem column.
  - Adds reusable color-dot indicator nodes.
  - Adds color dots to the info panel and occupied turn-order entries.
- Modify `Mesos/src/main/resources/it/polimi/ingsw/mesos/gui/mesos-gui.css`
  - Styles the reusable color-dot indicator classes.

---

### Task 1: Add Failing JavaFX Structural Tests

**Files:**
- Create: `Mesos/src/test/java/it/polimi/ingsw/mesos/gui/MesosGameBoardViewTest.java`

- [ ] **Step 1: Write the failing tests**

Create `Mesos/src/test/java/it/polimi/ingsw/mesos/gui/MesosGameBoardViewTest.java` with this content:

```java
package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.Controller.Enums.GameStatus;
import it.polimi.ingsw.mesos.Model.Enums.Color;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Network.GameSnapshot;
import it.polimi.ingsw.mesos.Network.PlayerSummary;
import it.polimi.ingsw.mesos.Network.TurnOrderSpaceSummary;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesosGameBoardViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS));
    }

    @Test
    void bottomInfoBarStartsWithPanelsAndShowsLocalPlayerColorIndicator() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshot(), "Offer tile selected.");

            HBox bottomBar = singleNode(view, ".bottom-info-bar", HBox.class);
            assertEquals(2, bottomBar.getChildren().size());
            assertTrue(bottomBar.getChildren().get(0).getStyleClass().contains("info-panel"));
            assertTrue(bottomBar.getChildren().get(1).getStyleClass().contains("owned-cards-debug-box"));
            assertEquals(1, view.lookupAll(".info-panel-color-indicator").size());
            return null;
        });
    }

    @Test
    void turnOrderEntriesShowColorIndicatorOnlyWhenOccupied() throws Exception {
        runOnFxThread(() -> {
            MesosGameBoardView view = buildView();

            view.updateSnapshot(snapshot(), "Offer tile selected.");

            assertEquals(1, view.lookupAll(".turn-order-color-indicator").size());
            assertEquals(2, view.lookupAll(".turn-order-tile-entry").size());
            return null;
        });
    }

    private static MesosGameBoardView buildView() {
        return new MesosGameBoardView(
                "dio",
                new MesosAssetResolver(),
                () -> { },
                () -> { },
                ignored -> { },
                (ignoredIndex, ignoredTopRow) -> { },
                () -> { }
        );
    }

    private static GameSnapshot snapshot() {
        List<PlayerSummary> players = List.of(
                new PlayerSummary("dio", Color.ORANGE, 3, 0, 0, 0),
                new PlayerSummary("Carlo2", Color.BLUE, 0, 0, 0, 0)
        );
        List<TurnOrderSpaceSummary> turnOrder = List.of(
                new TurnOrderSpaceSummary(0, 0, Color.ORANGE, "dio"),
                new TurnOrderSpaceSummary(1, 0, null, null)
        );
        return new GameSnapshot(
                "dio",
                2,
                2,
                true,
                false,
                GameStatus.ROUND_ACTIONS,
                1,
                Era.I,
                "dio",
                Color.ORANGE,
                1,
                0,
                List.of(Color.ORANGE, Color.BLUE),
                players,
                List.of(),
                turnOrder,
                List.of(),
                List.of(),
                false
        );
    }

    private static <T> T singleNode(MesosGameBoardView view, String selector, Class<T> expectedType) {
        Set<Node> matches = view.lookupAll(selector);
        assertEquals(1, matches.size());
        return expectedType.cast(matches.iterator().next());
    }

    private static <T> T runOnFxThread(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }
}
```

- [ ] **Step 2: Run the new tests to verify they fail**

Run:

```bash
cd Mesos && ./mvnw -Dtest=MesosGameBoardViewTest test
```

Expected: the test class compiles, then fails because `.bottom-info-bar`, `.info-panel-color-indicator`, and `.turn-order-color-indicator` do not exist yet.

- [ ] **Step 3: Keep the failing test uncommitted**

Leave `Mesos/src/test/java/it/polimi/ingsw/mesos/gui/MesosGameBoardViewTest.java` in the working tree. The red test proves the missing behavior, and the implementation task will commit the test together with the production code after the focused test passes.

---

### Task 2: Implement Board Color Indicators

**Files:**
- Modify: `Mesos/src/main/java/it/polimi/ingsw/mesos/gui/MesosGameBoardView.java`
- Modify: `Mesos/src/main/resources/it/polimi/ingsw/mesos/gui/mesos-gui.css`
- Test: `Mesos/src/test/java/it/polimi/ingsw/mesos/gui/MesosGameBoardViewTest.java`

- [ ] **Step 1: Add JavaFX imports and replace the totem wrapper field**

In `Mesos/src/main/java/it/polimi/ingsw/mesos/gui/MesosGameBoardView.java`, add this import beside the other shape imports:

```java
import javafx.scene.shape.Circle;
```

Replace:

```java
private final VBox totemWrapper;
```

with:

```java
private final StackPane infoPanelColorIndicator;
```

- [ ] **Step 2: Initialize the new indicator**

Replace this constructor line:

```java
this.totemWrapper = new VBox(4);
```

with:

```java
this.infoPanelColorIndicator = buildColorIndicator(null, "info-panel-color-indicator");
this.infoPanelColorIndicator.setVisible(false);
this.infoPanelColorIndicator.setManaged(false);
```

- [ ] **Step 3: Refresh the info-panel indicator from snapshots**

Replace this line in `updateSnapshot`:

```java
rebuildTotemWrapper();
```

with:

```java
refreshInfoPanelColorIndicator();
```

Delete the existing `rebuildTotemWrapper` method and add this method in its place:

```java
private void refreshInfoPanelColorIndicator() {
    Optional<it.polimi.ingsw.mesos.Model.Enums.Color> indicatorColor = resolveInfoPanelColor();
    boolean hasColor = indicatorColor.isPresent();
    infoPanelColorIndicator.setVisible(hasColor);
    infoPanelColorIndicator.setManaged(hasColor);
    indicatorColor.ifPresent(color -> updateColorIndicator(infoPanelColorIndicator, color));
}

private Optional<it.polimi.ingsw.mesos.Model.Enums.Color> resolveInfoPanelColor() {
    if (snapshot == null) {
        return Optional.empty();
    }
    Optional<it.polimi.ingsw.mesos.Model.Enums.Color> localPlayerColor = snapshot.players().stream()
            .filter(player -> player.nickname().equals(localNickname))
            .map(PlayerSummary::color)
            .filter(color -> color != null)
            .findFirst();
    return localPlayerColor.or(() -> Optional.ofNullable(snapshot.currentPlayerColor()));
}
```

- [ ] **Step 4: Remove the standalone bottom-bar totem column**

In `buildTabletopShell`, delete this block:

```java
// Player totem wrapper (left side, fixed identifier)
totemWrapper.setAlignment(Pos.TOP_CENTER);
totemWrapper.setPrefWidth(50);
totemWrapper.setMinWidth(50);
totemWrapper.setMaxWidth(50);
```

Add a structural CSS class to the bottom bar:

```java
bottomInfoBar.getStyleClass().add("bottom-info-bar");
```

Replace:

```java
bottomInfoBar.getChildren().addAll(totemWrapper, buildCompactInfoPanel(), ownedCardsDebugBox);
```

with:

```java
bottomInfoBar.getChildren().addAll(buildCompactInfoPanel(), ownedCardsDebugBox);
```

- [ ] **Step 5: Add the color dot to the info panel title row**

Replace the first line of `buildCompactInfoPanel`:

```java
VBox titleSection = new VBox(2, titleLabel, subtitleLabel);
```

with:

```java
HBox titleRow = new HBox(6, infoPanelColorIndicator, titleLabel);
titleRow.setAlignment(Pos.CENTER_LEFT);
VBox titleSection = new VBox(2, titleRow, subtitleLabel);
```

- [ ] **Step 6: Add color dots to occupied turn-order entries**

In `buildMinimalTurnOrderEntry`, replace:

```java
entry.getChildren().add(nameLabel);
```

with:

```java
if (space.occupantNickname() != null) {
    entry.getChildren().add(buildColorIndicator(space.occupantColor(), "turn-order-color-indicator"));
}
entry.getChildren().add(nameLabel);
```

- [ ] **Step 7: Add reusable color-dot helper methods**

Add these methods before `buildTotem`:

```java
private StackPane buildColorIndicator(
        it.polimi.ingsw.mesos.Model.Enums.Color color,
        String extraStyleClass
) {
    StackPane indicator = new StackPane();
    indicator.getStyleClass().add("player-color-indicator");
    if (extraStyleClass != null && !extraStyleClass.isBlank()) {
        indicator.getStyleClass().add(extraStyleClass);
    }
    indicator.setMinSize(12, 12);
    indicator.setPrefSize(12, 12);
    indicator.setMaxSize(12, 12);

    Circle dot = new Circle(5);
    dot.getStyleClass().add("player-color-indicator-dot");
    indicator.getChildren().add(dot);
    updateColorIndicator(indicator, color);
    return indicator;
}

private void updateColorIndicator(StackPane indicator, it.polimi.ingsw.mesos.Model.Enums.Color color) {
    if (!indicator.getChildren().isEmpty() && indicator.getChildren().getFirst() instanceof Circle dot) {
        dot.setFill(toIndicatorColor(color));
        dot.setStroke(Color.rgb(255, 236, 201, 0.72));
        dot.setStrokeWidth(1);
    }
}

private Color toIndicatorColor(it.polimi.ingsw.mesos.Model.Enums.Color color) {
    return switch (color == null ? it.polimi.ingsw.mesos.Model.Enums.Color.WHITE : color) {
        case BLUE -> Color.rgb(57, 146, 218);
        case YELLOW -> Color.rgb(244, 196, 48);
        case ORANGE -> Color.rgb(230, 92, 36);
        case WHITE -> Color.rgb(239, 232, 214);
        case BLACK -> Color.rgb(94, 53, 137);
    };
}
```

- [ ] **Step 8: Style the color indicators**

Add this block to `Mesos/src/main/resources/it/polimi/ingsw/mesos/gui/mesos-gui.css` near the existing `.info-panel` styles:

```css
.player-color-indicator {
    -fx-background-color: rgba(255, 255, 255, 0.12);
    -fx-background-radius: 999;
    -fx-border-color: rgba(255, 236, 201, 0.36);
    -fx-border-radius: 999;
    -fx-padding: 1;
}

.player-color-indicator-dot {
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.34), 3, 0.2, 0, 1);
}
```

- [ ] **Step 9: Run the focused test to verify it passes**

Run:

```bash
cd Mesos && ./mvnw -Dtest=MesosGameBoardViewTest test
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 10: Run the full test suite**

Run:

```bash
cd Mesos && ./mvnw test
```

Expected: Maven finishes with `BUILD SUCCESS`.

- [ ] **Step 11: Commit the green test and implementation**

```bash
git add Mesos/src/test/java/it/polimi/ingsw/mesos/gui/MesosGameBoardViewTest.java Mesos/src/main/java/it/polimi/ingsw/mesos/gui/MesosGameBoardView.java Mesos/src/main/resources/it/polimi/ingsw/mesos/gui/mesos-gui.css
git commit -m "feat: add board player color indicators"
```

---

## Self-Review

- Spec coverage: Task 2 removes the standalone totem column, moves the two panels left, adds the info-panel player color indicator, and adds turn-order occupant color indicators. Task 1 covers those behaviors with structural tests.
- Red-flag scan: The plan contains no deferred work markers, incomplete code blocks, or delayed implementation steps.
- Type consistency: The plan uses the existing `GameSnapshot`, `PlayerSummary`, `TurnOrderSpaceSummary`, `Color`, `Era`, and `GameStatus` record and enum names. The JavaFX indicator methods use `StackPane` and `Circle`, both imported or already present.
