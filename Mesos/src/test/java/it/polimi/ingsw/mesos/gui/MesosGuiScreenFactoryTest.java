package it.polimi.ingsw.mesos.gui;

import it.polimi.ingsw.mesos.network.config.ConnectionType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesosGuiScreenFactoryTest {
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
    void connectionScreenUsesDedicatedRootClassForBlackBackground() throws Exception {
        runOnFxThread(() -> {
            AtomicReference<ConnectionType> chosenType = new AtomicReference<>();
            AtomicReference<String> chosenHost = new AtomicReference<>();
            MesosGuiScreenFactory factory = new MesosGuiScreenFactory(new ReadOnlyDoubleWrapper(1280).getReadOnlyProperty());

            Node root = factory.buildConnectionScreen((type, host) -> {
                chosenType.set(type);
                chosenHost.set(host);
            });

            StackPane container = (StackPane) root;
            assertTrue(container.getStyleClass().contains("connection-screen-root"));
            assertTrue(container.getStyleClass().contains("connection-screen-content"));

            VBox shell = (VBox) container.getChildren().getFirst();
            StackPane logoWrapper = (StackPane) shell.getChildren().getFirst();
            ImageView logo = (ImageView) logoWrapper.getChildren().getFirst();
            assertEquals(580.0, logoWrapper.getMaxWidth());
            assertTrue(logo.fitWidthProperty().isBound());

            @SuppressWarnings("unchecked")
            ComboBox<ConnectionType> connectionTypeBox = singleNode(container, ".combo-box", ComboBox.class);
            TextField hostField = singleNode(container, ".text-field", TextField.class);
            Button connectButton = singleNode(container, ".primary-button", Button.class);
            hostField.setText("10.0.0.5");
            connectionTypeBox.getSelectionModel().select(ConnectionType.RMI);
            connectButton.fire();

            assertEquals(ConnectionType.RMI, chosenType.get());
            assertEquals("10.0.0.5", chosenHost.get());
            return null;
        });
    }

    private static <T> T singleNode(Node root, String selector, Class<T> expectedType) {
        Set<Node> matches = root.lookupAll(selector);
        assertEquals(1, matches.size());
        return expectedType.cast(matches.iterator().next());
    }

    private static <T> T runOnFxThread(java.util.concurrent.Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }
}
