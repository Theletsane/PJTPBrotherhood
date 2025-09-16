package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import java.io.IOException;
import java.util.*;

/**
 * RAPTOR algorithm for computing earliest arrival time between two train stops,
 * including the actual path taken.
 */
public class Raptor {

    private final List<TrainStop> stops;
    private final int maxRounds;

    public Raptor(List<TrainStop> stops, int maxRounds) {
        this.stops = stops;
        this.maxRounds = maxRounds;
    }

    /**
     * Result class for earliest arrival computation.
     */
    public static class Result {
        public final int arrivalTime; // in minutes from midnight
        public final List<TrainStop> path;

        public Result(int arrivalTime, List<TrainStop> path) {
            this.arrivalTime = arrivalTime;
            this.path = path;
        }
    }

    /**
     * Computes the earliest arrival time and path from source to destination.
     *
     * @param source        starting stop
     * @param destination   destination stop
     * @param departureTime time in minutes from midnight
     * @return Result containing earliest arrival time and path
     */
    public Result computeEarliestArrivalWithPath(TrainStop source, TrainStop destination, int departureTime) {
        Map<TrainStop, Integer> arrivalTimes = new HashMap<>();
        Map<TrainStop, TrainStop> previous = new HashMap<>();
        for (TrainStop stop : stops) arrivalTimes.put(stop, Integer.MAX_VALUE);
        arrivalTimes.put(source, departureTime);

        for (int round = 0; round < maxRounds; round++) {
            boolean updated = false;

            for (TrainStop stop : stops) {
                int arrivalAtStop = arrivalTimes.get(stop);

                for (TrainTrips trip : stop.getTrainTripsFromStop()) {
                    int depTime = trip.getDepartureTime().getHour() * 60 + trip.getDepartureTime().getMinute();
                    if (depTime < arrivalAtStop) continue; // can't catch this train

                    TrainStop dest = trip.getDestinationTrainStop();
                    int arrTime = depTime + trip.getDuration();

                    if (arrTime < arrivalTimes.get(dest)) {
                        arrivalTimes.put(dest, arrTime);
                        previous.put(dest, stop);
                        updated = true;
                    }
                }
            }

            if (!updated) break; // no improvement in this round
        }

        // Reconstruct path
        List<TrainStop> path = new ArrayList<>();
        TrainStop step = destination;
        if (previous.containsKey(step) || step.equals(source)) {
            while (step != null) {
                path.add(step);
                step = previous.get(step);
            }
            Collections.reverse(path);
        }

        int arrival = arrivalTimes.get(destination);
        if (arrival == Integer.MAX_VALUE) arrival = -1;

        return new Result(arrival, path);
    }
            
    public static void main(String[] args) throws IOException {

            TrainGraph graph = new TrainGraph();
            graph.loadTrainStops();
            graph.loadRouteNumber();

            TrainStop start = graph.getStopByName("CAPE TOWN");
            TrainStop end = graph.getStopByName("WELLINGTON");
            int departureTimeMinutes = 8 * 60; // 08:00 AM

            Raptor raptor = new Raptor(graph.getTrainStops(), 3); // max 3 transfers
            Raptor.Result result = raptor.computeEarliestArrivalWithPath(start, end, departureTimeMinutes);

            if (result.arrivalTime != -1) {
                System.out.println("Earliest arrival time: " + result.arrivalTime + " minutes from midnight");
                System.out.println("Path:");
                for (TrainStop stop : result.path) {
                    System.out.println(" - " + stop.getName());
                }
            } else {
                System.out.println("No available route found.");
            }
    }
       
}
