package com.boolean_brotherhood.public_transportation_journey_planner.Taxi;

import com.boolean_brotherhood.public_transportation_journey_planner.Stop;

public class TaxiStop extends Stop {
    private String routeNumber;

    public TaxiStop(String name, double latitude, double longitude, String stopcode, String address, String routeNumber) {
        super(name, latitude, longitude, stopcode, address);
        this.routeNumber = routeNumber;
    }

    public String getRouteNumber() { return routeNumber; }

    @Override
    public String toString() {
        return "TaxiStop{" +
                "name='" + getName() + '\'' +
                ", stopcode='" + getStopCode() + '\'' +
                ", address='" + getAddress() + '\'' +
                ", routeNumber='" + routeNumber + '\'' +
                '}';
    }
}
