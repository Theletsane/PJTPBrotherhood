package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalTime;
import java.util.List;

public class testBusGraphs {

    public static void main(String[] args) {

        // Build combined bus graph
        BusGraph busGraph = new BusGraph();
        List<Stop> allBusStops = busGraph.getStops();
        List<Trip> allBusTrips = busGraph.getTrips();

        // Create RAPTOR and CSA routers
        BusGraph.RaptorRouter raptor = new BusGraph.RaptorRouter(allBusStops, allBusTrips);
        BusGraph.ConnectionScanRouter csa = new BusGraph.ConnectionScanRouter(allBusStops, allBusTrips);

        // Example query
        Stop source = allBusStops.stream()
                .filter(s -> s.getName().equals("cape town"))
                .findFirst()
                .orElse(null);

        Stop target = allBusStops.stream()
                .filter(s -> s.getName().equals("Sea Point"))
                .findFirst()
                .orElse(null);

        LocalTime departure = LocalTime.of(8, 0);
        Trip.DayType day = Trip.DayType.WEEKDAY;

        // Run RAPTOR
        BusGraph.RouterResult rres = raptor.runRaptor(source, target, departure, 4, day);
        if (rres != null) {
            System.out.println("RAPTOR arrival: " + rres.arrival + ", duration: " + rres.totalMinutes + " min");
        } else {
            System.out.println("RAPTOR: No route found.");
        }

        // Run CSA
        BusGraph.RouterResult cres = csa.runConnectionScan(source, target, departure, day);
        if (cres != null) {
            System.out.println("CSA arrival: " + cres.arrival + ", duration: " + cres.totalMinutes + " min");
        } else {
            System.out.println("CSA: No route found.");
        }
    }
}
