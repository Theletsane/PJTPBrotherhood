/**
 * TrainGraph.java
 * 
 * This class represents a graph of train stations and trips for the Cape Town MetroRail network.
 * It loads train stops, routes, and trip schedules from CSV files, and provides methods for
 * finding nearest train stops and computing the earliest arrival path between stations.
 * 
 * Author: Boolean Brotherhood
 * Date: 2025
 */

package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.DataFilesRegistry;
import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.MyFileLoader;


import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

public class TrainGraph {


    // Public Transportation Journey
    private final String Complete_Metrorail_Stations;
    private final String summaryTrips ;

    // Data structures
    private final List<TrainStop> trainStops = new ArrayList<>();
    public List<String> routeNumbers = new ArrayList<>();
    private final List<TrainTrips> trainTrips = new ArrayList<>();

    private long stopLoadTimeMs = 0;
    private long tripLoadTimeMs = 0;
    private long lastRaptorLatencyMs = -1; // -1 means no query yet


    // Call this after each RAPTOR query
    public void setLastRaptorLatency(long ms) { this.lastRaptorLatencyMs = ms; }

    

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    
    public TrainGraph(){
        String usedBy = this.getClass().getSimpleName();
        Complete_Metrorail_Stations = DataFilesRegistry.getFile("TRAIN_STOPS",usedBy) ;
        summaryTrips = DataFilesRegistry.getFile("TRAIN_TRIPS",usedBy) ;
    }

        // call these in main/controller after loading
    public void setStopLoadTime(long ms) { this.stopLoadTimeMs = ms; }
    public void setTripLoadTime(long ms) { this.tripLoadTimeMs = ms; }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Count
        int stopCount = getTrainStops().size();
        int tripCount = getTrainTrips().size();
        int routeCount = routeNumbers.size();

        // Estimate memory usage of stops
        long stopMemory = 0;
        for (TrainStop stop : getTrainStops()) {
            stopMemory += 64; // object overhead
            stopMemory += stop.getName().length() * 2;
            stopMemory += stop.getStopCode().length() * 2;
            stopMemory += stop.getAddress().length() * 2;
        }

        // Estimate memory usage of trips
        long tripMemory = 0;
        for (TrainTrips trip : getTrainTrips()) {
            tripMemory += 128; // object overhead
            if (trip.getTripID() != null) tripMemory += trip.getTripID().length() * 2;
            if (trip.getRouteNumber() != null) tripMemory += trip.getRouteNumber().length() * 2;
        }

        // JVM memory
        long usedJvmMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

        // Put results
        metrics.put("stopsLoaded", stopCount);
        metrics.put("tripsLoaded", tripCount);
        metrics.put("routesLoaded", routeCount);
        metrics.put("stopLoadTimeMs", stopLoadTimeMs);
        metrics.put("tripLoadTimeMs", tripLoadTimeMs);
        metrics.put("stopMemoryBytes", stopMemory);
        metrics.put("tripMemoryBytes", tripMemory);
        metrics.put("totalEstimatedDataBytes", stopMemory + tripMemory);
        metrics.put("jvmUsedBytes", usedJvmMem);
        metrics.put("lastRaptorLatencyMs", lastRaptorLatencyMs);

        return metrics;
    }


    /**
     * Finds a train stop by exact name.
     *
     * @param stopName Name of the stop
     * @return TrainStop object or null if not found
     */
    public TrainStop getStopByName(String stopName) {
        for (TrainStop stop : trainStops) {
            if (stop.getName().equalsIgnoreCase(stopName)) {
                return stop;
            }
        }
        return null;
    }


    /**
     * Retrieves all outgoing trips starting from a specific stop.
     *
     * @param stop TrainStop object
     * @return List of TrainTrips starting from the stop
     */
    public List<TrainTrips> getOutgoingTrips(TrainStop stop) {
        List<TrainTrips> outgoing = new ArrayList<>();
        for (TrainTrips trip : trainTrips) {
            if (trip.getDepartureTrainStop().equals(stop)) {
                outgoing.add(trip);
            }
        }
        return outgoing;
    }


    /**
     * Gets the nearest train stop to a given set of coordinates.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return Nearest TrainStop object
     */
    public TrainStop getNearestTrainStop(double lat, double lon) {
        double minDist = Double.MAX_VALUE;
        TrainStop nearest = null;
        for (TrainStop stop : trainStops) {
            double d = stop.distanceTo(lat, lon);
            if (d < minDist) {
                minDist = d;
                nearest = stop;
            }
        }
        return nearest;
    }


    /**
     * Returns all train trips loaded in the graph.
     *
     * @return List of TrainTrips
     */
    public List<TrainTrips> getTrainTrips() {
        return this.trainTrips;
    }


    /**
     * Returns all train stops loaded in the graph.
     *
     * @return List of TrainStop
     */
    public List<TrainStop> getTrainStops() {
        return this.trainStops;
    }


    /* ===============================================================================================================
     * Loading Data from CSV FILE. 
     * @results: loading TrainStops
     * @results: loading TrainTrips
    ==================================================================================================================*/
    /**
     * Loads train stop data from the CSV file into the trainStops list.
     */
    public void loadTrainStops() throws IOException {
        String line;

        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(Complete_Metrorail_Stations)) {
            // skip header line
            String headers = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length >= 5) {

                    String name = parts[5].toUpperCase(); // ATHLONE
                    String code;
                    int OID = -1;
                    int objectID = -1;
                    int rank = -1;
                    if (!parts[3].isEmpty()) {
                        OID = Integer.parseInt(parts[3].trim());
                        objectID = Integer.parseInt(parts[13].trim());
                        rank = Integer.parseInt(parts[4].trim());
                    }
                    if(!parts[11].isEmpty()){ code = parts[11].toUpperCase();}else{ code =parts[5].toUpperCase();} // ATL
                    double lat;
                    double lon;
                    if(!parts[0].isEmpty()){
                         lat = Double.parseDouble(parts[6]); // -33.96089
                         lon = Double.parseDouble(parts[0]); // 18.501622
                    } else {
                         lat = Double.parseDouble(parts[1]); // -33.96089
                         lon = Double.parseDouble(parts[20]); // 18.501622
                    }
                    String address =  parts[22];
                    for (int i = 5; i < parts.length; i++) {
                        address += ", " + parts[i];
                    }

                    TrainStop stop = new TrainStop(name, lat, lon, code ,address);
                    stop.setOID(OID);
                    stop.setRank(rank);
                    stop.setObjectID(objectID);
                    trainStops.add(stop);
                    

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Loads trips from a CSV file for a specific route and populates trainTrips
     * list.
     *
     * @param filePath    Path to trip CSV
     * @param routeNumber Route identifier
     * @throws IOException if file reading fails
     */
    private void loadTripsFromCSV(String filePath, String routeNumber) throws IOException {
        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(filePath)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file empty: " + filePath);
            }

            String[] stationNames = headerLine.split(",", -1);
            String line;
            int autoTripCounter = 1;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length <= 4) {
                    continue;
                }

                String rawTripId = columns[0].trim();
                String rawDayType = columns[1].trim();
                String rawDirection = columns[2].trim();
                String rawRouteCode = columns[3].trim();

                Trip.DayType dayType;
                try {
                    dayType = Trip.DayType.parseDayType(rawDayType);
                } catch (IllegalArgumentException ex) {
                    dayType = Trip.DayType.WEEKDAY;
                }

                int limit = Math.min(columns.length, stationNames.length);
                List<TrainStop> orderedStops = new ArrayList<>();
                List<LocalTime> orderedTimes = new ArrayList<>();

                boolean inbound = rawDirection.equalsIgnoreCase("INBOUND");
                if (inbound) {
                    for (int idx = limit - 1; idx >= 4; idx--) {
                        collectStopTime(columns, stationNames, orderedStops, orderedTimes, idx);
                    }
                } else {
                    for (int idx = 4; idx < limit; idx++) {
                        collectStopTime(columns, stationNames, orderedStops, orderedTimes, idx);
                    }
                }

                if (orderedStops.size() < 2) {
                    continue;
                }

                String baseTripId = !rawTripId.isEmpty() ? routeNumber + "-" + rawTripId : routeNumber + "-T" + (autoTripCounter++);

                for (int idx = 0; idx < orderedStops.size() - 1; idx++) {
                    TrainStop departureStop = orderedStops.get(idx);
                    TrainStop arrivalStop = orderedStops.get(idx + 1);
                    LocalTime departureTime = orderedTimes.get(idx);
                    LocalTime arrivalTime = orderedTimes.get(idx + 1);

                    long minutes = Duration.between(departureTime, arrivalTime).toMinutes();
                    if (minutes < 0) {
                        minutes += 24 * 60;
                    }
                    if (minutes <= 0) {
                        minutes = 1;
                    }

                    TrainTrips trip = new TrainTrips(departureStop, arrivalStop, dayType, departureTime);
                    trip.setDuration((int) minutes);
                    trip.setRouteNumber(routeNumber);
                    if (!rawRouteCode.isEmpty()) {
                        trip.setRouteCode(rawRouteCode);
                    }
                    if (!rawDirection.isEmpty()) {
                        trip.setDirection(rawDirection);
                    }
                    trip.setTripID(baseTripId + "-S" + (idx + 1));

                    trainTrips.add(trip);
                    departureStop.addTrainTrip(trip);
                }
            }
        }
    }

    private void collectStopTime(String[] columns, String[] stationNames, List<TrainStop> orderedStops, List<LocalTime> orderedTimes, int idx) {
        String timeStr = columns[idx].trim();
        if (timeStr.isEmpty()) {
            return;
        }

        TrainStop stop = getStopByName(stationNames[idx].trim());
        if (stop == null) {
            return;
        }

        try {
            LocalTime parsedTime = LocalTime.parse(timeStr, TIME_FORMAT);
            orderedStops.add(stop);
            orderedTimes.add(parsedTime);
        } catch (DateTimeParseException e) {
            // ignore malformed times
        }
    }

    /**
     * Loads all route numbers from the summary CSV and loads the respective trip
     * schedules.
     */
    public void LoadTrainTrips() {
        String line;
        String csvSplitBy = ","; // tab-delimited, change to "," if comma-separated
        
        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(summaryTrips)) {
            // skip header line
            br.readLine();
            int i = 0;
            while ((line = br.readLine()) != null) {
                i++;
                String[] fields = line.split(csvSplitBy);
                if (i > 0) {
                    String routeNumber = fields[0].trim();
                    routeNumbers.add(routeNumber);
                    String fileTripName = String.format(
                            "CapeTownTransitData/train-schedules-2014/%s.csv",
                            routeNumber);
                            
                    this.loadTripsFromCSV(fileTripName, routeNumber);
                    ////// System.out.println(fileTripName);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /* =============================================================================================
     * Results class to will basically turn into journey class
      ==============================================================================================*/


    /**
     * Result class representing the outcome of earliest arrival calculation.
     */
    public static class Result {
        public final int totalMinutes;
        public final List<TrainTrips> trips;

        public Result(int totalMinutes, List<TrainTrips> trips) {
            this.totalMinutes = totalMinutes;
            this.trips = Collections.unmodifiableList(new ArrayList<>(trips));
        }

        public boolean hasJourney() {
            return totalMinutes >= 0 && !trips.isEmpty();
        }

        @Override
        public String toString() {
            if (!hasJourney()) {
                return "No valid path found.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Total travel time: ").append(totalMinutes).append(" min\n");
            sb.append("Path: ");
            for (int i = 0; i < trips.size(); i++) {
                TrainTrips trip = trips.get(i);
                if (i == 0) {
                    sb.append(trip.getDepartureTrainStop().getName()).append(" -> ");
                }
                sb.append(trip.getDestinationTrainStop().getName());
                if (i < trips.size() - 1) {
                    sb.append(" -> ");
                }
            }
            return sb.toString();
        }

        public List<TrainTrips> getTrips() {
            return trips;
        }
    }

    /**
     * Fixed RAPTOR Algorithm implementation with DayType filtering
     */
    public static class TrainRaptor {

        private final TrainGraph trainGraph;
        private static final LocalTime INF = LocalTime.MAX;

        public TrainRaptor(TrainGraph graph) {
            this.trainGraph = graph;
        }

        /**
         * Runs RAPTOR to compute the earliest arrival path with DayType filtering.
         *
         * @param sourceName   starting station name
         * @param targetName   destination station name
         * @param departureTime desired start time
         * @param dayType      day type to filter trips (WEEKDAY, SATURDAY, SUNDAY, HOLIDAY)
         * @param maxRounds    maximum number of rounds to explore
         * @return TrainJourney object or null if no path found
         */
        public TrainJourney runRaptor(String sourceName, String targetName, LocalTime departureTime, 
                                    Trip.DayType dayType, int maxRounds) {
            long startTime = System.currentTimeMillis();

            TrainStop source = trainGraph.getStopByName(sourceName);
            TrainStop target = trainGraph.getStopByName(targetName);

            if (source == null || target == null) {
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return null;
            }

            // If source equals target, return immediate journey
            if (source.equals(target)) {
                TrainJourney immediateJourney = new TrainJourney(source, target, departureTime, 
                                                            departureTime, new ArrayList<>());
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return immediateJourney;
            }

            List<TrainStop> stops = trainGraph.getTrainStops();

            // RAPTOR data structures - using new instances for each query to avoid memory leaks
            Map<TrainStop, LocalTime> prevArrival = new HashMap<>(stops.size());
            Map<TrainStop, LocalTime> currArrival = new HashMap<>(stops.size());

            // Initialize all stops with infinity
            for (TrainStop s : stops) {
                prevArrival.put(s, INF);
                currArrival.put(s, INF);
            }
            prevArrival.put(source, departureTime);

            // Parent tracking for path reconstruction
            Map<TrainStop, TrainTrips> parentTrip = new HashMap<>();
            Map<TrainStop, TrainStop> parentStop = new HashMap<>();

            // Filter trips by day type and sort by departure time
            List<TrainTrips> filteredTrips = trainGraph.getTrainTrips().stream()
                .filter(trip -> isValidForDayType(trip, dayType))
                .sorted((a, b) -> {
                    LocalTime da = a.getDepartureTime() != null ? a.getDepartureTime() : LocalTime.MIN;
                    LocalTime db = b.getDepartureTime() != null ? b.getDepartureTime() : LocalTime.MIN;
                    return da.compareTo(db);
                })
                .collect(java.util.stream.Collectors.toList());

            boolean improved = false;

            // RAPTOR rounds (1 to maxRounds)
            for (int round = 1; round <= maxRounds; round++) {
                // Copy previous round arrivals to current round
                for (TrainStop s : stops) {
                    currArrival.put(s, prevArrival.get(s));
                }

                improved = false;

                // Scan all eligible trips in chronological order
                for (TrainTrips trip : filteredTrips) {
                    TrainStop depStop = trip.getDepartureTrainStop();
                    TrainStop destStop = trip.getDestinationTrainStop();
                    LocalTime tripDepTime = trip.getDepartureTime();

                    if (tripDepTime == null || depStop == null || destStop == null) {
                        continue; // Skip malformed trips
                    }

                    LocalTime earliestAtDep = prevArrival.get(depStop);
                    if (earliestAtDep == null) earliestAtDep = INF;

                    // Can only board if we arrive at departure stop before or at trip departure time
                    if (!earliestAtDep.equals(INF) && !earliestAtDep.isAfter(tripDepTime)) {
                        LocalTime arrivalAtDest = tripDepTime.plusMinutes(trip.getDuration());
                        LocalTime currentBestAtDest = currArrival.get(destStop);
                        if (currentBestAtDest == null) currentBestAtDest = INF;

                        // If this trip gives us a better arrival time at destination
                        if (arrivalAtDest.isBefore(currentBestAtDest)) {
                            currArrival.put(destStop, arrivalAtDest);
                            parentTrip.put(destStop, trip);
                            parentStop.put(destStop, depStop);
                            improved = true;
                        }
                    }
                }

                // If no improvement this round, we can stop early
                if (!improved) {
                    break;
                }

                // Swap maps for next iteration (reuse objects to reduce GC pressure)
                Map<TrainStop, LocalTime> temp = prevArrival;
                prevArrival = currArrival;
                currArrival = temp;
            }

            LocalTime bestArrivalAtTarget = prevArrival.get(target);
            if (bestArrivalAtTarget == null || bestArrivalAtTarget.equals(INF)) {
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return null; // No journey found
            }

            // Reconstruct path by following parent pointers
            List<TrainTrips> journeyTrips = reconstructPath(target, source, parentTrip, parentStop);

            if (journeyTrips.isEmpty()) {
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return null;
            }

            TrainJourney journey = new TrainJourney(source, target, departureTime, 
                                                bestArrivalAtTarget, journeyTrips);

            trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
            return journey;
        }

        /**
         * Reconstructs the journey path from parent tracking maps.
         */
        private List<TrainTrips> reconstructPath(TrainStop target, TrainStop source,
                                            Map<TrainStop, TrainTrips> parentTrip,
                                            Map<TrainStop, TrainStop> parentStop) {
            List<TrainTrips> path = new ArrayList<>();
            TrainStop current = target;

            // Walk backwards from target to source
            while (!current.equals(source) && parentTrip.containsKey(current)) {
                TrainTrips trip = parentTrip.get(current);
                path.add(trip);
                
                TrainStop previous = parentStop.get(current);
                if (previous == null) {
                    break; // Defensive programming
                }
                current = previous;
            }

            // Reverse to get correct order (source -> target)
            java.util.Collections.reverse(path);
            return path;
        }

        /**
         * Checks if a trip is valid for the given day type.
         * You can extend this logic based on your business rules.
         */
        private boolean isValidForDayType(TrainTrips trip, Trip.DayType requestedDayType) {
            Trip.DayType tripDayType = trip.getDayType();
            
            if (tripDayType == null || requestedDayType == null) {
                return true; // If no day type specified, include all trips
            }

            // Exact match
            if (tripDayType == requestedDayType) {
                return true;
            }

            // Business logic: you might want to add rules like:
            // - WEEKDAY trips might run on HOLIDAY if it's a working holiday
            // - Special handling for different day types
            
            return false; // Only exact matches for now
        }
    }

    // Updated method in TrainGraph class to fix the signature mismatch
    /**
     * Computes the earliest arrival time and path from start to end station 
     * given a start time and day type.
     *
     * @param startName Name of the starting station
     * @param endName   Name of the destination station
     * @param startTime Departure time
     * @param dayType   Day type for filtering trips
     * @return TrainJourney object containing the journey details
     */
    public TrainJourney findEarliestArrival(String startName, String endName, LocalTime startTime, Trip.DayType dayType) {
        TrainRaptor raptor = new TrainRaptor(this);
        return raptor.runRaptor(startName, endName, startTime, dayType, 6);
    }

    // Overloaded method for backward compatibility (defaults to WEEKDAY)
    public TrainJourney findEarliestArrival(String startName, String endName, LocalTime startTime) {
        return findEarliestArrival(startName, endName, startTime, Trip.DayType.WEEKDAY);
    }
    // (keep all your existing code here ...)

    /* ================================================================================================
     * Metrics Printer
     * ================================================================================================ */
    public void printMetrics(long stopLoadTimeMs, long tripLoadTimeMs) {
        System.out.println("\n=== TrainGraph Metrics ===");
        System.out.println("Stop load time: " + stopLoadTimeMs + " ms");
        System.out.println("Trip load time: " + tripLoadTimeMs + " ms");

        // optional: memory usage
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        System.out.println("Approx. memory usage: " + usedMem + " MB");
    }

    public static void printJourneyWithTimes(TrainJourney journey) {
        if (journey == null || journey.getTrips().isEmpty()) {
            System.out.println("No valid journey found.");
            return;
        }

        System.out.println("Journey from " + journey.getSource().getName() +
                        " to " + journey.getDestination().getName() +
                        " starting at " + journey.getDepartureTime());
        System.out.println("Total duration: " + journey.getTotalDurationMinutes() + " minutes");

        for (TrainTrips trip : journey.getTrips()) {
            System.out.printf(
                "%s (%s) -> %s (%s) | Duration: %d min | Route: %s | Day: %s \n",
                trip.getDepartureTrainStop().getName(),
                trip.getDepartureTime(),
                trip.getDestinationTrainStop().getName(),
                trip.getDepartureTime().plusMinutes(trip.getDuration()),
                trip.getDuration(),
                trip.getRouteNumber(),
                trip.getDayType()
            );
        }
    }


    /**
     * Focused RAPTOR test with comprehensive mock data generation
     */
    public static void main(String[] args) throws IOException {
        System.out.println("=== RAPTOR Algorithm Test ===\n");
        
        TrainGraph graph = new TrainGraph();
        
        // Load real stops
        graph.loadTrainStops();
        System.out.printf("Loaded %d real train stops\n", graph.getTrainStops().size());
        
        // Generate comprehensive mock trip data for RAPTOR testing
        generateRichMockTripData(graph);
        System.out.printf("Generated %d mock trips across %d routes\n", 
                        graph.getTrainTrips().size(), graph.routeNumbers.size());
        
        // Test RAPTOR extensively
        testRaptorAlgorithm(graph);
    }

    /**
     * Generates comprehensive mock trip data for thorough RAPTOR testing
     */
    private static void generateRichMockTripData(TrainGraph graph) {
        List<TrainStop> stops = graph.getTrainStops();
        if (stops.size() < 5) {
            System.out.println("Not enough stops for comprehensive testing");
            return;
        }
        
        // Find key Cape Town stops
        TrainStop capeTown = findStop(stops, "CAPE TOWN");
        TrainStop rondebosch = findStop(stops, "RONDEBOSCH");
        TrainStop claremont = findStop(stops, "CLAREMONT");
        TrainStop wynberg = findStop(stops, "WYNBERG");
        TrainStop athlone = findStop(stops, "ATHLONE");
        
        // Use first available stops if specific ones not found
        List<TrainStop> testStops = new ArrayList<>();
        if (capeTown != null) testStops.add(capeTown);
        if (rondebosch != null) testStops.add(rondebosch);
        if (claremont != null) testStops.add(claremont);
        if (wynberg != null) testStops.add(wynberg);
        if (athlone != null) testStops.add(athlone);
        
        // Fill with any available stops if we don't have enough
        while (testStops.size() < 8 && testStops.size() < stops.size()) {
            for (TrainStop stop : stops) {
                if (!testStops.contains(stop)) {
                    testStops.add(stop);
                    if (testStops.size() >= 8) break;
                }
            }
        }
        
        System.out.printf("Using %d stops for mock data generation:\n", testStops.size());
        for (TrainStop stop : testStops) {
            System.out.printf("  - %s\n", stop.getName());
        }
        
        // Route 1: Main Line (North-South)
        createRoute(graph, "MAIN-LINE", testStops.subList(0, Math.min(5, testStops.size())), 
                LocalTime.of(5, 30), 30, 15, Trip.DayType.WEEKDAY);
        createRoute(graph, "MAIN-LINE", testStops.subList(0, Math.min(5, testStops.size())), 
                LocalTime.of(6, 30), 60, 20, Trip.DayType.SATURDAY);
        
        // Route 2: Circle Line (connecting different areas)
        if (testStops.size() >= 4) {
            List<TrainStop> circleStops = new ArrayList<>();
            circleStops.add(testStops.get(0)); // Start
            circleStops.add(testStops.get(2)); // Middle
            circleStops.add(testStops.get(3)); // End
            circleStops.add(testStops.get(1)); // Back to connect
            
            createRoute(graph, "CIRCLE-LINE", circleStops, 
                    LocalTime.of(6, 0), 45, 12, Trip.DayType.WEEKDAY);
        }
        
        // Route 3: Express Line (fewer stops, faster)
        if (testStops.size() >= 3) {
            createRoute(graph, "EXPRESS", Arrays.asList(testStops.get(0), testStops.get(2), testStops.get(4 % testStops.size())), 
                    LocalTime.of(7, 0), 20, 8, Trip.DayType.WEEKDAY);
        }
        
        // Route 4: Weekend Service (different schedule)
        createRoute(graph, "WEEKEND-LINE", testStops.subList(0, Math.min(4, testStops.size())), 
                LocalTime.of(8, 0), 90, 18, Trip.DayType.SATURDAY);
        createRoute(graph, "WEEKEND-LINE", testStops.subList(0, Math.min(4, testStops.size())), 
                LocalTime.of(9, 0), 120, 25, Trip.DayType.SUNDAY);
        
        // Route 5: Peak Hour Service (frequent during rush)
        if (testStops.size() >= 3) {
            // Morning peak
            createRoute(graph, "PEAK-AM", Arrays.asList(testStops.get(1), testStops.get(0), testStops.get(2)), 
                    LocalTime.of(6, 30), 15, 10, Trip.DayType.WEEKDAY);
            // Evening peak  
            createRoute(graph, "PEAK-PM", Arrays.asList(testStops.get(0), testStops.get(1), testStops.get(2)), 
                    LocalTime.of(17, 0), 15, 12, Trip.DayType.WEEKDAY);
        }
        
        // Add some reverse direction trips
        if (testStops.size() >= 4) {
            List<TrainStop> reverseStops = new ArrayList<>(testStops.subList(0, 4));
            Collections.reverse(reverseStops);
            createRoute(graph, "MAIN-LINE-REV", reverseStops, 
                    LocalTime.of(6, 15), 30, 16, Trip.DayType.WEEKDAY);
        }
    }

    /**
     * Creates a route with multiple trips throughout the day
     */
    private static void createRoute(TrainGraph graph, String routeName, List<TrainStop> stops, 
                                LocalTime startTime, int intervalMinutes, int tripDuration, 
                                Trip.DayType dayType) {
        if (stops.size() < 2) return;
        
        if (!graph.routeNumbers.contains(routeName)) {
            graph.routeNumbers.add(routeName);
        }
        
        LocalTime currentTime = startTime;
        int tripNumber = 1;
        
        // Create trips for 16 hours (realistic service day)
        while (currentTime.isBefore(LocalTime.of(22, 0))) {
            // Create trip segments between consecutive stops
            for (int i = 0; i < stops.size() - 1; i++) {
                TrainStop from = stops.get(i);
                TrainStop to = stops.get(i + 1);
                
                TrainTrips trip = new TrainTrips(from, to, dayType, currentTime);
                trip.setDuration(tripDuration);
                trip.setRouteNumber(routeName);
                trip.setTripID(routeName + "-T" + tripNumber + "-S" + (i + 1));
                trip.setDirection(i == 0 ? "OUTBOUND" : "OUTBOUND");
                
                graph.getTrainTrips().add(trip);
                from.addTrainTrip(trip);
                
                // Next segment departs after this one arrives
                currentTime = currentTime.plusMinutes(tripDuration);
            }
            
            // Reset time for next complete trip and add interval
            currentTime = startTime.plusMinutes(intervalMinutes * tripNumber);
            tripNumber++;
            
            // Prevent infinite loop
            if (tripNumber > 50) break;
        }
    }

    /**
     * Find stop by name (case insensitive, partial match)
     */
    private static TrainStop findStop(List<TrainStop> stops, String name) {
        for (TrainStop stop : stops) {
            if (stop.getName().toUpperCase().contains(name.toUpperCase())) {
                return stop;
            }
        }
        return null;
    }

    /**
     * Comprehensive RAPTOR algorithm testing
     */
    private static void testRaptorAlgorithm(TrainGraph graph) {
        System.out.println("\n=== RAPTOR ALGORITHM TESTING ===");
        
        List<TrainStop> stops = graph.getTrainStops();
        if (stops.size() < 2) {
            System.out.println("Need at least 2 stops for testing");
            return;
        }
        
        // Get some test stops
        List<TrainStop> testStops = stops.subList(0, Math.min(8, stops.size()));
        
        System.out.printf("Testing with %d stops and %d trips\n", 
                        stops.size(), graph.getTrainTrips().size());
        
        // Test Case 1: Basic Journey Planning
        System.out.println("\n--- Test 1: Basic Journeys ---");
        testBasicJourneys(graph, testStops);
        
        // Test Case 2: Different Day Types
        System.out.println("\n--- Test 2: Day Type Filtering ---");
        testDayTypes(graph, testStops);
        
        // Test Case 3: Time-based Planning
        System.out.println("\n--- Test 3: Time-based Planning ---");
        testTimePlanning(graph, testStops);
        
        // Test Case 4: Multi-hop Journeys
        System.out.println("\n--- Test 4: Multi-hop Journeys ---");
        testMultiHopJourneys(graph, testStops);
        
        // Test Case 5: Performance Testing
        System.out.println("\n--- Test 5: Performance Testing ---");
        testPerformance(graph, testStops);
        
        // Test Case 6: Edge Cases
        System.out.println("\n--- Test 6: Edge Cases ---");
        testEdgeCases(graph, testStops);
    }

    private static void testBasicJourneys(TrainGraph graph, List<TrainStop> stops) {
        for (int i = 0; i < Math.min(3, stops.size() - 1); i++) {
            TrainStop from = stops.get(i);
            TrainStop to = stops.get(i + 1);
            
            TrainJourney journey = graph.findEarliestArrival(
                from.getName(), to.getName(), LocalTime.of(8, 0), Trip.DayType.WEEKDAY);
            
            if (journey != null) {
                System.out.printf("✓ %s → %s: %d min, %d transfers\n",
                                from.getName(), to.getName(),
                                journey.getTotalDurationMinutes(),
                                journey.getNumberOfTransfers());
            } else {
                System.out.printf("✗ %s → %s: No route found\n",
                                from.getName(), to.getName());
            }
        }
    }

    private static void testDayTypes(TrainGraph graph, List<TrainStop> stops) {
        if (stops.size() < 2) return;
        
        TrainStop from = stops.get(0);
        TrainStop to = stops.get(1);
        
        Trip.DayType[] dayTypes = {Trip.DayType.WEEKDAY, Trip.DayType.SATURDAY, Trip.DayType.SUNDAY};
        
        for (Trip.DayType dayType : dayTypes) {
            TrainJourney journey = graph.findEarliestArrival(
                from.getName(), to.getName(), LocalTime.of(10, 0), dayType);
            
            if (journey != null) {
                System.out.printf("✓ %s (%s): %d min, %d trips\n",
                                dayType, from.getName() + "→" + to.getName(),
                                journey.getTotalDurationMinutes(),
                                journey.getTrips().size());
            } else {
                System.out.printf("✗ %s: No service\n", dayType);
            }
        }
    }

    private static void testTimePlanning(TrainGraph graph, List<TrainStop> stops) {
        if (stops.size() < 2) return;
        
        TrainStop from = stops.get(0);
        TrainStop to = stops.get(1);
        
        LocalTime[] times = {
            LocalTime.of(6, 0),   // Early morning
            LocalTime.of(8, 30),  // Morning rush
            LocalTime.of(12, 0),  // Midday
            LocalTime.of(17, 30), // Evening rush
            LocalTime.of(21, 0)   // Evening
        };
        
        for (LocalTime time : times) {
            TrainJourney journey = graph.findEarliestArrival(
                from.getName(), to.getName(), time, Trip.DayType.WEEKDAY);
            
            if (journey != null) {
                System.out.printf("✓ Depart %s: Arrive %s (%d min)\n",
                                time, journey.getArrivalTime(),
                                journey.getTotalDurationMinutes());
            } else {
                System.out.printf("✗ Depart %s: No service\n", time);
            }
        }
    }

    private static void testMultiHopJourneys(TrainGraph graph, List<TrainStop> stops) {
        // Test longer journeys that might require transfers
        for (int i = 0; i < Math.min(2, stops.size() - 2); i++) {
            TrainStop from = stops.get(i);
            TrainStop to = stops.get(i + 2); // Skip one stop
            
            TrainJourney journey = graph.findEarliestArrival(
                from.getName(), to.getName(), LocalTime.of(9, 0), Trip.DayType.WEEKDAY);
            
            if (journey != null) {
                System.out.printf("✓ %s → %s: %d min, %d transfers, %d legs\n",
                                from.getName(), to.getName(),
                                journey.getTotalDurationMinutes(),
                                journey.getNumberOfTransfers(),
                                journey.getTrips().size());
                
                // Show detailed path for multi-hop journeys
                if (journey.getTrips().size() > 1) {
                    for (TrainTrips trip : journey.getTrips()) {
                        System.out.printf("    [%s] %s → %s at %s\n",
                                        trip.getRouteNumber(),
                                        trip.getDepartureTrainStop().getName(),
                                        trip.getDestinationTrainStop().getName(),
                                        trip.getDepartureTime());
                    }
                }
            } else {
                System.out.printf("✗ %s → %s: No route found\n",
                                from.getName(), to.getName());
            }
        }
    }

    private static void testPerformance(TrainGraph graph, List<TrainStop> stops) {
        if (stops.size() < 2) return;
        
        TrainStop from = stops.get(0);
        TrainStop to = stops.get(stops.size() - 1);
        
        // Run multiple queries and measure performance
        long totalTime = 0;
        int successfulQueries = 0;
        int numTests = 10;
        
        for (int i = 0; i < numTests; i++) {
            long start = System.currentTimeMillis();
            TrainJourney journey = graph.findEarliestArrival(
                from.getName(), to.getName(), 
                LocalTime.of(8 + i % 12, 0), Trip.DayType.WEEKDAY);
            long elapsed = System.currentTimeMillis() - start;
            
            totalTime += elapsed;
            if (journey != null) successfulQueries++;
        }
        
        System.out.printf("Performance: %d/%d successful queries\n", successfulQueries, numTests);
        System.out.printf("Average query time: %.1f ms\n", totalTime / (double) numTests);
        System.out.printf("Last query latency: %d ms\n", graph.getMetrics().get("lastRaptorLatencyMs"));
    }

    private static void testEdgeCases(TrainGraph graph, List<TrainStop> stops) {
        if (stops.isEmpty()) return;
        
        TrainStop testStop = stops.get(0);
        
        // Same source and destination
        TrainJourney sameJourney = graph.findEarliestArrival(
            testStop.getName(), testStop.getName(), LocalTime.of(10, 0));
        System.out.printf("Same source/dest: %s (duration: %d min)\n",
                        sameJourney != null ? "✓ Handled" : "✗ Failed",
                        sameJourney != null ? sameJourney.getTotalDurationMinutes() : -1);
        
        // Non-existent stops
        TrainJourney invalidJourney = graph.findEarliestArrival(
            "NONEXISTENT_STOP", testStop.getName(), LocalTime.of(10, 0));
        System.out.printf("Invalid source: %s\n",
                        invalidJourney == null ? "✓ Correctly null" : "✗ Should be null");
        
        // Very late departure
        TrainJourney lateJourney = graph.findEarliestArrival(
            testStop.getName(), stops.get(Math.min(1, stops.size() - 1)).getName(), 
            LocalTime.of(23, 59));
        System.out.printf("Late departure: %s\n",
                        lateJourney != null ? "✓ Found route" : "✗ No late service");
        
        // Very early departure
        TrainJourney earlyJourney = graph.findEarliestArrival(
            testStop.getName(), stops.get(Math.min(1, stops.size() - 1)).getName(), 
            LocalTime.of(4, 0));
        System.out.printf("Early departure: %s\n",
                        earlyJourney != null ? "✓ Found route" : "✗ No early service");
    }
}
