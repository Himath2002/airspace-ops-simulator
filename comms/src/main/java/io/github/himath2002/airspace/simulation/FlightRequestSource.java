package io.github.himath2002.airspace.simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Produces a stream of randomly selected destination airport identifiers.
 *
 * <p>The pipe deliberately separates request production from request consumption, allowing each
 * airport to generate traffic independently while the controller applies back-pressure through its
 * own blocking queue.</p>
 */
public final class FlightRequestSource {
    private static final Logger LOGGER = Logger.getLogger(FlightRequestSource.class.getName());
    private static final int MIN_WAIT_MILLIS = 1_000;
    private static final int MAX_WAIT_MILLIS = 5_000;

    private final int airportCount;
    private final int sourceAirportId;
    private final PipedOutputStream output = new PipedOutputStream();
    private final PipedInputStream input = new PipedInputStream();

    public FlightRequestSource(int airportCount, int sourceAirportId) {
        if (airportCount < 2) {
            throw new IllegalArgumentException("At least two airports are required.");
        }
        this.airportCount = airportCount;
        this.sourceAirportId = sourceAirportId;

        try {
            output.connect(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to connect the flight request stream.", exception);
        }
    }

    public BufferedReader openReader() {
        return new BufferedReader(new InputStreamReader(input));
    }

    public void generate() throws InterruptedException {
        try (PrintStream writer = new PrintStream(output)) {
            while (!Thread.currentThread().isInterrupted()) {
                int destinationId = ThreadLocalRandom.current().nextInt(airportCount);
                if (destinationId != sourceAirportId) {
                    writer.println(destinationId);
                    int delay = ThreadLocalRandom.current()
                            .nextInt(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS);
                    Thread.sleep(delay);
                }
            }
        } finally {
            LOGGER.info(() -> "Flight request source stopped for airport " + sourceAirportId);
        }
    }
}
