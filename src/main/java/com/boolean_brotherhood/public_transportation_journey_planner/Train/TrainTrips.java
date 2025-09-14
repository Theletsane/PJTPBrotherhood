package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Train-specific Trip that carries some extra metadata.
 * NOTE: Uses Trip.DayType (do not redeclare a DayType enum here).
 */
public class TrainTrips extends Trip {

    private String tripID;
    private String direction;     // e.g., "SALT RIVER → WELLINGTON"
    private String routeCode;     // optional short code
    private String routeNumber;   // e.g., "S9"
    private String tripNumber;    // e.g., "1", "2", ...
    private List<String> majorStations = new ArrayList<>();
    private LocalTime departureTime;

    // Keep references as TrainStop for convenience, while Trip stores them as Stop
    private TrainStop departureStop;
    private TrainStop destinationStop;

    public TrainTrips(TrainStop from, TrainStop to, DayType dayType) {
        super(from, to, dayType);   //
        this.departureStop = from;
        this.destinationStop = to;
    }

    // ---------------------------
    // Getters / Setters
    // ---------------------------
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
    public void setMajorStations(List<String> majorStations) {
        this.majorStations = (majorStations == null) ? new ArrayList<>() : majorStations;
    }

    public TrainStop getDepartureTrainStop() { return departureStop; }
    public TrainStop getDestinationTrainStop() { return destinationStop; }

    public void setDepartureTrainStop(TrainStop s) {
        this.departureStop = s;
        setDepartureStop(s); // keep Trip's field in sync
    }
    public void setDestinationTrainStop(TrainStop s) {
        this.destinationStop = s;
        setDestinationStop(s); // keep Trip's field in sync
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    @Override
    public String toString() {
        String meta = "";
        if (routeNumber != null) meta += "[" + routeNumber + "] ";
        if (tripID != null) meta += "#" + tripID + " ";
        return meta + super.toString();
    }
}
