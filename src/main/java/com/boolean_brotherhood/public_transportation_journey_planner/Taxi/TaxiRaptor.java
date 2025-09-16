package com.boolean_brotherhood.public_transportation_journey_planner.Taxi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAPTOR algorithm implementation for TaxiGraph.
 * Finds the earliest arrival from a source stop to a target stop.
 */
public class TaxiRaptor {

    private final TaxiGraph graph;

    public TaxiRaptor(TaxiGraph graph) {
        this.graph = graph;
    }

    /**
     * Finds earliest-arrival path from source to target stop.
     *
     * @param sourceName starting taxi stop
     * @param targetName destination taxi stop
     * @return Result object with total duration and path
     */
    public TaxiGraph.Result findEarliestArrival(String sourceName, String targetName) {
        TaxiStop source = graph.getStopByName(sourceName);
        TaxiStop target = graph.getStopByName(targetName);

        if (source == null || target == null) {
            return new TaxiGraph.Result(-1, Collections.emptyList());
        }

        Map<TaxiStop, Integer> arrivalTime = new HashMap<>();
        Map<TaxiStop, TaxiStop> previousStop = new HashMap<>();
        Set<TaxiStop> stopsToUpdate = new HashSet<>();

        // Initialize: all stops have infinite arrival time except source
        for (TaxiStop stop : graph.getTaxiStops()) {
            arrivalTime.put(stop, Integer.MAX_VALUE);
        }
        arrivalTime.put(source, 0);
        stopsToUpdate.add(source);

        boolean improved;

        // RAPTOR rounds
        do {
            improved = false;
            Set<TaxiStop> nextRoundStops = new HashSet<>();

            for (TaxiStop currentStop : stopsToUpdate) {
                int currentTime = arrivalTime.get(currentStop);

                for (TaxiTrip trip : graph.getOutgoingTrips(currentStop)) {
                    TaxiStop neighbor = trip.getDestination();
                    int newArrivalTime = currentTime + trip.getDuration();

                    if (newArrivalTime < arrivalTime.get(neighbor)) {
                        arrivalTime.put(neighbor, newArrivalTime);
                        previousStop.put(neighbor, currentStop);
                        nextRoundStops.add(neighbor);
                        improved = true;
                    }
                }
            }

            stopsToUpdate = nextRoundStops;

        } while (improved);

        // Reconstruct path
        List<TaxiStop> path = new ArrayList<>();
        TaxiStop step = target;
        if (previousStop.get(step) != null || step.equals(source)) {
            while (step != null) {
                path.add(step);
                step = previousStop.get(step);
            }
            Collections.reverse(path);
        }

        int totalDuration = arrivalTime.get(target) == Integer.MAX_VALUE ? -1 : arrivalTime.get(target);
        return new TaxiGraph.Result(totalDuration, path);
    }



     
}
