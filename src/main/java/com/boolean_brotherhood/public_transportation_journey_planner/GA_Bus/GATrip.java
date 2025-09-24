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
    public GAStop getDepartureGAStop() { return (GAStop) getDepartureStop(); }
    public GAStop getDestinationGAStop() { return (GAStop) getDestinationStop(); }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Trip)) return false;
        GATrip other = (GATrip) o;
        return (
            (this.routeName.equals(other.getRouteName())) &&
            (this.getDayType() == other.getDayType()) && 
            (this.getDepartureTime().equals(other.getDepartureTime())) &&
            (this.getDepartureGAStop().equals(other.getDepartureGAStop())) &&                 
            (this.getDestinationGAStop().equals(other.getDestinationGAStop()))
                );
    }

    @Override
    public String toString() {
        return String.format("Golden Arrows Trip [ %s -> %s, route='%s', id=%s, dep=%s]",
                getDepartureStop().getName(),
                getDestinationStop().getName(),
                routeName,
                tripID,
                getDepartureTime());
    }

}
