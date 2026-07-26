package io.github.himath2002.airspace.model;

/**
 * Immutable route request between two airports.
 */
public record FlightRequest(int originId, int destinationId) {
    public FlightRequest {
        if (originId < 0 || destinationId < 0) {
            throw new IllegalArgumentException("Airport identifiers cannot be negative.");
        }
        if (originId == destinationId) {
            throw new IllegalArgumentException("Origin and destination must be different.");
        }
    }
}
