package it.polimi.ingsw.mesos.Launcher;

import it.polimi.ingsw.mesos.gui.MesosAssetResolver;
import it.polimi.ingsw.mesos.gui.MesosGuiFlowController;
import it.polimi.ingsw.mesos.gui.MesosGuiScreenFactory;
import it.polimi.ingsw.mesos.gui.MesosGuiService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class MesosClientApplication extends Application implements MesosGuiFlowController.Host {
    private final MesosGuiService guiService = new MesosGuiService();
    private final MesosAssetResolver assetResolver = new MesosAssetResolver();

    private Stage stage;
    private BorderPane appShell;
    private StackPane rootStack;
    private VBox busyOverlay;
    private Label busyLabel;
    private Label statusBarLabel;
    private MesosGuiFlowController flowController;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        statusBarLabel = new Label("Ready.");
        statusBarLabel.getStyleClass().add("status-bar");

        appShell = new BorderPane();
        appShell.getStyleClass().add("app-shell");
        appShell.setPadding(new Insets(16));
        appShell.setMinSize(0, 0);
        appShell.setBottom(buildStatusBar());

        busyOverlay = buildBusyOverlay();
        rootStack = new StackPane(appShell, busyOverlay);
        rootStack.setMinSize(0, 0);
        busyOverlay.setVisible(false);
        busyOverlay.setManaged(false);

        Scene scene = new Scene(rootStack, 1480, 900);
        applyStylesheet(scene);

        flowController = new MesosGuiFlowController(
                this,
                guiService,
                assetResolver,
                new MesosGuiScreenFactory(rootStack.widthProperty())
        );
        guiService.setUpdateHandler(flowController::handleRemoteUpdate);

        stage.setTitle("MESOS");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(620);
        stage.setMinHeight(520);

        flowController.start();
        stage.show();
    }

    @Override
    public void stop() {
        guiService.close();
    }

    @Override
    public void setContent(Node node) {
        appShell.setCenter(node);
    }

    @Override
    public void updateStatus(String message) {
        statusBarLabel.setText(message);
    }

    @Override
    public void setBusy(boolean busy, String message) {
        busyOverlay.setVisible(busy);
        busyOverlay.setManaged(busy);
        if (busy && message != null) {
            busyLabel.setText(message);
        }
    }

    @Override
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("MESOS");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }

    @Override
    public boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("MESOS");
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private Node buildStatusBar() {
        HBox bar = new HBox(statusBarLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.getStyleClass().add("status-bar-shell");
        return bar;
    }

    private VBox buildBusyOverlay() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(84, 84);

        busyLabel = new Label("Working...");
        busyLabel.getStyleClass().add("view-title");

        VBox overlay = new VBox(16, indicator, busyLabel);
        overlay.getStyleClass().add("busy-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setFillWidth(false);
        return overlay;
    }

    private void applyStylesheet(Scene scene) {
        var stylesheet = MesosClientApplication.class.getResource("/it/polimi/ingsw/mesos/gui/mesos-gui.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }
}
