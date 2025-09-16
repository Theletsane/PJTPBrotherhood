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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    
    public TrainGraph(){
        //C:\Users\thele\OneDrive\Desktop\PJTPBrotherhood\src\main\resources\CapeTownTransitData\complete_metrorail_stations.csv
        Complete_Metrorail_Stations = "CapeTownTransitData/TrainStation.csv";
        summaryTrips = "CapeTownTransitData/train-routes-summary.csv";
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
     * Computes the earliest arrival time and path from start to end station given a
     * start time.
     *
     * @param startName Name of the starting station
     * @param endName   Name of the destination station
     * @param startTime Departure time
     * @return Result object containing total minutes and path
     */
    public TrainJourney findEarliestArrival(String startName, String endName, LocalTime startTime) {
        TrainRaptor raptor = new TrainRaptor(this);
        return raptor.runRaptor(startName,endName,startTime,6);

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
            double d = stop.getDistanceBetween(lat, lon);
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
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length >= 5) {

                    String name = parts[0].toUpperCase(); // ATHLONE
                    String code =parts[0].toUpperCase(); // ATL
                    double lat = Double.parseDouble(parts[1]); // -33.96089
                    double lon = Double.parseDouble(parts[2]); // 18.501622
                    String address =  parts[4];
                    for (int i = 5; i < parts.length; i++) {
                        address += ", " + parts[i];
                    }

                    TrainStop stop = new TrainStop("Train", lat, lon, name, code,address);

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

            if (headerLine == null) {throw new IOException("CSV file empty: " + filePath);}

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

                        TrainStop currentStop = this.getStopByName(stationNames[i].trim());

                        if (currentStop == null) {
                            // advance prevStop/prevTime to avoid creating invalid edges
                            prevStop = null;
                            prevTime = null;
                            continue;
                        }

                        if (prevStop != null && prevTime != null) {
                            long diffMinutes = Duration.between(prevTime, parsedTime).toMinutes();
                            //System.out.println(prevTime);
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
        public final List<TrainStop> path;

        public Result(int totalMinutes, List<TrainStop> path) {
            this.totalMinutes = totalMinutes;
            this.path = path;
        }

        public Result(int totalDurationMinutes, Object path2) {
            this.path = (List<TrainStop>) path2;
            this.totalMinutes = totalDurationMinutes;
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

        /**
     * Simplified RAPTOR Algorithm for earliest arrival in the TrainGraph.
     * Works in rounds, updating arrival times based on available trips.
     */
    public static class TrainRaptor {

        private final TrainGraph trainGraph;
        private TrainGraph.Result result;

        public TrainRaptor(TrainGraph graph) {
            this.trainGraph = graph;
        }

        /**
         * Runs RAPTOR to compute the earliest arrival path.
         *
         * @param sourceName   starting station name
         * @param targetName   destination station name
         * @param departureTime desired start time
         * @param maxRounds    maximum number of rounds to explore
         * @return TrainGraph.Result object
         */
        public TrainJourney runRaptor(String sourceName, String targetName, LocalTime departureTime, int maxRounds) {
            TrainStop source = trainGraph.getStopByName(sourceName);
            TrainStop target = trainGraph.getStopByName(targetName);

            if (source == null || target == null) {
                result = new TrainGraph.Result(-1, Collections.emptyList());
                return null;
            }

            Map<TrainStop, LocalTime> bestArrival = new HashMap<>();
            for (TrainStop stop : trainGraph.getTrainStops()) {
                bestArrival.put(stop, LocalTime.MAX);
            }
            bestArrival.put(source, departureTime);

            // store previous stop *and* trip
            Map<TrainStop, TrainStop> previousStop = new HashMap<>();
            Map<TrainStop, TrainTrips> previousTrip = new HashMap<>();

            Set<TrainStop> markedStops = new HashSet<>();
            markedStops.add(source);

            for (int round = 0; round < maxRounds; round++) {
                Set<TrainStop> nextMarkedStops = new HashSet<>();

                for (TrainStop marked : markedStops) {
                    for (TrainTrips trip : trainGraph.getOutgoingTrips(marked)) {
                        LocalTime depTime = trip.getDepartureTime();
                        if (depTime.isBefore(bestArrival.get(marked))) continue;

                        LocalTime arrTime = depTime.plusMinutes(trip.getDuration());
                        TrainStop dest = trip.getDestinationTrainStop();

                        if (arrTime.isBefore(bestArrival.get(dest))) {
                            bestArrival.put(dest, arrTime);
                            previousStop.put(dest, marked);
                            previousTrip.put(dest, trip);
                            nextMarkedStops.add(dest);
                        }
                    }
                }

                markedStops = nextMarkedStops;
                if (markedStops.isEmpty()) break;
            }

            LocalTime arrival = bestArrival.get(target);
            if (arrival.equals(LocalTime.MAX)) {
                this.result = new TrainGraph.Result(-1, Collections.emptyList());
                return null;
            }

            // Reconstruct trips
            List<TrainTrips> trips = new ArrayList<>();
            TrainStop step = target;
            while (previousStop.containsKey(step)) {
                TrainTrips trip = previousTrip.get(step);
                trips.add(trip);
                step = previousStop.get(step);
            }
            Collections.reverse(trips);

            // Build Journey
            TrainJourney journey = new TrainJourney(source, target, departureTime, arrival, trips);
            this.result = new TrainGraph.Result((int) journey.getTotalDurationMinutes(), new ArrayList<>(journey.getTrips()));
            return journey;
        }
        
    }

}
