package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract Stop class representing a node in the transport network.
 * Stores basic stop information like name, coordinates, code, address, and trips.
 */
public abstract class Stop {

    private String name;
    private String companyName;
    private double latitude;
    private double longitude;
    private String stopCode;
    private String address;
    private List<Trip> trips;  // generic list of trips (BusTrip, TrainTrip, etc.)

    /**
     * Constructor for Stop.
     *
     * @param name      Name of the stop
     * @param latitude  Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @param stopCode  Unique stop code
     * @param address   Optional address
     */
    public Stop(String name, double latitude, double longitude, String stopCode, String address) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.stopCode = stopCode;
        this.address = address;
        this.trips = new ArrayList<>();
    }

    // --- Getters and Setters ---

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getStopCode() { return stopCode; }
    public String getAddress() { return address; }

    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setAddress(String address) { this.address = address; }
    public void setStopCode(String stopCode) { this.stopCode = stopCode; }

    // --- Trips Management ---

    public void addTrip(Trip trip) { trips.add(trip); }
    public List<Trip> getTrips() { return trips; }

    // --- Distance Calculation ---

    /**
     * Calculates distance to another stop in kilometers using Haversine formula.
     */
    public double distanceTo(Stop other) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Calculates distance to a specific coordinate.
     */
    public double distanceTo(double lat, double lon) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat - this.latitude);
        double dLon = Math.toRadians(lon - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(lat)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // --- Object Overrides ---

    @Override
    public String toString() {
        return String.format("%s (Code: %s) - Lat: %.6f, Lon: %.6f", name, stopCode, latitude, longitude);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Stop)) return false;
        Stop other = (Stop) obj;
        return Double.compare(latitude, other.latitude) == 0 &&
               Double.compare(longitude, other.longitude) == 0 &&
               Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, latitude, longitude, stopCode);
    }
}
