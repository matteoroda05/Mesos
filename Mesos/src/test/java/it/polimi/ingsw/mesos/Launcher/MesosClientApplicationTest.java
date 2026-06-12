package it.polimi.ingsw.mesos.Launcher;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesosClientApplicationTest {
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
    void setContentAppliesBlackShellOnlyForConnectionScreen() throws Exception {
        runOnFxThread(() -> {
            MesosClientApplication application = new MesosClientApplication();
            BorderPane appShell = new BorderPane();
            setField(application, "appShell", appShell);

            StackPane connectionScreen = new StackPane();
            connectionScreen.getStyleClass().add("connection-screen-root");
            application.setContent(connectionScreen);
            assertTrue(appShell.getStyleClass().contains("app-shell-connection"));

            application.setContent(new StackPane());
            assertFalse(appShell.getStyleClass().contains("app-shell-connection"));
            return null;
        });
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T runOnFxThread(java.util.concurrent.Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }
}
