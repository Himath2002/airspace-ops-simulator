package io.github.himath2002.airspace.controller;

import io.github.himath2002.airspace.config.SimulationSettings;
import io.github.himath2002.airspace.gui.GridArea;
import io.github.himath2002.airspace.gui.GridAreaIcon;
import io.github.himath2002.airspace.model.Airport;
import io.github.himath2002.airspace.model.FlightRequest;
import io.github.himath2002.airspace.model.Plane;
import io.github.himath2002.airspace.simulation.AircraftService;
import io.github.himath2002.airspace.simulation.FlightRequestSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

/**
 * Coordinates request generation, aircraft movement, servicing, and JavaFX updates.
 */
public final class SimulationController {
    private static final int SERVICE_WORKER_COUNT = 5;
    private static final long GUI_REFRESH_NANOS = 8_333_333L;
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 5_000L;
    private static final long MOVEMENT_INTERVAL_MILLIS = 100L;

    private final GridArea gridArea;
    private final Label statusLabel;
    private final TextArea eventLog;
    private final List<Airport> airports = new ArrayList<>();
    private final Map<Integer, Airport> airportsById = new HashMap<>();
    private final List<Plane> aircraft = new ArrayList<>();
    private final BlockingQueue<FlightRequest> flightRequests = new LinkedBlockingQueue<>();
    private final BlockingQueue<Plane> serviceQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger airborneCount = new AtomicInteger();
    private final AtomicInteger servicingCount = new AtomicInteger();
    private final AtomicInteger completedFlights = new AtomicInteger();

    private ExecutorService requestWorkers;
    private ExecutorService operationWorkers;
    private AnimationTimer updateTimer;
    private volatile boolean running;

    public SimulationController(GridArea gridArea, Label statusLabel, TextArea eventLog) {
        this.gridArea = gridArea;
        this.statusLabel = statusLabel;
        this.eventLog = eventLog;
        gridArea.setCaptionColour(Color.WHITE);
    }

    public void startSimulation() {
        if (running) {
            return;
        }

        resetState();
        running = true;
        requestWorkers = Executors.newFixedThreadPool(SimulationSettings.airportCount);
        operationWorkers = Executors.newCachedThreadPool();

        initializeEntities();
        for (Airport airport : airports) {
            requestWorkers.submit(() -> handleFlightRequests(airport));
        }
        for (int worker = 0; worker < SERVICE_WORKER_COUNT; worker++) {
            operationWorkers.submit(this::processServicing);
        }

        updateTimer = new AnimationTimer() {
            private long lastUpdate;

            @Override
            public void handle(long now) {
                if (running && now - lastUpdate >= GUI_REFRESH_NANOS) {
                    dispatchQueuedFlights();
                    lastUpdate = now;
                }
            }
        };
        updateTimer.start();
    }

    private void resetState() {
        airports.clear();
        airportsById.clear();
        aircraft.clear();
        flightRequests.clear();
        serviceQueue.clear();
        airborneCount.set(0);
        servicingCount.set(0);
        completedFlights.set(0);
        gridArea.getIcons().clear();
        updateStatus();
    }

    private void initializeEntities() {
        int columns = (int) SimulationSettings.mapWidth;
        int rows = (int) SimulationSettings.mapHeight;
        double cellWidth = SimulationSettings.mapWidth / columns;
        double cellHeight = SimulationSettings.mapHeight / rows;

        List<Integer> availableColumns = new ArrayList<>();
        for (int column = 0; column < columns; column++) {
            availableColumns.add(column);
        }
        Collections.shuffle(availableColumns);
        Random random = new Random();

        for (int airportId = 0; airportId < SimulationSettings.airportCount; airportId++) {
            int column = availableColumns.get(airportId);
            int row = random.nextInt(rows);
            double x = Math.min(
                    (column * cellWidth) + (cellWidth / 2),
                    SimulationSettings.mapWidth - (cellWidth / 2));
            double y = Math.min(
                    (row * cellHeight) + (cellHeight / 2),
                    SimulationSettings.mapHeight - (cellHeight / 2));

            String caption = "Airport " + (airportId + 1);
            GridAreaIcon icon = new GridAreaIcon(
                    x, y, 0.0, 0.8, resource("airport.png"), caption);
            Airport airport = new Airport(airportId, x, y, icon);
            airports.add(airport);
            airportsById.put(airportId, airport);
            gridArea.getIcons().add(icon);
        }

        int aircraftId = 0;
        for (Airport airport : airports) {
            for (int index = 0;
                    index < SimulationSettings.aircraftPerAirport;
                    index++) {
                GridAreaIcon icon = new GridAreaIcon(
                        airport.getX(),
                        airport.getY(),
                        0.0,
                        0.5,
                        resource("plane.png"),
                        "Aircraft " + (aircraftId + 1));
                Plane plane = new Plane(
                        aircraftId,
                        airport.getX(),
                        airport.getY(),
                        airport.getId(),
                        icon);
                aircraft.add(plane);
                airport.addGroundedAircraft(plane);
                gridArea.getIcons().add(icon);
                aircraftId++;
            }
        }

        appendLog("Initialized " + airports.size() + " airports and " + aircraft.size()
                + " aircraft on a " + columns + " × " + rows + " grid.");
        gridArea.requestLayout();
    }

    private void handleFlightRequests(Airport airport) {
        FlightRequestSource source = new FlightRequestSource(
                SimulationSettings.airportCount, airport.getId());
        Thread generator = new Thread(
                () -> generateRequests(source, airport.getId()),
                "flight-source-" + airport.getId());
        generator.setDaemon(true);
        generator.start();

        try (BufferedReader reader = source.openReader()) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                try {
                    int destinationId = Integer.parseInt(line);
                    flightRequests.put(new FlightRequest(airport.getId(), destinationId));
                } catch (NumberFormatException exception) {
                    appendLog("Ignored malformed flight request for airport "
                            + airport.getId() + ": " + line);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException exception) {
            if (running) {
                appendLog("Flight request stream failed for airport "
                        + airport.getId() + ": " + exception.getMessage());
            }
        } finally {
            generator.interrupt();
        }
    }

    private void generateRequests(FlightRequestSource source, int airportId) {
        try {
            source.generate();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (running) {
                appendLog("Flight request source interrupted for airport " + airportId + ".");
            }
        }
    }

    private void processServicing() {
        while (running) {
            boolean countedAsServicing = false;
            try {
                Plane plane = serviceQueue.take();
                servicingCount.incrementAndGet();
                countedAsServicing = true;
                updateStatusLater();

                AircraftService.service(plane.getCurrentAirportId(), plane.getId());
                plane.setState(Plane.State.GROUNDED);
                Airport airport = airportsById.get(plane.getCurrentAirportId());
                if (airport != null) {
                    airport.addGroundedAircraft(plane);
                    FlightRequest pendingRequest = airport.getPendingRequests().poll();
                    if (pendingRequest != null) {
                        flightRequests.put(pendingRequest);
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (IllegalStateException exception) {
                appendLog("Aircraft servicing failed: " + exception.getMessage());
            } finally {
                if (countedAsServicing) {
                    servicingCount.decrementAndGet();
                    updateStatusLater();
                }
            }
        }
    }

    private void dispatchQueuedFlights() {
        while (running) {
            FlightRequest request = flightRequests.poll();
            if (request == null) {
                return;
            }
            operationWorkers.submit(() -> operateFlight(request));
        }
    }

    private void operateFlight(FlightRequest request) {
        Airport origin = airportsById.get(request.originId());
        Airport destination = airportsById.get(request.destinationId());
        if (origin == null || destination == null) {
            return;
        }

        Plane plane = origin.pollAvailableAircraft();
        if (plane == null) {
            origin.getPendingRequests().offer(request);
            return;
        }

        boolean countedAsAirborne = false;
        try {
            plane.setState(Plane.State.FLYING);
            plane.getIcon().setShown(true);
            airborneCount.incrementAndGet();
            countedAsAirborne = true;
            updateStatusLater();
            appendLog("Aircraft " + (plane.getId() + 1) + " departed "
                    + origin.getName() + " for " + destination.getName() + ".");

            long previousUpdate = System.nanoTime();
            while (running) {
                long now = System.nanoTime();
                double elapsedSeconds = (now - previousUpdate) / 1_000_000_000.0;
                previousUpdate = now;

                boolean arrived = plane.updatePosition(
                        destination.getX(),
                        destination.getY(),
                        SimulationSettings.aircraftSpeed,
                        elapsedSeconds);
                plane.getIcon().setPosition(plane.getX(), plane.getY());
                Platform.runLater(gridArea::requestLayout);
                if (arrived) {
                    break;
                }
                Thread.sleep(MOVEMENT_INTERVAL_MILLIS);
            }

            if (!running) {
                return;
            }

            plane.setCurrentAirportId(destination.getId());
            plane.getIcon().setShown(false);
            airborneCount.decrementAndGet();
            countedAsAirborne = false;
            completedFlights.incrementAndGet();
            updateStatusLater();
            appendLog("Aircraft " + (plane.getId() + 1) + " landed at "
                    + destination.getName() + ".");

            plane.setState(Plane.State.SERVICING);
            serviceQueue.put(plane);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            if (countedAsAirborne) {
                airborneCount.decrementAndGet();
                plane.getIcon().setShown(false);
                updateStatusLater();
            }
        }
    }

    private void updateStatusLater() {
        Platform.runLater(this::updateStatus);
    }

    private void updateStatus() {
        statusLabel.setText("Aircraft airborne: " + airborneCount.get()
                + " | Servicing: " + servicingCount.get()
                + " | Completed flights: " + completedFlights.get());
    }

    private void appendLog(String message) {
        Runnable update = () -> eventLog.appendText(message + System.lineSeparator());
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    public void endSimulation() {
        if (!running) {
            return;
        }
        running = false;

        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (requestWorkers != null) {
            requestWorkers.shutdownNow();
        }
        if (operationWorkers != null) {
            operationWorkers.shutdownNow();
        }

        long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MILLIS;
        awaitTermination(requestWorkers, deadline);
        awaitTermination(operationWorkers, deadline);
        gridArea.requestLayout();
    }

    private void awaitTermination(ExecutorService executor, long deadline) {
        if (executor == null) {
            return;
        }
        long remaining = Math.max(0L, deadline - System.currentTimeMillis());
        try {
            if (!executor.awaitTermination(remaining, TimeUnit.MILLISECONDS)) {
                appendLog("Some background tasks exceeded the five-second shutdown window.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            appendLog("Shutdown was interrupted.");
        }
    }

    private static InputStream resource(String name) {
        InputStream stream = SimulationController.class.getResourceAsStream("/" + name);
        if (stream == null) {
            throw new IllegalStateException("Missing application resource: " + name);
        }
        return stream;
    }
}
