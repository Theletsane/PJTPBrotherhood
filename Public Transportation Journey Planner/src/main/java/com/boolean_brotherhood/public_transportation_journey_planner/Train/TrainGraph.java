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
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.MyFileLoader;
import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

public class TrainGraph {


    // Public Transportation Journey
    // Planner/src/main/resources/CapeTownTransitData/complete_metrorail_stations.csv
    private final String Complete_Metrorail_Stations;
    private final String summaryTrips ;

    // Data structures
    private final List<TrainStop> trainStops = new ArrayList<>();
    public List<String> routeNumbers = new ArrayList<>();
    private final List<TrainTrips> trainTrips = new ArrayList<>();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    
    public TrainGraph(){
        Complete_Metrorail_Stations = "CapeTownTransitData/complete_metrorail_stations.csv";
        summaryTrips = "CapeTownTransitData/train-routes-summary.csv";
    }

    /**
     * Loads all route numbers from the summary CSV and loads the respective trip
     * schedules.
     */
    public void loadRouteNumber() {
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
                            
                    loadTripsFromCSV(fileTripName, routeNumber);
                    ////// System.out.println(fileTripName);
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
    public void loadTripsFromCSV(String filePath, String routeNumber) throws IOException {
        
        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(filePath)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file empty: " + filePath);
            }
            String[] stationNames = headerLine.split(",");

            String line;
            int tripCounter = 1;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                String[] times = line.split(",");

                TrainStop prevStop = null;
                LocalTime prevTime = null;

                for (int i = 0; i < times.length && i < stationNames.length; i++) {
                    String timeStr = times[i].trim();
                    if (timeStr.isEmpty())
                        continue;

                    try {
                        LocalTime parsedTime = LocalTime.parse(timeStr, TIME_FORMAT);

                        TrainStop currentStop = this.findStopByName(stationNames[i].trim());
                        // If the CSV references a station name not in your stops list, currentStop may
                        // be null.
                        // Skip creating a trip when either stop is missing.
                        if (currentStop == null) {
                            // advance prevStop/prevTime to avoid creating invalid edges
                            prevStop = null;
                            prevTime = null;
                            continue;
                        }

                        if (prevStop != null && prevTime != null) {
                            long diffMinutes = Duration.between(prevTime, parsedTime).toMinutes();
                            int duration;
                            if (diffMinutes < 0) {
                                if (Math.abs(diffMinutes) > 12 * 60) {
                                    duration = (int) (diffMinutes + 24 * 60);
                                } else {
                                    duration = (int) Math.abs(diffMinutes);
                                }
                            } else {
                                duration = (int) diffMinutes;
                            }
                            if (duration <= 0)
                                duration = 1;

                            TrainTrips trip = new TrainTrips(prevStop, currentStop, Trip.DayType.WEEKDAY);
                            trip.setDuration(duration); // store scheduled duration
                            trip.setDepartureTime(prevTime); // <-- IMPORTANT: set departure time
                            trip.setRouteNumber(routeNumber);
                            trip.setTripID(routeNumber + "-T" + tripCounter++);
                            trainTrips.add(trip);

                            // System.out.println("Added trip: " + trip + " dep:" + prevTime + " dur:" +
                            // duration + "min");
                        }

                        prevStop = currentStop;
                        prevTime = parsedTime;

                    } catch (Exception e) {
                        // parsing failure - skip this cell
                    }
                }
            }
        }
    }

    /**
     * Loads train stop data from the CSV file into the trainStops list.
     */
    public void loadTrainStops() throws IOException {
        String line;

        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(Complete_Metrorail_Stations)) {
            // skip header line
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length >= 5) {
                    String name = parts[0].toUpperCase(); // ATHLONE
                    String code = parts[1]; // ATL
                    String type = parts[2]; // Train
                    double lat = Double.parseDouble(parts[3]); // -33.96089
                    double lon = Double.parseDouble(parts[4]); // 18.501622

                    TrainStop stop = new TrainStop(type, lat, lon, name, code);
                    trainStops.add(stop);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Generates a 2-3 character ID from a station name by skipping vowels.
     *
     * @param name Station name
     * @return Generated station ID
     */
    private String getIDFromName(String name) {
        String stationName = name;
        String stationID = "";

        stationID += stationName.charAt(0);

        for (int i = 1; i < stationName.length() && stationID.length() < 3; i++) {
            char c = stationName.charAt(i);
            if ("AEIOU".indexOf(Character.toUpperCase(c)) == -1) { // skip vowels
                stationID += c;
            }
        }
        stationID = stationID.toUpperCase();
        return stationID;

    }


    /**
     * Finds a train stop by its name.
     *
     * @param name Name of the stop
     * @return TrainStop object or null if not found
     */
    private TrainStop findStopByName(String name) {
        for (TrainStop stop : this.trainStops) {

            if (stop.getName().equalsIgnoreCase(name.trim())) {
                //// System.out.println(stop);
                return stop;
            }
        }
        return null;
    }

    /**
     * Computes the earliest arrival time and path from start to end station given a
     * start time.
     *
     * @param startName Name of the starting station
     * @param endName   Name of the destination station
     * @param startTime Departure time
     * @return Result object containing total minutes and path
     */
    public Result findEarliestArrival(String startName, String endName, LocalTime startTime) {
        TrainStop start = findStopByName(startName);
        TrainStop end = findStopByName(endName);

        if (start == null || end == null) {
            //// System.out.println("Start or end stop not found: " + startName + " -> " +
            //// endName);
            return new Result(-1, Collections.emptyList());
        }

        // earliest known arrival times
        Map<TrainStop, LocalTime> arrivalTimes = new HashMap<>();
        Map<TrainStop, TrainStop> previous = new HashMap<>();
        PriorityQueue<TrainStop> pq = new PriorityQueue<>(Comparator.comparing(arrivalTimes::get));

        for (TrainStop stop : trainStops) {
            arrivalTimes.put(stop, LocalTime.MAX); // "infinity"
        }
        arrivalTimes.put(start, startTime);
        pq.add(start);

        while (!pq.isEmpty()) {
            TrainStop current = pq.poll();
            LocalTime currentArrival = arrivalTimes.get(current);

            if (current.equals(end))
                break; // reached destination

            // Explore all trips leaving from current
            for (TrainTrips trip : getOutgoingTrips(current)) {
                LocalTime depTime = trip.getDepartureTime();
                if (depTime.isBefore(currentArrival)) {
                    continue; // can’t catch this train, already left
                }

                LocalTime newArrival = depTime.plusMinutes(trip.getDuration());
                TrainStop neighbor = trip.getDestinationTrainStop();

                if (newArrival.isBefore(arrivalTimes.get(neighbor))) {
                    arrivalTimes.put(neighbor, newArrival);
                    previous.put(neighbor, current);
                    pq.remove(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        // reconstruct path
        List<TrainStop> path = new ArrayList<>();
        TrainStop step = end;
        if (previous.get(step) != null || step.equals(start)) {
            while (step != null) {
                path.add(step);
                step = previous.get(step);
            }
            Collections.reverse(path);
        }

        LocalTime arrival = arrivalTimes.get(end);
        if (arrival.equals(LocalTime.MAX)) {
            return new Result(-1, Collections.emptyList());
        } else {
            int minutes = (int) Duration.between(startTime, arrival).toMinutes();
            return new Result(minutes, path);
        }
    }

    /**
     * Retrieves all outgoing trips starting from a specific stop.
     *
     * @param stop TrainStop object
     * @return List of TrainTrips starting from the stop
     */
    private List<TrainTrips> getOutgoingTrips(TrainStop stop) {
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
            double d = stop.getDistanceBetween(lat, lon);
            if (d < minDist) {
                minDist = d;
                nearest = stop;
            }
        }
        return nearest;
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

    /**
     * Returns a list of the closest train stops to given coordinates (up to
     * maxStops).
     *
     * @param lat      Latitude
     * @param lon      Longitude
     * @param maxStops Maximum stops to return
     * @return List of nearest TrainStop objects
     */
    public List<TrainStop> getNearestTaxiStops(double lat, double lon, int maxStops) {
        return this.trainStops.stream()
                .sorted((a, b) -> Double.compare(a.getDistanceBetween(lat, lon), b.getDistanceBetween(lat, lon)))
                .limit(maxStops)
                .collect(Collectors.toList());
    }

    /**
     * Computes earliest arrival times using RAPTOR.
     *
     * @param stops         list of all stops
     * @param source        starting stop
     * @param departureTime time in minutes from midnight
     * @param maxRounds     maximum number of rounds / transfers
     * @return Map of Stop -> earliest arrival time
     */
    public Map<TrainStop, Integer> computeRAPTOR(TrainStop source, int departureTime, int maxRounds) {
        Map<TrainStop, Integer> arrivalTimes = new HashMap<>();
        for (TrainStop s : trainStops)
            arrivalTimes.put(s, Integer.MAX_VALUE);
        arrivalTimes.put(source, departureTime);

        for (int round = 0; round < maxRounds; round++) {
            boolean updated = false;

            for (TrainStop stop : trainStops) {
                for (TrainTrips route : stop.getTrainTripsFromStop()) {
                    int currentTime = arrivalTimes.get(stop);
                    Stop nextStop = route.getDestinationStop();
                    int departure = route.getDuration();
                    if (departure >= currentTime && currentTime + 0 < arrivalTimes.get(nextStop)) {
                        arrivalTimes.put((TrainStop) nextStop, departure); // simplified, ignores in-vehicle travel
                        updated = true;
                    }
                }
            }

            if (!updated)
                break; // no improvements in this round
        }

        return arrivalTimes;
    }

    /**
     * Result class representing the outcome of earliest arrival calculation.
     */
    public static class Result {
        public final int totalMinutes;
        public final List<TrainStop> path;

        public Result(int totalMinutes, List<TrainStop> path) {
            this.totalMinutes = totalMinutes;
            this.path = path;
        }

        @Override
        public String toString() {
            if (totalMinutes == -1 || path.isEmpty()) {
                return "No valid path found.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Total travel time: ").append(totalMinutes).append(" min\n");
            sb.append("Path: ");
            for (int i = 0; i < path.size(); i++) {
                sb.append(path.get(i).getName());
                if (i < path.size() - 1)
                    sb.append(" -> ");
            }
            return sb.toString();
        }
    }
}
