package io.github.himath2002.airspace.app;

import io.github.himath2002.airspace.config.SimulationSettings;
import io.github.himath2002.airspace.controller.SimulationController;
import io.github.himath2002.airspace.gui.GridArea;
import io.github.himath2002.airspace.ui.DialogManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX entry point and lifecycle coordinator for the simulator.
 */
public final class AirspaceOpsApplication extends Application {
    private SimulationController controller;
    private GridArea gridArea;
    private BorderPane contentPane;
    private TextArea eventLog;
    private Label statusLabel;
    private Button startButton;
    private Button stopButton;
    private boolean simulationRunning;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        eventLog = new TextArea();
        eventLog.setEditable(false);
        eventLog.setPromptText("Operational events will appear here.");

        statusLabel = new Label(initialStatus());
        gridArea = createGrid();
        controller = new SimulationController(gridArea, statusLabel, eventLog);

        startButton = new Button("Start simulation");
        stopButton = new Button("Stop");
        stopButton.setDisable(true);

        TextField widthField = fieldFor(SimulationSettings.mapWidth);
        TextField heightField = fieldFor(SimulationSettings.mapHeight);
        TextField airportField = fieldFor(SimulationSettings.airportCount);
        TextField aircraftField = fieldFor(SimulationSettings.aircraftPerAirport);
        TextField speedField = fieldFor(SimulationSettings.aircraftSpeed);

        HBox configurationBar = new HBox(
                10,
                new Label("Width"),
                widthField,
                new Label("Height"),
                heightField,
                new Label("Airports"),
                airportField,
                new Label("Aircraft / airport"),
                aircraftField,
                new Label("Speed"),
                speedField);

        startButton.setOnAction(event -> startSimulation(
                widthField, heightField, airportField, aircraftField, speedField));
        stopButton.setOnAction(event -> stopSimulation());
        stage.setOnCloseRequest(event -> stopSimulation());

        ToolBar toolbar = new ToolBar(
                startButton, stopButton, new Separator(), statusLabel);
        contentPane = new BorderPane();
        contentPane.setTop(new VBox(toolbar, configurationBar));
        contentPane.setCenter(createSplitPane());

        stage.setTitle("Airspace Ops Simulator");
        stage.setScene(new Scene(contentPane, 1_200, 900));
        stage.show();
    }

    private void startSimulation(
            TextField widthField,
            TextField heightField,
            TextField airportField,
            TextField aircraftField,
            TextField speedField) {
        if (simulationRunning) {
            DialogManager.showWarning(
                    "Simulation already running",
                    "Stop the current simulation before starting another.");
            return;
        }

        try {
            double width = Math.max(2.0, Double.parseDouble(widthField.getText()));
            double height = Math.max(2.0, Double.parseDouble(heightField.getText()));
            int requestedAirports = Integer.parseInt(airportField.getText());
            int aircraftPerAirport = Integer.parseInt(aircraftField.getText());
            double speed = Double.parseDouble(speedField.getText());

            if (aircraftPerAirport < 1 || speed <= 0) {
                throw new IllegalArgumentException(
                        "Aircraft per airport must be at least 1 and speed must be positive.");
            }

            int maxAirports = calculateMaxAirports(width);
            int airportCount = Math.clamp(requestedAirports, 2, maxAirports);
            if (airportCount != requestedAirports) {
                DialogManager.showWarning(
                        "Airport count adjusted",
                        "The requested " + requestedAirports + " airports were adjusted to "
                                + airportCount + " for a " + (int) width + " × "
                                + (int) height + " grid.");
            }

            SimulationSettings.mapWidth = width;
            SimulationSettings.mapHeight = height;
            SimulationSettings.airportCount = airportCount;
            SimulationSettings.aircraftPerAirport = aircraftPerAirport;
            SimulationSettings.aircraftSpeed = speed;

            widthField.setText(String.valueOf(width));
            heightField.setText(String.valueOf(height));
            airportField.setText(String.valueOf(airportCount));
            eventLog.clear();

            gridArea = createGrid();
            controller = new SimulationController(gridArea, statusLabel, eventLog);
            contentPane.setCenter(createSplitPane());

            controller.startSimulation();
            simulationRunning = true;
            startButton.setDisable(true);
            stopButton.setDisable(false);
        } catch (NumberFormatException exception) {
            DialogManager.showError(
                    "Invalid input", "Enter numeric values for every simulation setting.");
        } catch (IllegalArgumentException exception) {
            DialogManager.showError("Invalid settings", exception.getMessage());
        }
    }

    private void stopSimulation() {
        if (controller != null) {
            controller.endSimulation();
        }
        simulationRunning = false;
        if (startButton != null) {
            startButton.setDisable(false);
        }
        if (stopButton != null) {
            stopButton.setDisable(true);
        }
    }

    private GridArea createGrid() {
        GridArea area = new GridArea(
                SimulationSettings.mapWidth, SimulationSettings.mapHeight);
        area.setGridLines(true);
        area.setStyle(
                "-fx-background-image: url('background.png');"
                        + "-fx-background-size: cover;"
                        + "-fx-background-repeat: no-repeat;"
                        + "-fx-background-position: center;");
        return area;
    }

    private SplitPane createSplitPane() {
        SplitPane pane = new SplitPane(gridArea, eventLog);
        pane.setDividerPositions(0.75);
        return pane;
    }

    private static TextField fieldFor(double value) {
        return new TextField(String.valueOf(value));
    }

    private static int calculateMaxAirports(double width) {
        return Math.max(2, (int) width);
    }

    private static String initialStatus() {
        return "Aircraft airborne: 0 | Servicing: 0 | Completed flights: 0";
    }
}
