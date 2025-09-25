package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Standalone harness for exercising the RAPTOR implementations that power the
 * unified {@link Graph} and the bus-only {@link BusGraph}. The goal is to
 * provide quick smoke tests that can be run from the command line without any
 * testing framework, giving visibility into typical and edge-case behaviour.
 */
public final class RaptorDiagnostics {

    private RaptorDiagnostics() {
        // utility class
    }

    public static void main(String[] args) {
        System.out.println("=== RAPTOR Diagnostics ===\n");

        try {
            runGraphRaptorDiagnostics();
        } catch (Exception e) {
            System.err.println("Graph RAPTOR diagnostics failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }

        try {
            runBusGraphRaptorDiagnostics();
        } catch (Exception e) {
            System.err.println("BusGraph RAPTOR diagnostics failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static void runGraphRaptorDiagnostics() throws IOException {
        System.out.println("--- Unified Graph RAPTOR ---");

        Graph graph = new Graph();
        graph.loadGraphData();
        graph.buildCombinedGraph();

        System.out.printf("Loaded graph with %d stops and %d trips.%n",
                graph.getStops().size(), graph.getTrips().size());

        Graph.Raptor raptor = graph.new Raptor(graph.getStops(), graph.getTrips());

        Trip seedTrip = graph.getTrips().stream()
                .filter(t -> t.getDepartureTime() != null)
                .findFirst()
                .orElse(null);

        if (seedTrip == null) {
            System.out.println("No timed trips available; skipping main RAPTOR tests for Graph.\n");
            return;
        }

        Stop source = seedTrip.getDepartureStop();
        Stop target = seedTrip.getDestinationStop();
        LocalTime departure = seedTrip.getDepartureTime();

        List<Trip> directItinerary = raptor.compute(source, target, departure, 1);
        printItinerary("Direct trip match", source, target, departure, directItinerary);

        Optional<Trip> connectingLeg = graph.getTrips().stream()
                .filter(t -> t != seedTrip)
                .filter(t -> t.getDepartureTime() != null)
                .filter(t -> t.getDepartureStop().equals(target))
                .filter(t -> !t.getDepartureTime().isBefore(deriveArrival(seedTrip)))
                .min(Comparator.comparing(Trip::getDepartureTime));

        if (connectingLeg.isPresent()) {
            Stop twoLegTarget = connectingLeg.get().getDestinationStop();
            List<Trip> twoLegItinerary = raptor.compute(source, twoLegTarget, departure, 3);
            printItinerary("Two-leg journey", source, twoLegTarget, departure, twoLegItinerary);
        } else {
            System.out.println("No obvious connection after the seed trip; skipping multi-leg Graph test.");
        }

        List<Trip> nullGuard = raptor.compute(null, target, departure, 2);
        System.out.println("Null-source guard returned " + (nullGuard.isEmpty() ? "an empty itinerary (expected)." : "a non-empty itinerary (investigate!)."));
        System.out.println();
    }

    private static void runBusGraphRaptorDiagnostics() {
        System.out.println("--- BusGraph RAPTOR ---");

        BusGraph busGraph = new BusGraph();
        List<Trip> busTrips = busGraph.getTrips();
        if (busTrips.isEmpty()) {
            System.out.println("BusGraph produced no trips; skipping RAPTOR tests.\n");
            return;
        }

        Set<Stop> stopSet = new LinkedHashSet<>();
        for (Trip trip : busTrips) {
            if (trip.getDepartureStop() != null) {
                stopSet.add(trip.getDepartureStop());
            }
            if (trip.getDestinationStop() != null) {
                stopSet.add(trip.getDestinationStop());
            }
        }
        List<Stop> routerStops = new ArrayList<>(stopSet);

        BusGraph.RaptorRouter router = new BusGraph.RaptorRouter(routerStops, busTrips);

        Trip seedTrip = busTrips.stream()
                .filter(t -> t.getDepartureTime() != null)
                .findFirst()
                .orElse(null);

        if (seedTrip == null) {
            System.out.println("No scheduled bus trips found; cannot exercise RAPTOR.\n");
            return;
        }

        Stop source = seedTrip.getDepartureStop();
        Stop target = seedTrip.getDestinationStop();
        LocalTime departure = seedTrip.getDepartureTime();

        BusGraph.RouterResult direct = router.runRaptor(source, target, departure, 1, Trip.DayType.WEEKDAY);
        printRouterResult("Direct bus trip", direct);

        Optional<Trip> connectingLeg = busTrips.stream()
                .filter(t -> t != seedTrip)
                .filter(t -> t.getDepartureTime() != null)
                .filter(t -> t.getDepartureStop().equals(target))
                .filter(t -> !t.getDepartureTime().isBefore(deriveArrival(seedTrip)))
                .min(Comparator.comparing(Trip::getDepartureTime));

        if (connectingLeg.isPresent()) {
            Stop twoLegTarget = connectingLeg.get().getDestinationStop();
            BusGraph.RouterResult twoLeg = router.runRaptor(source, twoLegTarget, departure, 3, Trip.DayType.WEEKDAY);
            printRouterResult("Two-leg bus journey", twoLeg);
        } else {
            System.out.println("No follow-on bus services for the seed trip; skipping multi-leg bus test.");
        }

        BusGraph.RouterResult offDay = router.runRaptor(source, target, departure, 1, Trip.DayType.SUNDAY);
        System.out.println("SUNDAY query returned " + (offDay == null ? "no itinerary (expected if data is weekday-only)." : "a journey (verify service calendars!)."));
        System.out.println();
    }

    private static LocalTime deriveArrival(Trip trip) {
        if (trip == null || trip.getDepartureTime() == null) {
            return null;
        }
        return trip.getDepartureTime().plusMinutes(trip.getDuration());
    }

    private static void printItinerary(String label, Stop source, Stop target, LocalTime departure, List<Trip> itinerary) {
        System.out.println("[" + label + "]");
        if (itinerary == null || itinerary.isEmpty()) {
            System.out.println("  No itinerary returned.");
            return;
        }

        LocalTime current = departure;
        for (int i = 0; i < itinerary.size(); i++) {
            Trip leg = itinerary.get(i);
            LocalTime legDeparture = leg.getDepartureTime();
            if (legDeparture == null) {
                legDeparture = current;
            }
            LocalTime legArrival = legDeparture != null ? legDeparture.plusMinutes(leg.getDuration()) : null;
            current = legArrival != null ? legArrival : current != null ? current.plusMinutes(leg.getDuration()) : null;

            String mode = leg.getMode() != null ? leg.getMode() : leg.getClass().getSimpleName();
            System.out.printf("  %d) %s -> %s | depart %s | arrive %s | %d min | %s%n",
                    i + 1,
                    leg.getDepartureStop() != null ? leg.getDepartureStop().getName() : "<unknown>",
                    leg.getDestinationStop() != null ? leg.getDestinationStop().getName() : "<unknown>",
                    legDeparture != null ? legDeparture : "immediate",
                    legArrival != null ? legArrival : "unknown",
                    leg.getDuration(),
                    mode);
        }

        if (current != null && departure != null) {
            int elapsed = computeElapsedMinutes(departure, current);
            System.out.printf("  Total legs: %d | arrival %s | elapsed %d min%n",
                    itinerary.size(), current, elapsed);
        } else {
            System.out.printf("  Total legs: %d | arrival unknown%n", itinerary.size());
        }
    }

    private static void printRouterResult(String label, BusGraph.RouterResult result) {
        System.out.println("[" + label + "]");
        if (result == null) {
            System.out.println("  No itinerary returned.");
            return;
        }

        System.out.printf("  Departure %s | arrival %s | duration %d min | transfers %d | legs %d%n",
                result.departure,
                result.arrival,
                result.totalMinutes,
                result.transfers,
                result.trips.size());

        Stop source = result.trips.isEmpty() ? null : result.trips.get(0).getDepartureStop();
        Stop target = result.trips.isEmpty() ? null : result.trips.get(result.trips.size() - 1).getDestinationStop();
        printItinerary("  Itinerary details", source, target, result.departure, result.trips);
    }

    private static int computeElapsedMinutes(LocalTime departure, LocalTime arrival) {
        int dep = departure.getHour() * 60 + departure.getMinute();
        int arr = arrival.getHour() * 60 + arrival.getMinute();
        int elapsed = arr - dep;
        if (elapsed < 0) {
            elapsed += 24 * 60;
        }
        return elapsed;
    }
}
