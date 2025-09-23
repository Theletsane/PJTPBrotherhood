package com.boolean_brotherhood.public_transportation_journey_planner;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

import java.time.LocalTime;
import java.util.*;

/**
 * UnifiedGraph.java
 *
 * Central graph that aggregates TrainGraph, TaxiGraph, and MyCitiBusGraph.
 * Provides unified access to all stops, trips, and journey calculations.
 *
 * Author: Boolean Brotherhood
 * Date: 2025
 */
public class UnifiedGraph {

    private final TrainGraph trainGraph;
    private final MyCitiBusGraph busGraph;
    private final TaxiGraph taxiGraph;

    public UnifiedGraph() {
        this.trainGraph = new TrainGraph();
        this.busGraph = new MyCitiBusGraph();
        this.taxiGraph = new TaxiGraph();
    }

    /**
     * Loads all networks
     */
    public void loadAllNetworks() {
        try {
            System.out.println("Loading Train network...");
            trainGraph.loadTrainStops();
            trainGraph.LoadTrainTrips();

            System.out.println("Loading Bus network...");
            busGraph.loadBusStops();
            busGraph.loadTrips();

            System.out.println("Loading Taxi network...");
            taxiGraph.loadData();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =====================================================
     * Unified Stop Access
     * ===================================================== */
    public List<Object> getAllStops() {
        List<Object> stops = new ArrayList<>();
        stops.addAll(trainGraph.getTrainStops());
        stops.addAll(busGraph.getBusStops());
        stops.addAll(taxiGraph.getTaxiStops());
        return stops;
    }

    public Object getStopByName(String name) {
        Object stop = trainGraph.getStopByName(name);
        if (stop != null) return stop;

        stop = busGraph.getStopByName(name);
        if (stop != null) return stop;

        stop = taxiGraph.getStopByName(name);
        return stop; // might be null
    }

    /* =====================================================
     * Unified Trip Access
     * ===================================================== */
    public List<Object> getAllTrips() {
        List<Object> trips = new ArrayList<>();
        trips.addAll(trainGraph.getTrainTrips());
        trips.addAll(busGraph.getBusTrips());
        trips.addAll(taxiGraph.getTaxiTrips());
        return trips;
    }

    /**
     * Get nearest stop across all networks
     */
    public Object getNearestStop(double lat, double lon) {
        Object nearest = trainGraph.getNearestTrainStop(lat, lon);
        double minDist = distanceToStop(lat, lon, nearest);

        Object candidate = busGraph.getNearestBusStop(lat, lon);
        double dist = distanceToStop(lat, lon, candidate);
        if (dist < minDist) {
            nearest = candidate;
            minDist = dist;
        }

        candidate = taxiGraph.getNearestTaxiStop(lat, lon);
        dist = distanceToStop(lat, lon, candidate);
        if (dist < minDist) {
            nearest = candidate;
        }

        return nearest;
    }

    private double distanceToStop(double lat, double lon, Object stop) {
        if (stop instanceof TrainGraph.TrainStop ts) {
            return ts.distanceTo(lat, lon);
        } else if (stop instanceof MyCitiBusGraph.BusStop bs) {
            return bs.distanceTo(lat, lon);
        } else if (stop instanceof TaxiGraph.TaxiStop tx) {
            return tx.distanceTo(lat, lon);
        }
        return Double.MAX_VALUE;
    }

    /* =====================================================
     * Metrics & Stats
     * ===================================================== */
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("Train", trainGraph.getMetrics());
        metrics.put("Bus", busGraph.getMetrics());
        metrics.put("Taxi", taxiGraph.getMetrics());
        return metrics;
    }

    /* =====================================================
     * Unified Journey Calculations
     * ===================================================== */
    public Object findEarliestArrival(String start, String end, LocalTime departure, Trip.DayType dayType) {
        // Trains
        TrainGraph.TrainJourney trainJourney = trainGraph.findEarliestArrival(start, end, departure, dayType);
        if (trainJourney != null) return trainJourney;

        // Buses
        MyCitiBusGraph.BusJourney busJourney = busGraph.findEarliestArrival(start, end, departure, dayType);
        if (busJourney != null) return busJourney;

        // Taxi (shortest duration)
        TaxiGraph.Result taxiJourney = taxiGraph.findShortestDuration(start, end);
        return taxiJourney;
    }

    /* =====================================================
     * Extended Functionality: combine journeys
     * ===================================================== */
    public List<Object> findMultiModalJourney(String start, String end, LocalTime departure) {
        // TODO: Implement a multimodal RAPTOR / Dijkstra combination
        return new ArrayList<>();
    }

    

}
