package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCityTrip;

import java.util.ArrayList;
import java.util.List;

/**
 * TrainStop represents a stop for trains.
 * Extends Stop and manages TrainTrips.
 */
public class TrainStop extends Stop {

    private static final String TYPE = "Train";

    /**
     * Constructor for TrainStop
     *
     * @param name      Name of the stop
     * @param latitude  Latitude
     * @param longitude Longitude
     * @param stopCode  Unique stop code
     * @param address   Address of the stop
     */
    public TrainStop(String name, double latitude, double longitude, String stopCode, String address) {
        super(name, latitude, longitude, stopCode, address);
    }

    /**
     * Add a train-specific trip
     *
     * @param trip TrainTrips object
     */
    public void addTrainTrip(TrainTrips trip) {
        super.addTrip(trip);
    }

    /**
     * Get all train trips at this stop
     */

    public List<TrainTrips> getTrainTrips() {
    List<TrainTrips> result = new ArrayList<>();
        for (Trip trip : super.getTrips()) {
            if (trip instanceof TrainTrips) {
                result.add((TrainTrips) trip);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "TrainStop{name='%s', stopCode='%s', Lat=%.6f, Lon=%.6f, trips=%d}",
                getName(),
                getStopCode(),
                getLatitude(),
                getLongitude(),
                getTrips().size()
        );
    }
}
