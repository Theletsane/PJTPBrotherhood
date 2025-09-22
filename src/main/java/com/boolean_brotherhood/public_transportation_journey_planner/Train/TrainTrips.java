package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Train-specific Trip.
 */
public class TrainTrips extends Trip {

    private String tripID;
    private String direction;       // "SALT RIVER → WELLINGTON"
    private String routeCode;       // "Metrorail Central Line"
    private String routeNumber;     // e.g., "S9"
    private String tripNumber;      // e.g., "1", "2"
    private List<String> majorStations = new ArrayList<>();

    public TrainTrips(TrainStop from, TrainStop to, DayType dayType, LocalTime departureTime) {
        super(from, to, dayType);
        setMode("Train");
        setDepartureTime(departureTime);
    }

    // Extra metadata
    public String getTripID() { return tripID; }
    public void setTripID(String tripID) { this.tripID = tripID; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public String getRouteNumber() { return routeNumber; }
    public void setRouteNumber(String routeNumber) { this.routeNumber = routeNumber; }

    public String getTripNumber() { return tripNumber; }
    public void setTripNumber(String tripNumber) { this.tripNumber = tripNumber; }

    public List<String> getMajorStations() { return majorStations; }
    public void addMajorStation(String station) { this.majorStations.add(station); }

    // Convenience casts
    public TrainStop getDepartureTrainStop() { return (TrainStop) getDepartureStop(); }
    public TrainStop getDestinationTrainStop() { return (TrainStop) getDestinationStop(); }

    @Override
    public String toString() {
        return String.format("TrainTrip[%s -> %s, dep=%s, route=%s, id=%s]",
                getDepartureStop().getName(),
                getDestinationStop().getName(),
                getDepartureTime(),
                routeNumber,
                tripID);
    }
}
