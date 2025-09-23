package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

/**
 * Unified multimodal Graph combining Taxi, Train, and Bus networks.
 */
public class Graph {

    private final List<Stop> totalStops;
    private final List<Trip> totalTrips;

    private final TaxiGraph taxiGraph;
    private final TrainGraph trainGraph;
    private final MyCitiBusGraph busGraph;

    // Walking assumptions
    private static final double WALKING_DISTANCE_THRESHOLD_KM = 0.5;   // 500m max
    private static final double WALKING_SPEED_KM_PER_MIN = 0.0833;     // 5km/h

    public Graph() throws IOException {
        this.totalStops = new ArrayList<>();
        this.totalTrips = new ArrayList<>();

        this.taxiGraph = new TaxiGraph();
        this.trainGraph = new TrainGraph();
        this.busGraph   = new MyCitiBusGraph();
    }

    
    /**
     * Loads underlying graphs (call before buildCombinedGraph).
     */
    public void loadGraphData() throws IOException {
        taxiGraph.loadData();               // your taxi loader
        trainGraph.loadTrainStops();
        trainGraph.LoadTrainTrips();
    }

    /**
     * Builds a unified multimodal graph with bus, taxi, train, and walking connections.
     * IMPORTANT: call loadGraphData() before this.
     */
    public void buildCombinedGraph() {
        totalStops.clear();
        totalTrips.clear();

        
        List<TrainStop> trainStops = trainGraph.getTrainStops();
        List<TrainTrips> trainTrips = trainGraph.getTrainTrips();

        List<MyCitiStop> busStops = busGraph.getMyCitiStops();
        List<MyCitiTrip> busTrips = busGraph.getMyCitiTrips();

        // Add all stops (we keep original stop objects, not copies)
        totalStops.addAll(trainStops);
        totalStops.addAll(busStops);

        // Add all trips to unified list
        // Note: TrainTrips, MyCitiTrip and TaxiTrip are subclasses of Trip
        totalTrips.addAll(trainTrips);
        totalTrips.addAll(busTrips);

        // Ensure every trip is attached to its departure stop in the unified graph:
        // (some per-mode loaders may not have added trips into Stop.TRIPS used here)
        for (Trip t : new ArrayList<>(totalTrips)) {
            Stop dep = t.getDepartureStop();
            if (dep != null) {
                // make sure the departure stop we have is one of the stops in totalStops
                // (it should be, because we added original stop objects from each graph)
                dep.addTrip(t);
            }
        }

        // Add walking transfer edges between nearby stops (cross-mode interchange)
        // avoid adding duplicates by tracking created edges (depId->destId)
        Set<String> addedWalking = new HashSet<>();
        for (Stop s1 : totalStops) {
            for (Stop s2 : totalStops) {
                if (s1 == s2) continue;
                double distKm = s1.distanceTo(s2);
                if (distKm <= WALKING_DISTANCE_THRESHOLD_KM) {
                    int durationMinutes = (int) Math.max(1, Math.round(distKm / WALKING_SPEED_KM_PER_MIN));
                    // walking trips are untimed (departureTime == null) -> immediate
                    Trip walking = new Trip(s1, s2, durationMinutes, Trip.DayType.WEEKDAY);
                    walking.setMode("Walking"); // mark type
                    // Create a simple key to prevent duplicates
                    String key = s1.getName() + "->" + s2.getName() + ":WALK";
                    if (!addedWalking.contains(key)) {
                        totalTrips.add(walking);
                        s1.addTrip(walking);
                        addedWalking.add(key);
                    }
                }
            }
        }
    }

    /**
     * Get all stops.
     */
    public List<Stop> getStops() {
        return totalStops;
    }

    /**
     * Get all trips.
     */
    public List<Trip> getTrips() {
        return totalTrips;
    }

    public TrainGraph getTrainGraph(){
        return this.trainGraph;
    }  

    public MyCitiBusGraph getMyCitiBusGraph(){
        return this.busGraph;
    }


    /**
     * Find stop by name (case-insensitive).
     */
    public Stop findStopByName(String name) {
        if (name == null) return null;
        String want = name.trim().toUpperCase();
        for (Stop stop : totalStops) {
            if (stop.getName() != null && stop.getName().trim().toUpperCase().equals(want)) {
                return stop;
            }
        }
        return null;
    }

    /**
     * RAPTOR inner class that works across all modes in this unified graph.
     */
    public class Raptor {

        private final List<Stop> stops;
        private final List<Trip> trips;

        public Raptor(List<Stop> stops, List<Trip> trips) {
            this.stops = stops;
            this.trips = trips;
        }

        /**
         * Run RAPTOR: earliest arrival from source to destination.
         * - Timed trips: must depart at or after arrivalAtP.
         * - Untimed trips (departureTime == null): considered immediate and take 'duration' minutes.
         */
        public List<Trip> compute(Stop source, Stop target, LocalTime departureTime, int maxRounds) {
            // earliest arrival per stop (LocalTime)
            Map<Stop, LocalTime> earliestArrival = new HashMap<>();
            for (Stop stop : stops) earliestArrival.put(stop, LocalTime.MAX);
            earliestArrival.put(source, departureTime);

            Map<Stop, Trip> parentTrip = new HashMap<>();

            Set<Stop> marked = new HashSet<>();
            marked.add(source);

            for (int round = 0; round < maxRounds; round++) {
                Set<Stop> nextMarked = new HashSet<>();

                for (Stop p : marked) {
                    LocalTime arrivalAtP = earliestArrival.get(p);
                    if (arrivalAtP == null || arrivalAtP.equals(LocalTime.MAX)) continue;

                    // Note: iterate over p.getTripsFromStop() which we've ensured are populated
                    for (Trip trip : p.getTrips()) {
                        LocalTime dep = trip.getDepartureTime();
                        LocalTime arrAtQ;

                        if (dep == null) {
                            // untimed trip (walking/transfer/etc.): depart immediately when you arrive at p
                            arrAtQ = arrivalAtP.plusMinutes(trip.getDuration());
                        } else {
                            // scheduled trip: must catch at scheduled departure
                            if (dep.isBefore(arrivalAtP)) continue; // cannot catch
                            arrAtQ = dep.plusMinutes(trip.getDuration());
                        }

                        Stop q = trip.getDestinationStop();
                        if (arrAtQ.isBefore(earliestArrival.get(q))) {
                            earliestArrival.put(q, arrAtQ);
                            parentTrip.put(q, trip);
                            nextMarked.add(q);
                        }
                    }
                }

                if (nextMarked.isEmpty()) break;
                marked = nextMarked;
            }

            // reconstruct journey (list of trips)
            List<Trip> journey = new ArrayList<>();
            Stop cur = target;
            while (parentTrip.containsKey(cur)) {
                Trip t = parentTrip.get(cur);
                journey.add(t);
                cur = t.getDepartureStop();
            }
            Collections.reverse(journey);
            return journey;
        }
    }

    // convenience accessors
    public List<TaxiStop> getTaxiStops() { return taxiGraph.getTaxiStops(); }
    public TaxiGraph getTaxiGraph() { return taxiGraph; }
    public TaxiStop getNearestTaxiStart(double lat, double lon) { return taxiGraph.getNearestTaxiStop(lat, lon); }
    public List<TaxiStop> getNearestTaxiStops(double lat, double lon, int max)  { return taxiGraph.getNearestTaxiStops(lat, lon,max); }
    public TrainStop getNearestTrainStart(double lat, double lon) { return trainGraph.getNearestTrainStop(lat, lon); }


}
