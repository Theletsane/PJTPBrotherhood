package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

/**
 * Combined graph for multimodal route planning.
 * <p>
 * Supports taxi, train, and walking connections. Computes outgoing trips for any
 * stop and allows construction of a total list of stops and trips for convenience.
 */
public class Graph {

    private final List<Stop> totalStops = new ArrayList<>();
    private final List<Trip> totalTrips = new ArrayList<>();
    private final TaxiGraph taxiGraph = new TaxiGraph();
    private final TrainGraph trainGraph = new TrainGraph();

    // walking threshold and speed
    private static final double WALKING_DISTANCE_THRESHOLD_KM = 0.5;
    private static final double WALKING_SPEED_KM_PER_MIN = 0.0833; // 5 km/h

    
    /**
     * Build interchange walking edges between taxi and train stops (used to populate totalStops/totalTrips).
     * This is optional but convenient if you want a combined list; we still compute outgoing edges on-the-fly.
     */
    public void makeTotalGraph() {
        List<TaxiStop> taxiStops = taxiGraph.getTaxiStops();
        List<TrainStop> trainStops = trainGraph.getTrainStops();

        totalStops.clear();
        totalTrips.clear();

        totalStops.addAll(taxiStops);
        totalStops.addAll(trainStops);

        // add explicit walking trips between close stops (both directions)
        for (TaxiStop ts : taxiStops) {
            for (TrainStop tr : trainStops) {
                double dist = ts.getDistanceBetween(tr.getLatitude(), tr.getLongitude());
                if (dist <= WALKING_DISTANCE_THRESHOLD_KM) {
                    int dur = (int) Math.round(dist / WALKING_SPEED_KM_PER_MIN);
                    totalTrips.add(new Trip(ts, tr, dur));
                    totalTrips.add(new Trip(tr, ts, dur));
                }
            }
        }

        // add taxi and train trips too (so totalTrips contains everything)
        totalTrips.addAll(taxiGraph.getTaxiTrips());
        totalTrips.addAll(trainGraph.getTrainTrips());
    }
    

    /**
     * Returns a list of all outgoing trips from a given stop (taxi/train/walking).
     *
     * @param stop the stop to find outgoing trips from
     * @return list of outgoing trips
     */
    public List<Trip> getOutgoingTrips(Stop stop) {
        List<Trip> outgoing = new ArrayList<>();

        // taxi trips
        for (TaxiTrip t : taxiGraph.getTaxiTrips()) {
            if (t.getDeparture().equals(stop)) outgoing.add(t);
        }

        // train trips
        for (TrainTrips tr : trainGraph.getTrainTrips()) {
            // Note: TrainTrips exposes departure TrainStop via getDepartureTrainStop()
            if (tr.getDepartureTrainStop() != null && tr.getDepartureTrainStop().equals(stop)) outgoing.add(tr);
        }

        // walking connections to nearby stops (both taxi and train)
        for (TaxiStop ts : taxiGraph.getTaxiStops()) {
            if (!ts.equals(stop)) {
                double d = stop.getDistanceBetween(ts.getLatitude(), ts.getLongitude());
                if (d <= WALKING_DISTANCE_THRESHOLD_KM) {
                    int dur = (int) Math.round(d / WALKING_SPEED_KM_PER_MIN);
                    outgoing.add(new Trip(stop, ts, dur));
                }
            }
        }
        for (TrainStop tr : trainGraph.getTrainStops()) {
            if (!tr.equals(stop)) {
                double d = stop.getDistanceBetween(tr.getLatitude(), tr.getLongitude());
                if (d <= WALKING_DISTANCE_THRESHOLD_KM) {
                    int dur = (int) Math.round(d / WALKING_SPEED_KM_PER_MIN);
                    outgoing.add(new Trip(stop, tr, dur));
                }
            }
        }

        return outgoing;
    }
    

    /**
     * Finds a trip from a given stop to a destination stop.
     *
     * @param from starting stop
     * @param to   destination stop
     * @return the first matching Trip if exists, null otherwise
     */
    private Trip findTripBetween(Stop from, Stop to) {
        for (Trip t : getOutgoingTrips(from)) {
            if (t.getDestinationStop().equals(to)) {
                return t;
            }
        }
        return null;
    }


    /**
     * Finds a stop by name from the totalStops list.
     *
     * @param name name of the stop
     * @return Stop object if found, null otherwise
     */
    private Stop findStopByNameInTotal(String name){
        for (Stop s : totalStops) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }


    /**
     * Loads taxi stops and trips from TaxiGraph.
     */
    public void LoadTaxiData() {
        System.out.println("Loading taxi graph...");
        taxiGraph.loadData();
        System.out.printf("TaxiGraph loaded with: %d stops, %d trips\n", taxiGraph.getNumtaxiStops(), taxiGraph.getNumTrips());
    }

    /**
     * Loads train stops and trips from TrainGraph.
     * @throws IOException 
     */
    public void LoadTrainData() throws IOException {
        System.out.println("Loading train data...");
        trainGraph.loadTrainStops();
        trainGraph.loadRouteNumber();
        System.out.printf("TrainGraph loaded with: %d stops, %d trips\n", trainGraph.getTrainStops().size(), trainGraph.getTrainTrips().size());
    }

    /**
     * Returns the nearest taxi stop to a given location.
     *
     * @param lat latitude
     * @param lon longitude
     * @return nearest TaxiStop
     */
    public TaxiStop getNearestTaxiStart(double lat, double lon) {
        return taxiGraph.getNearestTaxiStop(lat, lon);
    }

    /**
     * Returns a list of nearest taxi stops up to a maximum number.
     *
     * @param lat latitude
     * @param lon longitude
     * @param max maximum number of stops
     * @return list of nearest TaxiStops
     */
    public List<TaxiStop> getNearestTaxiStops(double lat, double lon, int max)  {
        return taxiGraph.getNearestTaxiStops(lat, lon,max);
    }

    /**
     * Returns the nearest train stop to a given location.
     *
     * @param lat latitude
     * @param lon longitude
     * @return nearest TrainStop
     */
    public TrainStop getNearestTrainStart(double lat, double lon) {
        return trainGraph.getNearestTrainStop(lat, lon);
    }

    /**
     * Loads both train and taxi graphs.
     * @throws IOException 
     */
    public void loadGraph() throws IOException{
        this.LoadTrainData();
        this.LoadTaxiData();
    }


    public TaxiGraph getTaxiGraph() {
        return this.taxiGraph;
    }


    

}
