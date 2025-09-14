package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import java.util.List;
import java.util.ArrayList;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

public class TrainStop extends Stop {

    private List<TrainTrips> trainTRIPS = new ArrayList<>();
    
    public TrainStop(String type, double lat, double lon,String name, String stopCode) {
        super("Train", lat, lon, name, stopCode);
    }


   
    public void addTrainTrip(TrainTrips trip){
        super.addTrip(trip); // store it in Stop's TRIPS list
    }

    public List<TrainTrips> getTrainTripsFromStop() {
        return super.getTripsFromStop().stream().map(t -> (TrainTrips) t).toList();
    }

    /*
     *This is a toString method and returns a stringified version of the object.
     * e.g "Umlazi stop: Train - 252.5, 582.55"
     */
    @Override
    public String toString() {
        return String.format(
                "%s stop code: %s \nvehicle type: (%s) - Lat: %.6f, Lon: %.6f",
                this.getName(),
                this.stopcode,
                "Train",
                this.getLatitude(),
                this.getLongitude()
        );
    }

}




