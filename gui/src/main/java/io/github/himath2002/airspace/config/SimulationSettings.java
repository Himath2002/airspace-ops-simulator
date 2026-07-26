package io.github.himath2002.airspace.config;

/**
 * Runtime parameters selected from the simulator toolbar.
 *
 * <p>Values are updated before a simulation starts and remain stable while its workers run.</p>
 */
public final class SimulationSettings {
    public static double mapWidth = 12.0;
    public static double mapHeight = 12.0;
    public static int airportCount = 10;
    public static int aircraftPerAirport = 10;
    public static double aircraftSpeed = 1.2;

    private SimulationSettings() {
        // Static configuration holder.
    }
}
