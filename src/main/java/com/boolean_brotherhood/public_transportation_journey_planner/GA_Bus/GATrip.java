package com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus;

import java.time.LocalTime;

import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

public class GATrip extends Trip {
    private String tripID;
    private String routeName;

    public GATrip(GAStop from, GAStop to, DayType dayType, LocalTime departureTime) {
        super(from, to, dayType);
        setMode("Bus");
        setDepartureTime(departureTime);
    }

    public String getTripID() { return tripID; }
    public void setTripID(String tripID) { this.tripID = tripID; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String rn) { this.routeName = rn; }

    // Convenience casts
    public GAStop getDepartureMyCitiStop() { return (GAStop) getDepartureStop(); }
    public GAStop getDestinationMyCitiStop() { return (GAStop) getDestinationStop(); }

    

    @Override
    public String toString() {
        return String.format("MyCitiTrip[%s -> %s, route='%s', id=%s, dep=%s]",
                getDepartureStop().getName(),
                getDestinationStop().getName(),
                routeName,
                tripID,
                getDepartureTime());
    }

}
