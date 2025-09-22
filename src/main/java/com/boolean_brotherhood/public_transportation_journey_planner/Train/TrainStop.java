package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;

import java.util.ArrayList;
import java.util.List;

/**
 * TrainStop represents a stop for trains.
 * Extends Stop and manages TrainTrips.
 */
public class TrainStop extends Stop {

    private static final String TYPE = "Train";
    private int OID;
    private int rank;
    private int ObjectID;


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
        OID      = 0;
        rank     = 0;
        ObjectID = 0;

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


        // OID
    public int getOID() {
        return OID;
    }

    public void setOID(int OID) {
        this.OID = OID;
    }

    // rank
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    // ObjectID
    public int getObjectID() {
        return ObjectID;
    }

    public void setObjectID(int ObjectID) {
        this.ObjectID = ObjectID;
    }

    @Override
    public String toString() {
        return String.format(
                "TrainStop{name='%s', stopCode='%s', latitude=%.6f, longitude=%.6f, OID=%d, rank=%d, ObjectID=%d, trips=%d}",
                getName(),
                getStopCode(),
                getLatitude(),
                getLongitude(),
                OID,
                rank,
                ObjectID,
                getTrips().size()
        );
    }
}
