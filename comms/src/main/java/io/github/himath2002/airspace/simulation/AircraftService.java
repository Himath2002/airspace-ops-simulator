package io.github.himath2002.airspace.simulation;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Simulates the turnaround time required before an aircraft can return to service.
 */
public final class AircraftService {
    private static final Logger LOGGER = Logger.getLogger(AircraftService.class.getName());
    private static final int MIN_SERVICE_MILLIS = 1_000;
    private static final int MAX_SERVICE_MILLIS = 5_000;

    private AircraftService() {
        // Utility class.
    }

    public static void service(int airportId, int aircraftId) throws InterruptedException {
        int duration = ThreadLocalRandom.current()
                .nextInt(MIN_SERVICE_MILLIS, MAX_SERVICE_MILLIS);

        LOGGER.info(() -> "Servicing aircraft " + aircraftId + " at airport " + airportId);
        try {
            Thread.sleep(duration);
            LOGGER.info(() -> "Aircraft " + aircraftId + " returned to service after "
                    + duration + " ms");
        } catch (InterruptedException exception) {
            LOGGER.warning(() -> "Service interrupted for aircraft " + aircraftId
                    + " at airport " + airportId);
            throw exception;
        }
    }
}
