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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.DataFilesRegistry;
import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.MyFileLoader;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.SystemLog;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
public class TrainGraph {




    // Public Transportation Journey
    private final String Complete_Metrorail_Stations;
    private final String summaryTrips ;
    private final String usageTag;

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

    private BufferedReader openResource(String resourcePath) throws IOException {
        BufferedReader reader = MyFileLoader.getBufferedReaderFromResource(resourcePath);
        if (reader == null) {
            throw new IOException("Resource not found on classpath: " + resourcePath);
        }
        return reader;
    }

    public TrainGraph(){
        this.usageTag = this.getClass().getSimpleName();
        Complete_Metrorail_Stations = DataFilesRegistry.getFile("TRAIN_STOPS", usageTag);
        summaryTrips = DataFilesRegistry.getFile("TRAIN_TRIPS", usageTag);
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
        long start = System.currentTimeMillis();
        SystemLog.log_event("TRAIN_GRAPH", "Loading train stops", "INFO", Map.of(
                "source", Complete_Metrorail_Stations
        ));

        trainStops.clear();

        try (BufferedReader br = openResource(Complete_Metrorail_Stations)) {
            // skip header line
            String headers = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length >= 5) {

                    String name = parts[5].toUpperCase().trim(); // ATHLONE
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
                    SystemLog.add_stations(name, "TRAIN");
                }
            }
        }

        SystemLog.log_event("TRAIN_GRAPH", "Loaded train stops", "INFO", Map.of(
                "count", trainStops.size(),
                "elapsedMs", System.currentTimeMillis() - start
        ));
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
        long start = System.currentTimeMillis();
        int beforeTrips = trainTrips.size();
        SystemLog.log_event("TRAIN_GRAPH", "Loading train trips", "INFO", Map.of(
                "file", filePath,
                "route", routeNumber
        ));
        SystemLog.add_active_route(routeNumber);

        int segmentsAdded = 0;
        try (BufferedReader br = openResource(filePath)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file empty: " + filePath);
            }

            String[] stationNames = headerLine.split(",", -1);
            String line;
            int autoTripCounter = 1;
            segmentsAdded = 0;

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
                    segmentsAdded++;
                }
            }
        }

        SystemLog.log_event("TRAIN_GRAPH", "Loaded train trips", "INFO", Map.of(
                "route", routeNumber,
                "addedSegments", segmentsAdded,
                "elapsedMs", System.currentTimeMillis() - start
        ));
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
    public void LoadTrainTrips() throws IOException {
        String line;
        String csvSplitBy = ","; // tab-delimited, change to "," if comma-separated

        long start = System.currentTimeMillis();
        SystemLog.log_event("TRAIN_GRAPH", "Loading train trip schedules", "INFO", Map.of(
                "summaryResource", summaryTrips
        ));

        routeNumbers.clear();
        trainTrips.clear();
        for (TrainStop stop : trainStops) {
            stop.getTrips().removeIf(trip -> trip instanceof TrainTrips);
        }

        int routesProcessed = 0;
        try (BufferedReader br = openResource(summaryTrips)) {

            // skip header line
            br.readLine();
            int i = 0;
            while ((line = br.readLine()) != null) {
                i++;
                String[] fields = line.split(csvSplitBy);
                if (i > 0 && fields.length > 0) {
                    String routeNumber = fields[0].trim();
                    if (routeNumber.isEmpty()) {
                        continue;
                    }
                    routeNumber = routeNumber.toUpperCase();
                    routeNumbers.add(routeNumber);
                    routesProcessed++;

                    String scheduleResource = DataFilesRegistry.getTrainScheduleResource(routeNumber, usageTag);
                    try {
                        loadTripsFromCSV(scheduleResource, routeNumber);
                    } catch (IOException scheduleEx) {
                        SystemLog.log_event("TRAIN_GRAPH", "Failed to load train schedule", "ERROR", Map.of(
                                "route", routeNumber,
                                "resource", scheduleResource,
                                "error", scheduleEx.getMessage()
                        ));
                    }
                }

            }
        }

        SystemLog.log_event("TRAIN_GRAPH", "Loaded train trip schedules", "INFO", Map.of(
                "routes", routesProcessed,
                "tripSegments", trainTrips.size(),
                "elapsedMs", System.currentTimeMillis() - start
        ));
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
        private static final int MINUTES_PER_DAY = 24 * 60;

        public TrainRaptor(TrainGraph graph) {
            this.trainGraph = graph;
        }

        public TrainJourney runRaptor(String sourceName, String targetName, LocalTime departureTime,
                                      Trip.DayType dayType, int maxRounds) {
            long startTime = System.currentTimeMillis();

            TrainStop source = trainGraph.getStopByName(sourceName);
            TrainStop target = trainGraph.getStopByName(targetName);

            if (source == null || target == null) {
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return null;
            }

            if (source.equals(target)) {
                LocalTime baseline = departureTime != null ? departureTime : LocalTime.MIN;
                TrainJourney immediateJourney = new TrainJourney(source, target, baseline, baseline, new ArrayList<>());
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return immediateJourney;
            }

            if (departureTime == null) {
                departureTime = LocalTime.MIN;
            }
            int requestedDepartureMinutes = minutesOf(departureTime);

            List<TrainStop> stops = trainGraph.getTrainStops();
            Map<TrainStop, Integer> prevArrival = new HashMap<>(stops.size());
            Map<TrainStop, Integer> currArrival = new HashMap<>(stops.size());
            for (TrainStop stop : stops) {
                prevArrival.put(stop, Integer.MAX_VALUE);
                currArrival.put(stop, Integer.MAX_VALUE);
            }
            prevArrival.put(source, requestedDepartureMinutes);

            Map<TrainStop, TrainTrips> parentTrip = new HashMap<>();
            Map<TrainStop, TrainStop> parentStop = new HashMap<>();

            List<TrainTrips> filteredTrips = trainGraph.getTrainTrips().stream()
                    .filter(trip -> trip.getDepartureTime() != null)
                    .filter(trip -> isValidForDayType(trip, dayType))
                    .sorted(Comparator.comparing(TrainTrips::getDepartureTime))
                    .collect(java.util.stream.Collectors.toList());

            int rounds = Math.max(1, maxRounds);
            for (int round = 1; round <= rounds; round++) {
                for (TrainStop stop : stops) {
                    currArrival.put(stop, prevArrival.get(stop));
                }

                boolean improved = false;

                for (TrainTrips trip : filteredTrips) {
                    TrainStop depStop = trip.getDepartureTrainStop();
                    TrainStop destStop = trip.getDestinationTrainStop();
                    LocalTime departureLocal = trip.getDepartureTime();

                    if (depStop == null || destStop == null || departureLocal == null) {
                        continue;
                    }

                    int tripDepartureMinutes = minutesOf(departureLocal);
                    if (tripDepartureMinutes < requestedDepartureMinutes) {
                        continue;
                    }

                    int earliestAtDeparture = prevArrival.getOrDefault(depStop, Integer.MAX_VALUE);
                    if (tripDepartureMinutes < earliestAtDeparture) {
                        continue;
                    }

                    int durationMinutes = Math.max(1, trip.getDuration());
                    int arrivalMinutes = tripDepartureMinutes + durationMinutes;
                    if (arrivalMinutes >= MINUTES_PER_DAY) {
                        continue;
                    }

                    int currentBest = currArrival.getOrDefault(destStop, Integer.MAX_VALUE);
                    if (arrivalMinutes < currentBest) {
                        currArrival.put(destStop, arrivalMinutes);
                        parentTrip.put(destStop, trip);
                        parentStop.put(destStop, depStop);
                        improved = true;
                    }
                }

                if (!improved) {
                    break;
                }

                Map<TrainStop, Integer> temp = prevArrival;
                prevArrival = currArrival;
                currArrival = temp;
            }

            int arrivalMinutes = prevArrival.getOrDefault(target, Integer.MAX_VALUE);
            if (arrivalMinutes == Integer.MAX_VALUE) {
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return null;
            }

            List<TrainTrips> journeyTrips = reconstructPath(target, source, parentTrip, parentStop);
            if (journeyTrips.isEmpty()) {
                trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
                return null;
            }

            LocalTime arrivalTime = LocalTime.of(arrivalMinutes / 60, arrivalMinutes % 60);
            TrainJourney journey = new TrainJourney(source, target, departureTime, arrivalTime, journeyTrips);
            trainGraph.setLastRaptorLatency(System.currentTimeMillis() - startTime);
            return journey;
        }

        private List<TrainTrips> reconstructPath(TrainStop target, TrainStop source,
                                                Map<TrainStop, TrainTrips> parentTrip,
                                                Map<TrainStop, TrainStop> parentStop) {
            List<TrainTrips> path = new ArrayList<>();
            TrainStop current = target;
            Set<TrainStop> guard = new HashSet<>();

            while (!current.equals(source) && parentTrip.containsKey(current) && guard.add(current)) {
                TrainTrips trip = parentTrip.get(current);
                path.add(trip);
                TrainStop previous = parentStop.get(current);
                if (previous == null) {
                    break;
                }
                current = previous;
            }

            Collections.reverse(path);
            return path;
        }

        private static int minutesOf(LocalTime time) {
            return time.getHour() * 60 + time.getMinute();
        }

        private boolean isValidForDayType(TrainTrips trip, Trip.DayType requestedDayType) {
            Trip.DayType tripDayType = trip.getDayType();

            if (tripDayType == null || requestedDayType == null) {
                return true; // If no day type specified, include all trips
            }

            if (tripDayType == requestedDayType) {
                return true;
            }

            return false;
        }
    }    

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
        SystemLog.log_event("TRAIN_GRAPH", "findEarliestArrival invoked", "INFO", Map.of(
                "from", startName,
                "to", endName,
                "dayType", dayType != null ? dayType.name() : "ANY"
        ));
        long start = System.currentTimeMillis();
        TrainRaptor raptor = new TrainRaptor(this);
        TrainJourney journey = raptor.runRaptor(startName, endName, startTime, dayType, 6);
        SystemLog.log_event("TRAIN_GRAPH", "findEarliestArrival completed", (journey == null || journey.getTrips().isEmpty()) ? "WARN" : "INFO", Map.of(
                "from", startName,
                "to", endName,
                "segments", journey != null ? journey.getTrips().size() : 0,
                "elapsedMs", System.currentTimeMillis() - start
        ));
        return journey;
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

        // In TrainGraph.java, add this method:
    public RouteCoordinateExtractor.RouteSegment getRouteCoordinates(TrainTrips trip) throws IOException {
        RouteCoordinateExtractor extractor = new RouteCoordinateExtractor();
        return extractor.getRouteCoordinates(trip);
    }
 
}



