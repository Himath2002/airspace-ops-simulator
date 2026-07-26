package io.github.himath2002.airspace.model;

import io.github.himath2002.airspace.gui.GridAreaIcon;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread-safe airport state shared by request, flight, and servicing workers.
 */
public final class Airport {
    private static final List<String> NAME_PREFIXES = List.of(
            "Northgate", "Harbor", "Summit", "Riverside", "Lakeside");

    private final int id;
    private final String name;
    private final double x;
    private final double y;
    private final Queue<Plane> groundedAircraft =
            new ConcurrentLinkedQueue<>();
    private final BlockingQueue<FlightRequest> pendingRequests =
            new LinkedBlockingQueue<>();
    private final GridAreaIcon icon;

    public Airport(int id, double x, double y, GridAreaIcon icon) {
        this.id = id;
        this.name = NAME_PREFIXES.get(Math.floorMod(id, NAME_PREFIXES.size())) + " International";
        this.x = x;
        this.y = y;
        this.icon = icon;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public GridAreaIcon getIcon() {
        return icon;
    }

    public void addGroundedAircraft(Plane aircraft) {
        groundedAircraft.add(aircraft);
    }

    public Plane pollAvailableAircraft() {
        return groundedAircraft.poll();
    }

    public BlockingQueue<FlightRequest> getPendingRequests() {
        return pendingRequests;
    }
}
