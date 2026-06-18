package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.EventLogEntry;
import it.polimi.ingsw.mesos.EventMessageFormatter;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-of-everything overlay that flashes a transient, non-blocking text popup
 * for each resolved game event. Each popup shows the event title and body and
 * is sized to its own text. Popups auto-dismiss after a few seconds and stack
 * with a slight offset so the most recent one is rendered in front and fully
 * visible while older ones peek out behind it.
 *
 * <p>All methods must be called on the JavaFX application thread.
 */
public final class MesosEventPopupOverlay {
    private static final Duration VISIBLE_DURATION = Duration.seconds(3);
    // Actual game-event rounds carry more text and matter more, so they linger.
    private static final Duration EVENT_VISIBLE_DURATION = Duration.seconds(7);
    private static final Duration FADE_DURATION = Duration.millis(450);
    private static final double STACK_STEP = 16.0;
    private static final int MAX_POPUPS = 5;
    // Fixed horizontal size, roughly the width of a "turn order tile return"
    // message; only the height varies to match each popup's text length.
    private static final double CARD_WIDTH = 360.0;

    private final StackPane container;
    private final List<Node> activePopups = new ArrayList<>();

    public MesosEventPopupOverlay() {
        this.container = new StackPane();
        this.container.getStyleClass().add("event-popup-layer");
        this.container.setAlignment(Pos.CENTER);
        this.container.setMouseTransparent(true);
        this.container.setPickOnBounds(false);
        this.container.setMinSize(0, 0);
        this.container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /**
     * Returns the overlay node to layer on top of the application root.
     *
     * @return the overlay container node
     */
    public Node getNode() {
        return container;
    }

    /**
     * Shows a transient popup describing the given event entry. No-op for a null
     * entry.
     *
     * @param entry resolved event to describe
     */
    public void show(EventLogEntry entry) {
        if (entry == null) {
            return;
        }

        while (activePopups.size() >= MAX_POPUPS) {
            Node evicted = activePopups.remove(0);
            container.getChildren().remove(evicted);
        }

        Region card = buildCard(entry);
        double offset = activePopups.size() * STACK_STEP;
        card.setTranslateX(offset);
        card.setTranslateY(offset);

        activePopups.add(card);
        container.getChildren().add(card); // added last => rendered in front (most visible)

        Duration visibleDuration = entry.isEventRound() ? EVENT_VISIBLE_DURATION : VISIBLE_DURATION;
        PauseTransition stay = new PauseTransition(visibleDuration);
        FadeTransition fade = new FadeTransition(FADE_DURATION, card);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        SequentialTransition sequence = new SequentialTransition(stay, fade);
        sequence.setOnFinished(event -> {
            container.getChildren().remove(card);
            activePopups.remove(card);
        });
        sequence.play();
    }

    private Region buildCard(EventLogEntry entry) {
        VBox card = new VBox(8);
        card.getStyleClass().add("event-popup");
        card.setAlignment(Pos.TOP_LEFT);
        // Fixed horizontal size; the height hugs the text content.
        card.setMinWidth(CARD_WIDTH);
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label(EventMessageFormatter.title(entry));
        title.getStyleClass().add("event-popup-title");
        title.setWrapText(true);
        card.getChildren().add(title);

        String bodyText = EventMessageFormatter.body(entry);
        if (bodyText != null && !bodyText.isBlank()) {
            Label body = new Label(bodyText);
            body.getStyleClass().add("event-popup-body");
            body.setWrapText(true);
            card.getChildren().add(body);
        }

        return card;
    }
}
