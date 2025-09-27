package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

/**
 * Utility entry point that locates the RONDEBOSCH train stop(s) and prints all trips related to them.
 */
public final class RondeboschTripsPrinter {

    private static final String TARGET_STOP_NAME = "RONDEBOSCH";

    private RondeboschTripsPrinter() {
        // no instances
    }

    public static void main(String[] args) {
        TrainGraph graph = new TrainGraph();
        try {
            graph.loadTrainStops();
            graph.LoadTrainTrips();
        } catch (IOException e) {
            System.err.println("Failed to load train data: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        List<TrainStop> matches = graph.getTrainStops().stream()
                .filter(stop -> TARGET_STOP_NAME.equalsIgnoreCase(stop.getName()))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            System.out.println("No train stops named '" + TARGET_STOP_NAME + "' were found.");
            return;
        }

        for (TrainStop stop : matches) {
            System.out.println();
            System.out.println("=== Trips for stop: " + stop.getName() + " (" + stop.getStopCode() + ") ===");
            List<TrainTrips> departures = new ArrayList<>();
            List<TrainTrips> arrivals = new ArrayList<>();

            for (TrainTrips trip : graph.getTrainTrips()) {
                if (trip.getDepartureTrainStop().equals(stop)) {
                    departures.add(trip);
                } else if (trip.getDestinationTrainStop().equals(stop)) {
                    arrivals.add(trip);
                }
            }

            Comparator<TrainTrips> byDepartureTime = Comparator.comparing(
                    TrainTrips::getDepartureTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));

            departures.sort(byDepartureTime);
            arrivals.sort(byDepartureTime);

            if (departures.isEmpty() && arrivals.isEmpty()) {
                System.out.println("No trips reference this stop.");
                continue;
            }

            if (!departures.isEmpty()) {
                System.out.println("-- Departures --");
                departures.forEach(trip -> System.out.println(formatTrip("Dep", trip)));
            }

            if (!arrivals.isEmpty()) {
                System.out.println("-- Arrivals --");
                arrivals.forEach(trip -> System.out.println(formatTrip("Arr", trip)));
            }
        }
    }

    private static String formatTrip(String directionLabel, TrainTrips trip) {
        String route = trip.getRouteNumber() != null ? trip.getRouteNumber() : "N/A";
        String tripId = trip.getTripID() != null ? trip.getTripID() : "N/A";
        String departureTime = trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "Unknown";

        return String.format("%s | %s -> %s | dep %s | duration %d min | route %s | id %s",
                directionLabel,
                trip.getDepartureTrainStop().getName(),
                trip.getDestinationTrainStop().getName(),
                departureTime,
                trip.getDuration(),
                route,
                tripId);
    }
}