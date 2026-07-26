package io.github.himath2002.airspace.model;

import io.github.himath2002.airspace.gui.GridAreaIcon;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable aircraft state with synchronized movement and atomic lifecycle transitions.
 */
public final class Plane {
    private static final double ARRIVAL_THRESHOLD = 0.01;

    public enum State {
        GROUNDED,
        FLYING,
        SERVICING
    }

    private final int id;
    private final AtomicReference<State> state = new AtomicReference<>(State.GROUNDED);
    private final GridAreaIcon icon;
    private volatile double x;
    private volatile double y;
    private volatile int currentAirportId;

    public Plane(int id, double x, double y, int currentAirportId, GridAreaIcon icon) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.currentAirportId = currentAirportId;
        this.icon = icon;
        icon.setShown(false);
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public State getState() {
        return state.get();
    }

    public void setState(State newState) {
        state.set(newState);
    }

    public int getCurrentAirportId() {
        return currentAirportId;
    }

    public void setCurrentAirportId(int currentAirportId) {
        this.currentAirportId = currentAirportId;
    }

    public GridAreaIcon getIcon() {
        return icon;
    }

    /**
     * Moves the aircraft toward a destination and reports whether it has arrived.
     */
    public boolean updatePosition(
            double destinationX,
            double destinationY,
            double speed,
            double elapsedSeconds) {
        synchronized (this) {
            double deltaX = destinationX - x;
            double deltaY = destinationY - y;
            double distance = Math.hypot(deltaX, deltaY);

            if (distance < ARRIVAL_THRESHOLD) {
                x = destinationX;
                y = destinationY;
                return true;
            }

            double movement = speed * elapsedSeconds;
            double ratio = Math.min(1.0, movement / distance);
            x += deltaX * ratio;
            y += deltaY * ratio;

            double rotation = Math.toDegrees(Math.atan2(deltaY, deltaX)) + 90.0;
            icon.setRotation((rotation + 360.0) % 360.0);

            return Math.abs(x - destinationX) < ARRIVAL_THRESHOLD
                    && Math.abs(y - destinationY) < ARRIVAL_THRESHOLD;
        }
    }
}
