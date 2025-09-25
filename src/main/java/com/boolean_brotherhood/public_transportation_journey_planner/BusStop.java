package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BusStop represents a bus stop in the transportation network.
 * Extends Stop and stores route codes specific to this bus stop.
 */
public class BusStop extends Stop {

    private List<String> routeCodes;  // List of route codes this stop belongs to
    private String company;           // Optional: bus company name

    /**
     * Constructor for BusStop.
     *
     * @param name      Name of the stop
     * @param latitude  Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @param stopCode  Unique stop code
     * @param address   Address of the stop
     * @param company   Bus company operating at this stop (optional)
     */
    public BusStop(String name, double latitude, double longitude, String stopCode, String address, String company) {
        super(name, latitude, longitude, stopCode, address);
        this.company = company;
        this.routeCodes = new ArrayList<>();
    }
 
    /**
     * Add a route code that passes through this bus stop.
     *
     * @param code Route code
     */
    public void addRouteCode(String code) {
        if (!routeCodes.contains(code)) {
            routeCodes.add(code);
        }
    }

    /**
     * Get all route codes for this bus stop.
     *
     * @return List of route codes
     */
    public List<String> getRouteCodes() {
        return routeCodes;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    @Override
    public String toString() {
        return String.format(
                "BusStop{name='%s', stopCode='%s', company='%s', Lat=%.6f, Lon=%.6f, routes=%s, trips=%d}",
                getName(),
                getStopCode(),
                company,
                getLatitude(),
                getLongitude(),
                routeCodes,
                getTrips().size()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BusStop)) return false;
        BusStop other = (BusStop) obj;
        return super.equals(other) &&
               Objects.equals(routeCodes, other.routeCodes) &&
               Objects.equals(company, other.company);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), routeCodes, company);
    }
}
