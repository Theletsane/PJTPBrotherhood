/**
 * TaxiGraph.java
 *
 * This class represents a graph of taxi stops and trips in the Cape Town taxi network.
 * It loads taxi stop data and trip connections from CSV files, provides methods for
 * finding nearest stops, and computes the shortest-duration path between stops.
 *
 * @author: Boolean Brotherhood
 * @Date: 2025
 */

package com.boolean_brotherhood.public_transportation_journey_planner.Taxi;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.stream.Collectors;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.MyFileLoader;

public class TaxiGraph {

    // Data structures
    private final List<TaxiStop> taxiStops = new ArrayList<>();
    private final List<TaxiTrip> taxiTrips = new ArrayList<>();

    // CSV file paths
    private final String TRIPSFILENAME;
    private final String taxiStopsFILENAME;

    public TaxiGraph() {
        TRIPSFILENAME = "CapeTownTransitData/trips_output.csv";
        taxiStopsFILENAME = "CapeTownTransitData/TaxiStops(09_15_2025).csv";
    }

    /**
     * Loads taxi stops and trips from CSV files into the graph.
     */
    public void loadData() {
        this.LoadtaxiStops();
        this.loadTrips();
    }

    /**
     * Returns all taxi stops in the graph.
     */
    public List<TaxiStop> getTaxiStops() {
        return this.taxiStops;
    }

    /**
     * Returns the total number of taxi stops loaded.
     */
    public int getNumtaxiStops() {
        return this.taxiStops.size();
    }

    /**
     * Returns all taxi stops in the graph.
     */
    public List<TaxiTrip> getTaxiTrips() {
        return this.taxiTrips;
    }

    /**
     * Returns the total number of trips loaded.
     */
    public int getNumTrips() {
        return this.taxiTrips.size();
    }

    /**
     * Finds the nearest taxi stop to given coordinates.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return Nearest TaxiStop object
     */
    public TaxiStop getNearestTaxiStop(double lat, double lon) {
        double dist = Double.MAX_VALUE;
        TaxiStop nearest = null;
        for (TaxiStop taxiStop : this.taxiStops) {
            double temp = taxiStop.getDistanceBetween(lat, lon);
            if (dist > temp) {
                dist = temp;
                nearest = taxiStop;
            }
        }
        return nearest;
    }

    /**
     * Returns a list of the closest taxi stops to the given coordinates, up to a
     * maximum number of stops.
     *
     * @param lat      Latitude of the point
     * @param lon      Longitude of the point
     * @param maxStops Maximum number of nearest stops to return
     * @return List of TaxiStop objects closest to the given coordinates
     */
    public List<TaxiStop> getNearestTaxiStops(double lat, double lon, int maxStops) {
        // Sort stops by distance from (lat, lon)
        return taxiStops.stream()
                .sorted(Comparator.comparingDouble(stop -> stop.getDistanceBetween(lat, lon)))
                .limit(maxStops)
                .collect(Collectors.toList());
    }

    private void LoadtaxiStops() {

        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(taxiStopsFILENAME)) {
            String line;

            // Skip the header
            br.readLine();
            while ((line = br.readLine()) != null) {

                // Split on tab (or use "," if comma-separated)

                String[] tokens = line.split(",");
                if (tokens.length > 2) {

                    String name = tokens[1].trim();
                    String stopCode = tokens[0].trim();
                    double latitude = Double.parseDouble(tokens[2].trim());
                    double longitude = Double.parseDouble(tokens[3].trim());
                    String Address = tokens[4];
                    for (int i = 5; i < tokens.length; i++) {
                        Address += ", " + tokens[i];
                    }
                    System.out.println(Address);
                    TaxiStop stop = new TaxiStop(latitude, longitude, name, stopCode, Address);

                    // Only add if not already in the list
                    boolean exists = taxiStops.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name) &&
                            Double.compare(s.getLatitude(), latitude) == 0 &&
                            Double.compare(s.getLongitude(), longitude) == 0);
                    if (!exists) {
                        this.taxiStops.add(stop);
                    }
                }
            }
            System.out.println("Loaded " + taxiStops.size() + " stops.");

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void loadTrips() {

        Random random = new Random();

        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(TRIPSFILENAME)) {
            String line;
            boolean headerSkipped = false;

            int limit = 0;
            while ((line = br.readLine()) != null && limit < 200) {
                limit++;
                // Skip header
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                // Split by comma or tab
                String[] parts = line.split(",");
                if (parts.length < 2)
                    continue;

                String departureName = parts[0].trim();
                String destinationName = parts[1].trim();

                // Try to find TaxitaxiStops from the taxiStops list
                TaxiStop departure = findStopByName(departureName);
                TaxiStop destination = findStopByName(destinationName);

                if (departure == null || destination == null) {
                    continue; // skip if stop not found
                }

                TaxiTrip trip = new TaxiTrip(departure, destination);

                // Generate a random duration (10–60 min)
                double dist = departure.getDistanceBetween(destination);

                if (dist < 1) {
                    dist = 1;
                }
                trip.setDuration((int) Math.round(dist));

                taxiTrips.add(trip);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private TaxiStop findStopByName(String name) {
        for (TaxiStop stop : this.taxiStops) {
            if (stop.getName().equalsIgnoreCase(name)) {
                return stop;
            }
        }
        return null;
    }

    /**
     * Computes the shortest-duration path between two taxi stops using Dijkstra's
     * algorithm.
     *
     * @param startstop Name of starting stop
     * @param endstop   Name of destination stop
     * @return Result object containing total duration and path
     */
    public Result findShortestDuration(String startstop, String endstop) {
        TaxiStop start = findStopByName(startstop);
        TaxiStop end = findStopByName(endstop);

        if (start == null || end == null) {
            return new Result(-1, Collections.emptyList());
        }

        Map<TaxiStop, Integer> distance = new HashMap<>();
        Map<TaxiStop, TaxiStop> previous = new HashMap<>();
        PriorityQueue<TaxiStop> queue = new PriorityQueue<>(Comparator.comparingInt(distance::get));

        for (TaxiStop stop : taxiStops) {
            distance.put(stop, Integer.MAX_VALUE);
        }
        distance.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            TaxiStop current = queue.poll();

            if (current.equals(end)) {
                break;
            }

            for (TaxiTrip trip : getOutgoingTrips(current)) {
                TaxiStop neighbor = trip.getDestination();

                int newDist = distance.get(current) + trip.getDuration();

                if (newDist < distance.get(neighbor)) {
                    distance.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.remove(neighbor);
                    queue.add(neighbor);

                }
            }
        }

        List<TaxiStop> path = new ArrayList<>();
        TaxiStop step = end;
        if (previous.get(step) != null || step.equals(start)) {
            while (step != null) {
                path.add(step);
                step = previous.get(step);
            }
            Collections.reverse(path);
        }

        int totalDuration = distance.get(end) == Integer.MAX_VALUE ? -1 : distance.get(end);

        return new Result(totalDuration, path);
    }

    /**
     * Returns all outgoing trips from a specific taxi stop.
     *
     * @param stop TaxiStop object
     * @return List of TaxiTrip objects departing from stop
     */
    public List<TaxiTrip> getOutgoingTrips(TaxiStop stop) {
        List<TaxiTrip> outgoing = new ArrayList<>();
        for (TaxiTrip trip : taxiTrips) {
            if (trip.getDeparture().equals(stop)) {
                outgoing.add(trip);
            }
        }
        return outgoing;
    }

    /**
     * Finds a TaxiStop by its exact name.
     *
     * @param stopName Name of the stop
     * @return TaxiStop object or null if not found
     */
    public TaxiStop getStopByName(String stopName) {
        for (TaxiStop stop : taxiStops) {
            if (stop.getName().equalsIgnoreCase(stopName)) {
                return stop;
            }
        }
        return null;
    }

    /**
     * Result container for shortest-duration search.
     */
    public static class Result {
        public final int totalDuration;
        public final List<TaxiStop> path;

        public Result(int totalDuration, List<TaxiStop> path) {
            this.totalDuration = totalDuration;
            this.path = path;
        }

        @Override
        public String toString() {
            if (totalDuration == -1 || path.isEmpty()) {
                return "No path found between the specified stops.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Total duration: ").append(totalDuration).append(" min\n");
            sb.append("Path: ");
            for (int i = 0; i < path.size(); i++) {
                sb.append(path.get(i).getName());
                if (i < path.size() - 1) {
                    sb.append(" -> ");
                }
            }
            return sb.toString();
        }
    }

    /*
     * public static void main(String[] args) {
     * TaxiGraph g = new TaxiGraph();
     * g.LoadtaxiStops();
     * }
     */

}
