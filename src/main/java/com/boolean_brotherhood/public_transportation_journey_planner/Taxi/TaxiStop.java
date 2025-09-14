package com.boolean_brotherhood.public_transportation_journey_planner.Taxi;

import com.boolean_brotherhood.public_transportation_journey_planner.Stop;

import java.util.Objects;

public class TaxiStop extends Stop {

    private String routeNumber; // Taxi stop code/id

    /**
     * Constructor for stops with explicit company, coordinates, name, and code.
     *
     * @param lat      latitude in decimal degrees
     * @param lon      longitude in decimal degrees
     * @param stopName display name of the stop
     * @param stopCode stop code/id
     */
    public TaxiStop(double lat, double lon, String stopName, String stopCode) {
        super("Taxi", lat, lon, stopName,stopCode);
        this.routeNumber =routeNumber ;
    }



    @Override
    public String toString() {

        return "TaxiStop{" +
                "name='" + getName() + "\n" +
                ", type='" + getType() + "\n" +
                ", latitude=" + getLatitude()+"\n" +
                ", longitude=" + getLongitude() + "\n"+
                ", stopcode='" + stopcode + "\n" +
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
