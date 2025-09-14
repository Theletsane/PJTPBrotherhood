package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.Objects;

public class TaxiStop extends Stop {

    private String stopcode; // Taxi stop code/id

    /**
     * Constructor for stops with explicit company, coordinates, name, and code.
     *
     * @param lat      latitude in decimal degrees
     * @param lon      longitude in decimal degrees
     * @param stopName display name of the stop
     * @param stopCode stop code/id
     * @param routeNumber route number (optional, not stored here)
     */
    public TaxiStop(double lat, double lon, String stopName, String stopCode, String routeNumber) {
        super("Taxi", lat, lon, stopName);
        this.stopcode = stopCode;
    }

    // ---------------------------
    // Getters
    // ---------------------------

    public String getStopcode() {
        return stopcode;
    }

    // ---------------------------
    // Setters
    // ---------------------------

    public void setStopcode(String stopcode) {
        this.stopcode = stopcode;
    }

    // ---------------------------
    // Overrides
    // ---------------------------

    @Override
    public String toString() {
        return "TaxiStop{" +
                "name='" + getName() + '\'' +
                ", type='" + getType() + '\'' +
                ", latitude=" + getLatitude() +
                ", longitude=" + getLongitude() +
                ", stopcode='" + stopcode + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getType(), getLatitude(), getLongitude(), stopcode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TaxiStop)) return false;
        TaxiStop other = (TaxiStop) obj;
        return Double.compare(getLatitude(), other.getLatitude()) == 0
                && Double.compare(getLongitude(), other.getLongitude()) == 0
                && Objects.equals(getName(), other.getName())
                && Objects.equals(getType(), other.getType())
                && Objects.equals(stopcode, other.stopcode);
    }
}
