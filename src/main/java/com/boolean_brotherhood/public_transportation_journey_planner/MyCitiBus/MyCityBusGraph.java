package com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.MyFileLoader;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;




public class MyCityBusGraph{

    private final List<MyCityStop> totalStops ;
    private final List<MyCityTrip> totalTrips ;
    private final Map<String, MyCityStop> stopLookup = new HashMap<>();
    private final List<String> logs = new ArrayList<>();

    private static final String stopFileName = "CapeTownTransitData/MyCiti_Data/stops_cleaned.csv";
    private static final String myCityRoute = "CapeTownTransitData/MyCiti_Data/myciti-route_summary.csv";
    public static final Logger LOGGER = Logger.getLogger(MyCityBusGraph.class.getName());

        // Metrics
    private long stopsLoadTimeMs;
    private long tripsLoadTimeMs;
    private int malformedStopLines = 0;
    private int missingTripStops = 0;

    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public MyCityBusGraph() throws IOException{
        totalStops = new ArrayList<>();
        totalTrips = new ArrayList<>();
        log("Loading MyCiti stops...");
        long start = System.currentTimeMillis();
        loadStopsFromCSV(stopFileName);
        long duration = System.currentTimeMillis() - start;
        this.stopsLoadTimeMs = duration;
        log("Loaded " + totalStops.size() + " stops in " + duration + " ms");
        start = System.currentTimeMillis();
        
        this.loadMyCityTrips();
        duration = System.currentTimeMillis() - start;
        this.tripsLoadTimeMs = duration;
    }
        


    public void printStops(int thisMany) {
        for (int i = 0; i < thisMany && i < this.totalStops.size(); i++) {
            MyCityStop stop = this.totalStops.get(i);
            System.out.println("Stop Code: " + stop.getStopCode());
            System.out.println("Stop Name: " + stop.getName());
            System.out.println("Coordinates: (" + stop.getLatitude() + ", " + stop.getLongitude() + ")");
            System.out.println("Address: " + stop.getAddress());
            System.out.println("---------------------------");
        }
    }

    public List<MyCityStop> getMyCityStops(){
        return this.totalStops;
    }

    public List<MyCityTrip> getMyCityTrips(){
        return this.totalTrips;
    }

    
        /**
     * Gets the nearest MyCity stop to a given set of coordinates.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return Nearest TrainStop object
     */
    public MyCityStop getNearestMyCityStop(double lat, double lon) {
        double minDist = Double.MAX_VALUE;
        MyCityStop nearest = null;
        for (MyCityStop stop : totalStops) {
            double d = stop.distanceTo(lat, lon);
            if (d < minDist) {
                minDist = d;
                nearest = stop;
            }
        }
        return nearest;
    }


    public void loadMyCityTrips() throws IOException{
        long start = System.currentTimeMillis();
        if(!MyFileLoader.resourceExists(myCityRoute)){return ;}
        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(myCityRoute)) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                String routeCode = parts[0];
                String routeFullName = parts[1];
                Path dir1RoutePath = Paths.get("Public Transportation Journey Planner/src/main/resources/CapeTownTransitData/MyCiti_Data/myciti-bus-schedules/" + routeCode + "-dir1.csv");
                Path dir2RoutePath = Paths.get("Public Transportation Journey Planner/src/main/resources/CapeTownTransitData/MyCiti_Data/myciti-bus-schedules/" + routeCode + "-dir2.csv");

                getTripsInRoute(dir1RoutePath, routeFullName);
                getTripsInRoute(dir2RoutePath, routeFullName);

                }
            }
            tripsLoadTimeMs = System.currentTimeMillis() - start;
    }



    public MyCityStop findStop(String name) {
        return stopLookup.get(name == null ? "" : name.trim().toUpperCase());
    }


    public void getTripsInRoute(Path routePath, String routeFullName) {
        int missingStops = 0;

        if (Files.exists(routePath)) {
            try (BufferedReader br = Files.newBufferedReader(routePath)) {
                String headerLine = br.readLine();
                if (headerLine == null) {
                    return;
                }

                String[] headerList = headerLine.split(",");
                String line;
                int tripCounter = 1;

                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] cols = line.split(",", -1);
                    if (cols.length < 2) continue;

                    String routeId = cols[0].trim();
                    String dayType = (cols.length > 1 && !cols[1].isBlank()) ? cols[1].split(" ")[0] : "";

                    // iterate legs: for each i -> i+1 create a trip from header[i] -> header[i+1]
                    for (int i = 2; i < Math.max(cols.length - 1, 2); i++) {
                        String fromStopName = headerList[i].trim().toUpperCase();
                        String toStopName = headerList[i + 1].trim().toUpperCase();
                        String timeStrFrom = cols[i].trim();
                        String timeStrTo = (i + 1 < cols.length) ? cols[i + 1].trim() : "";

                        if (fromStopName.isEmpty() || toStopName.isEmpty() || timeStrFrom.isEmpty() || timeStrTo.isEmpty()) {
                            missingStops++;
                            continue;
                        }

                        MyCityStop from = findStop(fromStopName);   // CORRECT: from = current header
                        MyCityStop to = findStop(toStopName);       // CORRECT: to = next header

                        if (from == null || to == null) {
                            continue;
                        }

                        try {
                            LocalTime parsedTimeFrom = LocalTime.parse(timeStrFrom, TIME_FORMAT);
                            LocalTime parsedTimeTo = LocalTime.parse(timeStrTo, TIME_FORMAT);

                            long diffMinutes = Duration.between(parsedTimeFrom, parsedTimeTo).toMinutes();
                            int duration;
                            if (diffMinutes < 0) {
                                // handle midnight wrap-around
                                if (Math.abs(diffMinutes) > 12 * 60) {
                                    duration = (int) (diffMinutes + 24 * 60);
                                } else {
                                    duration = (int) Math.abs(diffMinutes);
                                }
                            } else {
                                duration = (int) diffMinutes;
                            }
                            if (duration <= 0) duration = 1;

                            MyCityTrip trip = new MyCityTrip(from, to, Trip.DayType.parseDayType(dayType), parsedTimeFrom);
                            trip.setDuration(duration);
                            trip.setRouteName(routeId);
                            trip.setTripID(routeId + "-T" + tripCounter++);

                            this.totalTrips.add(trip);
                            from.addMyCityTrip(trip); // add outgoing trip to 'from' stop
                        } catch (Exception ex) {
                            LOGGER.log(Level.FINE, "Skipping malformed time values at route {0}, line: {1}", new Object[]{routeId, line});
                        }
                    }
                    for (MyCityStop stop : totalStops) {
                        stop.sortTripsByDeparture();
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error reading CSV file: {0}", e.getMessage());
                LOGGER.log(Level.FINEST, "Exception details", e);
            }
        } else {
            LOGGER.log(Level.WARNING, "Route file does not exist: {0}", routePath);
        }
    }

    
    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("totalStops", (long) totalStops.size());
        metrics.put("totalTrips", (long) totalTrips.size());
        metrics.put("malformedStopLines", (long) malformedStopLines);
        metrics.put("missingTripStops", (long) missingTripStops);
        metrics.put("stopsLoadTimeMs", stopsLoadTimeMs);
        metrics.put("tripsLoadTimeMs", tripsLoadTimeMs);
        return metrics;
    }


    public void loadStopsFromCSV(String filePath) {
        long start = System.currentTimeMillis();
        if(!MyFileLoader.resourceExists(filePath)){return;}
        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(filePath)) {

            String line;
            br.readLine();

            while ((line = br.readLine()) != null) {

                String[] values = line.split(",");
                if (values.length < 5) {

                    continue; // skip this line and move on
                }
                String stopCode = values[0].trim();
                String name = values[1].trim().toUpperCase();


                double xcoord = Double.parseDouble(values[3].trim());
                double ycoord = Double.parseDouble(values[2].trim());


                StringBuilder addressBuilder = new StringBuilder(values[4]);
                for (int i = 5; i < values.length; i++) {
                    addressBuilder.append(" ").append(values[i]);
                }
                String address = addressBuilder.toString();

                MyCityStop stop = new MyCityStop(name, xcoord, ycoord, stopCode, address);
                this.totalStops.add(stop);
                stopLookup.put(name.toUpperCase(), stop);
            }

        } catch (IOException e) {
            // Logs message + full exception (keeps stack trace)
            LOGGER.log(Level.SEVERE, "Error reading CSV file", e);
        } catch (NumberFormatException e) {
            // Use parameterized logging instead of string concatenation
            LOGGER.log(Level.SEVERE, "Invalid number format in CSV: {0}", e.getMessage());
        }
        stopsLoadTimeMs = System.currentTimeMillis() - start;
    }
    public static class Result {
        public final int totalMinutes;
        public final List<MyCityStop> path;

        public Result(int totalMinutes, List<MyCityStop> path) {
            this.totalMinutes = totalMinutes;
            this.path = path;
        }

        public Result(int totalDurationMinutes, Object path2) {
            this.path = (List<MyCityStop>) path2;
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




    public void log(String msg) {
        String entry = "[" + java.time.LocalTime.now() + "] " + msg;
        logs.add(entry);
        System.out.println(entry); // still print to console
    }

    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public static class MyCityRaptor {

        private final MyCityBusGraph myCityGraph;
        private MyCityBusGraph.Result result;

        public MyCityRaptor(MyCityBusGraph graph) {
            this.myCityGraph = graph;
        }

        /**
         * Runs RAPTOR to compute earliest arrival path using integers for minutes.
         *
         * @param sourceName   starting stop
         * @param targetName   destination stop
         * @param departureTime start time
         * @param maxRounds    maximum number of rounds
         * @return MyCityBusJourney object or null if no path found
         */
        public MyCityBusJourney runRaptor(String sourceName, String targetName, LocalTime departureTime, int maxRounds) {
            MyCityStop source = myCityGraph.findStop(sourceName == null ? "" : sourceName.toUpperCase());
            MyCityStop target = myCityGraph.findStop(targetName == null ? "" : targetName.toUpperCase());

            if (source == null || target == null) {
                result = new MyCityBusGraph.Result(-1, Collections.emptyList());
                return null;
            }

            // Convert start time to minutes since midnight
            int departureMinutes = departureTime.getHour() * 60 + departureTime.getMinute();

            // Initialize best arrival times
            Map<MyCityStop, Integer> bestArrival = new HashMap<>();
            for (MyCityStop stop : myCityGraph.getMyCityStops()) {
                bestArrival.put(stop, Integer.MAX_VALUE);
            }
            bestArrival.put(source, departureMinutes);

            // Previous stop and trip for path reconstruction
            Map<MyCityStop, MyCityStop> previousStop = new HashMap<>();
            Map<MyCityStop, MyCityTrip> previousTrip = new HashMap<>();

            Set<MyCityStop> markedStops = new HashSet<>();
            markedStops.add(source);

            for (int round = 0; round < maxRounds; round++) {
                Set<MyCityStop> nextMarkedStops = new HashSet<>();

                for (MyCityStop marked : markedStops) {
                    int arrivalAtMarked = bestArrival.get(marked);
                    if (arrivalAtMarked == Integer.MAX_VALUE) continue;

                    List<MyCityTrip> trips = marked.getMyCityTrips();

                    // Binary search to find first catchable trip
                    int startIdx = 0;
                    int left = 0, right = trips.size() - 1;
                    while (left <= right) {
                        int mid = (left + right) / 2;
                        int tripDep = trips.get(mid).getDepartureTime().getHour() * 60
                                    + trips.get(mid).getDepartureTime().getMinute();
                        if (tripDep < arrivalAtMarked) left = mid + 1;
                        else { startIdx = mid; right = mid - 1; }
                    }

                    for (int i = startIdx; i < trips.size(); i++) {
                        MyCityTrip trip = trips.get(i);
                        int tripDep = trip.getDepartureTime().getHour() * 60
                                    + trip.getDepartureTime().getMinute();
                        if (tripDep < arrivalAtMarked) continue;

                        MyCityStop dest = (MyCityStop) trip.getDestinationStop();
                        int arrTime = tripDep + trip.getDuration();

                        if (arrTime < bestArrival.get(dest)) {
                            bestArrival.put(dest, arrTime);
                            previousStop.put(dest, marked);
                            previousTrip.put(dest, trip);
                            nextMarkedStops.add(dest);
                        }
                    }
                }

                if (nextMarkedStops.isEmpty()) break;
                markedStops = nextMarkedStops;
            }

            int arrivalMinutesTarget = bestArrival.get(target);
            if (arrivalMinutesTarget == Integer.MAX_VALUE) {
                result = new MyCityBusGraph.Result(-1, Collections.emptyList());
                return null;
            }

            // Reconstruct path
            List<MyCityTrip> trips = new ArrayList<>();
            MyCityStop step = target;
            while (!step.equals(source) && previousStop.containsKey(step)) {
                MyCityTrip trip = previousTrip.get(step);
                if (trip == null) break;
                trips.add(trip);
                step = previousStop.get(step);
            }
            Collections.reverse(trips);

            LocalTime arrivalTime = LocalTime.of(arrivalMinutesTarget / 60, arrivalMinutesTarget % 60);
            MyCityBusJourney journey = new MyCityBusJourney(source, target, departureTime, arrivalTime, trips);
            result = new MyCityBusGraph.Result(arrivalMinutesTarget - departureMinutes, new ArrayList<>(trips));

            return journey;
        }
    }



}
