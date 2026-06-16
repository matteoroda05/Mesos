package it.polimi.ingsw.mesos.Launcher;

import it.polimi.ingsw.mesos.Model.Enums.Color;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MesosClientApplication extends Application implements MesosGuiFlowController.Host {
    private static final String CONNECTION_SCREEN_MARKER = "connection-screen-root";
    private static final String CONNECTION_SCREEN_SHELL_CLASS = "app-shell-connection";

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
        appShell.getStyleClass().remove(CONNECTION_SCREEN_SHELL_CLASS);
        if (node != null && node.getStyleClass().contains(CONNECTION_SCREEN_MARKER)) {
            appShell.getStyleClass().add(CONNECTION_SCREEN_SHELL_CLASS);
        }
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
        alert.setGraphic(null);
        alert.getDialogPane().getStyleClass().add("mesos-alert-pane");
        applyStylesheet(alert.getDialogPane());
        alert.setOnShown(e -> {
            Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.getStyleClass().add("primary-button");
        });
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

    @Override
    public Color showTotemSelection(String title, String gameName, Set<Color> takenColors, Color selectedColor) {
        Dialog<Color> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("MESOS");
        dialog.setHeaderText(title);
        dialog.getDialogPane().getStyleClass().add("totem-dialog-pane");
        applyStylesheet(dialog.getDialogPane());

        ButtonType confirmType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        Label intro = new Label("Choose a totem for " + gameName + ".\nUnavailable colors are disabled and the list refreshes if another player reserves one first.");
        intro.getStyleClass().add("details-label");
        intro.setWrapText(true);
        intro.setTextAlignment(TextAlignment.CENTER);

        ToggleGroup toggleGroup = new ToggleGroup();
        Map<ToggleButton, Color> colorsByButton = new LinkedHashMap<>();
        FlowPane choices = new FlowPane(12, 12);
        choices.getStyleClass().add("totem-dialog-grid");
        choices.setAlignment(Pos.CENTER);

        for (Color color : Color.values()) {
            ToggleButton button = buildTotemChoiceButton(color);
            button.setToggleGroup(toggleGroup);
            button.setDisable(takenColors != null && takenColors.contains(color));
            if (color == selectedColor && !button.isDisable()) {
                button.setSelected(true);
            }
            colorsByButton.put(button, color);
            choices.getChildren().add(button);
        }

        Label footer = new Label("You can choose a different totem in another match.");
        footer.getStyleClass().add("details-label");
        footer.setWrapText(true);
        footer.setTextAlignment(TextAlignment.CENTER);

        VBox content = new VBox(14, intro, choices, footer);
        content.getStyleClass().add("totem-dialog-content");
        content.setAlignment(Pos.CENTER);
        dialog.getDialogPane().setContent(content);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.getStyleClass().add("primary-button");
        confirmButton.disableProperty().bind(toggleGroup.selectedToggleProperty().isNull());

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");

        dialog.setResultConverter(buttonType -> {
            if (buttonType != confirmType) {
                return null;
            }
            ToggleButton selectedButton = (ToggleButton) toggleGroup.getSelectedToggle();
            return colorsByButton.get(selectedButton);
        });

        return dialog.showAndWait().orElse(null);
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
        if (scene == null) {
            return;
        }
        var stylesheet = MesosClientApplication.class.getResource("/it/polimi/ingsw/mesos/gui/mesos-gui.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private void applyStylesheet(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        var stylesheet = MesosClientApplication.class.getResource("/it/polimi/ingsw/mesos/gui/mesos-gui.css");
        if (stylesheet != null && !dialogPane.getStylesheets().contains(stylesheet.toExternalForm())) {
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private ToggleButton buildTotemChoiceButton(Color color) {
        Label nameLabel = new Label(formatTotemName(color));
        nameLabel.getStyleClass().addAll("subsection-title", "totem-option-label");

        VBox graphic = new VBox(8);
        graphic.setAlignment(Pos.CENTER);
        assetResolver.resolveTotem(color).ifPresent(image -> {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(58);
            imageView.setFitHeight(58);
            imageView.setPreserveRatio(true);
            graphic.getChildren().add(imageView);
        });
        graphic.getChildren().add(nameLabel);

        ToggleButton button = new ToggleButton();
        button.getStyleClass().add("totem-option-button");
        button.setGraphic(graphic);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setMinWidth(112);
        button.setPrefWidth(112);
        button.setMinHeight(112);
        return button;
    }

    private String formatTotemName(Color color) {
        return switch (color) {
            case BLUE -> "Blue";
            case ORANGE -> "Orange";
            case BLACK -> "Purple";
            case WHITE -> "White";
            case YELLOW -> "Yellow";
        };
    }
}
