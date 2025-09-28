package com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus;

import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

import java.time.LocalTime;
import java.util.Objects;

/**
 * MyCiti Bus Trip (company-specific).
 */
public class MyCitiTrip extends Trip {

    private String tripID;
    private String routeName;

    public MyCitiTrip(MyCitiStop from, MyCitiStop to, DayType dayType, LocalTime departureTime) {
        super(from, to, dayType);
        setMode("Bus");
        setDepartureTime(departureTime);
    }

    public String getTripID() { return tripID; }
    public void setTripID(String tripID) { this.tripID = tripID; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String rn) { this.routeName = rn; }

    // Convenience casts
    public MyCitiStop getDepartureMyCitiStop() { return (MyCitiStop) getDepartureStop(); }
    public MyCitiStop getDestinationMyCitiStop() { return (MyCitiStop) getDestinationStop(); }

    

    @Override
    public String toString() {
        return String.format("MyCitiTrip[%s -> %s, route='%s', id=%s, dep=%s]",
                getDepartureStop().getName(),
                getDestinationStop().getName(),
                routeName,
                tripID,
                getDepartureTime());
    }

        // This is what your GATrip.equals() method should look like
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        // Safe cast only after type checking
        MyCitiTrip other = (MyCitiTrip) obj;
        
        // Your comparison logic here
        return Objects.equals(this.getTripID(), other.getTripID()) &&
            Objects.equals(this.getDepartureStop(), other.getDepartureStop()) &&
            Objects.equals(this.getDestinationStop(), other.getDestinationStop());
    }
}
