package com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.MyFileLoader;
import com.boolean_brotherhood.public_transportation_journey_planner.SystemLog;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

/**
 * Represents the Golden Arrow (GA) bus transit network.
 * 
 * Responsibilities:
 * - Load bus stops and trips from CSV resource files.
 * - Build stop indices for fast lookup.
 * - Support journey planning using RAPTOR and Connection Scan Algorithm (CSA).
 * - Provide system metrics (stops, trips, load times).
 */
public class GABusGraph {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final String STOPS_RESOURCE = "CapeTownTransitData/GA_Data/ga-bus-stops.csv";
    private static final String SCHEDULE_SUMMARY_RESOURCE = "CapeTownTransitData/GA_Data/bus_schedule-summary.csv";
    private static final String SCHEDULE_DIR_RESOURCE = "CapeTownTransitData/GA_Data/ga-bus-schedules";

    public static final Logger LOGGER = Logger.getLogger(GABusGraph.class.getName());
    private final List<GAStop> totalStops = new ArrayList<>();
    private final List<GATrip> totalTrips = new ArrayList<>();
    private final Map<String, GAStop> stopsByCode = new HashMap<>();
    private final Map<String, GAStop> stopsByName = new HashMap<>();
    private final Map<GAStop, Set<String>> stopTokenCache = new HashMap<>();
    private long stopsLoadTimeMs = 0;
    private long tripsLoadTimeMs = 0;
    private final List<Connection> connections = new ArrayList<>(); // all connections sorted by departure time
    private final Map<GAStop, Integer> stopToId = new HashMap<>(); // GAStop -> integer id
    private final List<GAStop> idToStop = new ArrayList<>(); // index -> GAStop

    static {
        try {
            FileHandler fileHandler = new FileHandler("app.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads stop data and trip schedules from CSV resources,
     * builds stop indices, and prepares connections for CSA queries.
     *
     * @throws IOException if any resource file cannot be read
     */

    public GABusGraph() throws IOException {
        long p1 = System.currentTimeMillis();
        loadStopsFromCSV(STOPS_RESOURCE);
        long p2 = System.currentTimeMillis();
        loadGATrips();
        long p3 = System.currentTimeMillis();
        stopsLoadTimeMs = p2-p1;
        tripsLoadTimeMs = p3-p2;

        // Build indices & connections for CSA
        buildStopIndex();
        buildConnectionsFromTrips();

    }

    /**
     * Returns metrics about the loaded graph including total stops,
     * total trips, and load times in milliseconds.
     *
     * @return map of metric name → value
     */
    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("totalStops", (long) totalStops.size());
        metrics.put("totalTrips", (long) totalTrips.size());
        metrics.put("stopsLoadTimeMs", stopsLoadTimeMs);
        metrics.put("tripsLoadTimeMs", tripsLoadTimeMs);
        return metrics;
    }

    /**
     * @return an unmodifiable list of all GA stops in the network
     */
    public List<GAStop> getGAStops() {
        return Collections.unmodifiableList(totalStops);
    }

    /**
     * @return an unmodifiable list of all GA trips (legs) in the network
     */
    public List<GATrip> getGATrips() {
        return Collections.unmodifiableList(totalTrips);
    }

    /**
     * Finds the GA stop nearest to the given latitude and longitude.
     *
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     * @return nearest GAStop or null if no stops exist
     */
    public GAStop getNearestGAStop(double lat, double lon) {
        double minDist = Double.MAX_VALUE;
        GAStop nearest = null;
        for (GAStop stop : totalStops) {
            double d = stop.distanceTo(lat, lon);
            if (d < minDist) {
                minDist = d;
                nearest = stop;
            }
        }
        return nearest;
    }

    /**
     * Attempts to find a stop by its label (code, name, or classification).
     *
     * @param label the stop name or code
     * @return a GAStop if resolved, or null otherwise
     */
    public GAStop findStop(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String codeKey = label.trim().toUpperCase(Locale.ROOT);
        GAStop byCode = stopsByCode.get(codeKey);
        if (byCode != null) {
            return byCode;
        }
        return resolveStop(label);
    }

    private void loadStopsFromCSV(String resourcePath) throws IOException {
        BufferedReader reader = MyFileLoader.getBufferedReaderFromResource(resourcePath);
        if (reader == null) {
            throw new IOException("Unable to open GA stops resource: " + resourcePath);
        }

        try (BufferedReader br = reader) {
            String header = br.readLine();
            if (header == null) {
                LOGGER.warning("GA stops resource is empty");
                return;
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 9) {
                    continue;
                }

                String routeCode = parts[1].trim();
                String routeName = parts[2].trim();
                String stopCode = parts[3].trim();
                String description = parts[4].trim();
                String classification = parts[5].trim();
                double lon = parseDouble(parts[7]);
                double lat = parseDouble(parts[8]);

                if (stopCode.isEmpty() || Double.isNaN(lat) || Double.isNaN(lon)) {
                    continue;
                }

                String codeKey = stopCode.toUpperCase(Locale.ROOT);
                GAStop stop = stopsByCode.get(codeKey);
                if (stop == null) {
                    String displayName = description.isEmpty() ? stopCode : description;
                    String address = description.isEmpty() ? classification : description;
                    stop = new GAStop(displayName, lat, lon, stopCode, address);
                    SystemLog.add_stop(stop); // ------------------------------------------------------------- LOG
                    totalStops.add(stop);
                    stopsByCode.put(codeKey, stop);
                    stopsByName.putIfAbsent(normalizeNameKey(displayName), stop);
                    stopsByName.putIfAbsent(normalizeNameKey(stopCode), stop);
                    if (!classification.isEmpty()) {
                        if (classification.toUpperCase().equals("STATION")){
                            SystemLog.add_stations(classification, "BUS");// ---------------------------- LOG
                        }
                        stopsByName.putIfAbsent(normalizeNameKey(classification), stop);
                    }
                }

                if (!routeCode.isEmpty()) {
                    stop.addRouteCode(routeCode);
                }
                if (!routeName.isEmpty()) {
                    SystemLog.add_active_route(routeName); // ------------------------------------------------ LOG
                    stop.addRouteCode(routeName);
                }
            }
        }
    }

    private void loadGATrips() throws IOException {
        BufferedReader reader = MyFileLoader.getBufferedReaderFromResource(SCHEDULE_SUMMARY_RESOURCE);
        if (reader == null) {
            throw new IOException("Unable to open GA schedule summary: " + SCHEDULE_SUMMARY_RESOURCE);
        }

        Map<String, String> scheduleDescriptions = new HashMap<>();

        try (BufferedReader br = reader) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    continue;
                }
                String scheduleNo = parts[0].trim();
                String direction = parts[1].trim();
                if (scheduleNo.isEmpty() || direction.isEmpty()) {
                    continue;
                }
                String scheduleId = scheduleNo + "-" + direction;
                String description = buildRouteDescription(scheduleId, parts);
                scheduleDescriptions.put(scheduleId, description);
            }
        }

        for (Map.Entry<String, String> entry : scheduleDescriptions.entrySet()) {
            loadTripsForSchedule(entry.getKey(), entry.getValue());
        }

        totalStops.forEach(GAStop::sortTripsByDeparture);
    }

    private void loadTripsForSchedule(String scheduleId, String routeDescription) {
        String resourcePath = SCHEDULE_DIR_RESOURCE + "/" + scheduleId + ".csv";
        BufferedReader reader = MyFileLoader.getBufferedReaderFromResource(resourcePath);
        if (reader == null) {
            LOGGER.log(Level.FINE, "Schedule resource not found: {0}", resourcePath);
            return;
        }
        Map<GATrip,Integer> tripCountMap = new HashMap<>();
        try (BufferedReader br = reader) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return;
            }

            String[] headerStops = headerLine.split(",", -1);
            String line;
            int tripCounter = 1;
            

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                if (values.length < 2) {
                    continue;
                }

                Trip.DayType dayType = parseDayType(values[0]);
                List<GAStop> orderedStops = new ArrayList<>();
                List<LocalTime> orderedTimes = new ArrayList<>();

                for (int i = 1; i < headerStops.length && i < values.length; i++) {
                    String rawTime = values[i].trim();
                    if (rawTime.isEmpty() || rawTime.equalsIgnoreCase("via")) {
                        continue;
                    }

                    LocalTime departureTime = parseTime(rawTime);
                    if (departureTime == null) {
                        continue;
                    }

                    GAStop stop = resolveStop(headerStops[i].trim());
                    if (stop == null) {
                        continue;
                    }

                    orderedStops.add(stop);
                    orderedTimes.add(departureTime);
                }


                if (orderedStops.size() < 2) {
                    continue;
                }

                
                String baseTripId = scheduleId + "-T" + tripCounter++;
                for (int i = 0; i < orderedStops.size() - 1; i++) {
                    GAStop from = orderedStops.get(i); //this stop
                    GAStop to = orderedStops.get(i + 1); //next stop
                    //Skip self-loops
                    if (from.equals(to)) {
                        LOGGER.warning("Skipping self-loop at stop: " + from.getName());
                        continue;
                    }
                    LocalTime dep = orderedTimes.get(i);
                    LocalTime arr = orderedTimes.get(i + 1);

                    long minutes = Duration.between(dep, arr).toMinutes();
                    if (minutes < 0) {
                        minutes += 24 * 60;
                    }
                    if (minutes <= 0) {
                        minutes = 1;
                    }

                    GATrip trip = new GATrip(from, to, dayType, dep);
                    trip.setDuration((int) minutes);
                    trip.setTripID(baseTripId + "-S" + (i + 1));
                    trip.setRouteName(routeDescription);

                    
                    tripCountMap.put(trip,tripCountMap.getOrDefault(trip,0)+1);    
                    from.addGATrip(trip);
                    //System.out.println(from.toString()+"Has added trip "+from.getGATrips().size());
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading GA schedule {0}: {1}", new Object[]{scheduleId, e.getMessage()});
            LOGGER.log(Level.FINEST, "Exception details", e);
        }

        
    for (GATrip trip : tripCountMap.keySet()) {
            totalTrips.add(trip);
        }
    }

    private String buildRouteDescription(String scheduleId, String[] parts) {
        List<String> destinations = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            String value = parts[i].trim();
            if (!value.isEmpty()) {
                destinations.add(value);
            }
        }
        if (destinations.isEmpty()) {
            return "Schedule " + scheduleId;
        }
        return String.join(" - ", destinations);
    }

    private Trip.DayType parseDayType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Trip.DayType.WEEKDAY;
        }
        try {
            return Trip.DayType.parseDayType(raw);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.FINER, "Unknown GA day type"+raw+", defaulting to WEEKDAY", raw);
            return Trip.DayType.WEEKDAY;
        }
    }

    private LocalTime parseTime(String raw) {
        String candidate = raw.replace('.', ':');
        try {
            return LocalTime.parse(candidate, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            LOGGER.log(Level.FINER, "Skipping unparsable time '{0}'", raw);
            return null;
        }
    }

    private double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private GAStop resolveStop(String rawLabel) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return null;
        }
        String normalized = normalizeNameKey(rawLabel);
        GAStop direct = stopsByName.get(normalized);
        if (direct != null) {
            return direct;
        }

        List<String> tokens = tokenize(rawLabel);
        if (tokens.isEmpty()) {
            return null;
        }

        GAStop bestMatch = null;
        int bestScore = 0;

        for (GAStop stop : totalStops) {
            Set<String> stopTokens = stopTokenCache.computeIfAbsent(stop, this::tokensForStop);
            int score = matchScore(tokens, stopTokens);
            if (score == tokens.size()) {
                return stop;
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = stop;
            }
        }

        return bestScore > 0 ? bestMatch : null;
    }

    private String normalizeNameKey(String raw) {
        return String.join(" ", tokenize(raw));
    }

    private Set<String> tokensForStop(GAStop stop) {
        Set<String> tokens = new HashSet<>(tokenize(stop.getName()));
        tokens.addAll(tokenize(stop.getAddress()));
        tokens.addAll(tokenize(stop.getStopCode()));
        for (String route : stop.getRouteCodes()) {
            tokens.addAll(tokenize(route));
        }
        return tokens;
    }

    private int matchScore(List<String> targetTokens, Set<String> stopTokens) {
        int score = 0;
        for (String token : targetTokens) {
            if (stopTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private List<String> tokenize(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String upper = raw.toUpperCase(Locale.ROOT).replace('&', ' ');
        String[] parts = upper.split("[^A-Z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            tokens.add(canonicalizeToken(part));
        }
        return tokens;
    }

    private String canonicalizeToken(String token) {
        switch (token) {
            case "RD":
            case "ROAD":
                return "ROAD";
            case "DRV":
            case "DRIVE":
            case "DR":
                return "DRIVE";
            case "ST":
            case "STREET":
            case "STR":
                return "STREET";
            case "STN":
            case "STATION":
                return "STATION";
            case "CTR":
            case "CNTR":
            case "CENTRE":
                return "CENTRE";
            case "AVE":
            case "AVENUE":
                return "AVENUE";
            case "PK":
            case "PARK":
                return "PARK";
            case "PKY":
            case "PKWY":
            case "PARKWAY":
                return "PARKWAY";
            default:
                return token;
        }
    }

    public static class Result {
        public final int totalMinutes;
        public final List<GATrip> trips;

        public Result(int totalMinutes, List<GATrip> trips) {
            this.totalMinutes = totalMinutes;
            this.trips = Collections.unmodifiableList(new ArrayList<>(trips));
        }

        public boolean hasJourney() {
            return totalMinutes >= 0 && !trips.isEmpty();
        }

        public List<GATrip> getTrips() {
            return trips;
        }

        @Override
        public String toString() {
            if (!hasJourney()) {
                return "No valid path found.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Total travel time: ").append(totalMinutes).append(" min");
            sb.append("Segments: ");
            for (int i = 0; i < trips.size(); i++) {
                GATrip trip = trips.get(i);
                if (i > 0) {
                    sb.append(" -> ");
                }
                sb.append(trip.getDepartureStop().getName())
                  .append(" to ")
                  .append(trip.getDestinationStop().getName());
            }
            return sb.toString();
        }
    }

    /**
     * Represents a single connection (trip segment) for the CSA algorithm.
     * A connection is a directed edge between two stops at specific times.
     */

    private static class Connection implements Comparable<Connection> {
        final int fromId;
        final int toId;
        final int dep; // departure minutes since midnight (0.. perhaps >1440 after adjustments)
        final int arr; // arrival minutes (>= dep)
        final GATrip trip;

        Connection(int fromId, int toId, int dep, int arr, GATrip trip) {
            this.fromId = fromId;
            this.toId = toId;
            this.dep = dep;
            this.arr = arr;
            this.trip = trip;
        }

        @Override
        public int compareTo(Connection o) {
            return Integer.compare(this.dep, o.dep);
        }
    }

    /**
     * Build stop index (stopToId & idToStop). Called once after loading stops.
     */
    private void buildStopIndex() {
        stopToId.clear();
        idToStop.clear();
        for (int i = 0; i < totalStops.size(); i++) {
            GAStop s = totalStops.get(i);
            stopToId.put(s, i);
            idToStop.add(s);
        }
    }

    /**
     * Build connection list from totalTrips. Each GATrip corresponds to a connection (leg).
     * The departure time is minutes since midnight; if arrival < departure we assume next day
     * and add 1440 minutes to arrival so arr >= dep.
     */
    private void buildConnectionsFromTrips() {
        connections.clear();

        for (GATrip t : totalTrips) {
            GAStop from = t.getDepartureGAStop();
            GAStop to = t.getDestinationGAStop();

            Integer fromId = stopToId.get(from);
            Integer toId = stopToId.get(to);
            if (fromId == null || toId == null) {
                // Shouldn't happen normally — but skip if mapping missing
                continue;
            }

            LocalTime depTime = t.getDepartureTime();
            if (depTime == null) continue;
            int depMinutes = depTime.getHour() * 60 + depTime.getMinute();
            int arrMinutes = depMinutes + t.getDuration();
            // If arrival <= departure assume it crossed midnight and add 24h to arrival (so arr >= dep)
            if (arrMinutes <= depMinutes) {
                arrMinutes += 24 * 60;
            }

            connections.add(new Connection(fromId, toId, depMinutes, arrMinutes, t));
        }

        // Sort connections by departure time (ascending)
        Collections.sort(connections);
    }


    /**
     * Binary search lower bound into connections list given a departure minute.
     */
    private int lowerBoundConnections(int depMinutes) {
        int left = 0;
        int right = connections.size();
        while (left < right) {
            int mid = (left + right) >>> 1;
            if (connections.get(mid).dep < depMinutes) left = mid + 1;
            else right = mid;
        }
        return left;
    }

    /**
     * Public CSA query method.
     * Returns a GABusJourney (same type used by your RAPTOR) or null if no path found.
     *
     * NOTE: This CSA assumes single-day timetable; it handles per-leg midnight wrapping by
     * adding 24h to arrival when arrival <= departure. Queries are performed by using
     * minutes since midnight for the requested departure time.
     *
     * 
     * Runs the Connection Scan Algorithm (CSA) to find the earliest
     * arrival journey between two stops.
     *
     * @param sourceName name of the starting stop
     * @param targetName name of the destination stop
     * @param departureTime requested departure time
     * @return a GABusJourney if found, null otherwise
     * 
     */
    public GABusJourney runConnectionScan(String sourceName, String targetName, LocalTime departureTime) {
        if (stopToId.isEmpty()) {
            buildStopIndex();
        }
        if (connections.isEmpty()) {
            buildConnectionsFromTrips();
        }

        GAStop source = findStop(sourceName);
        GAStop target = findStop(targetName);
        if (source == null || target == null) {
            LOGGER.warning("Source or target stop not found for CSA: " + sourceName + " -> " + targetName);
            return null;
        }

        Integer srcId = stopToId.get(source);
        Integer tgtId = stopToId.get(target);
        if (srcId == null || tgtId == null) {
            LOGGER.warning("Source/target id missing for CSA: " + sourceName + " -> " + targetName);
            return null;
        }

        final int nStops = idToStop.size();
        final int INF = Integer.MAX_VALUE / 4;
        int[] best = new int[nStops];
        int[] prevConn = new int[nStops];
        Arrays.fill(best, INF);
        Arrays.fill(prevConn, -1);

        int departureMinutes = departureTime.getHour() * 60 + departureTime.getMinute();
        best[srcId] = departureMinutes;

        // binary search where to start scanning connections
        int startIdx = lowerBoundConnections(departureMinutes);

        int bestTarget = INF;

        for (int i = startIdx; i < connections.size(); i++) {
            Connection c = connections.get(i);

            // pruning: if the departure time of this connection is already after the best known arrival to target,
            // no later connection can improve the target :-)
            if (c.dep > bestTarget) {
                break;
            }

            // can we catch this connection?
            if (c.dep < best[c.fromId]) {
                continue;
            }

            if (c.arr < best[c.toId]) {
                best[c.toId] = c.arr;
                prevConn[c.toId] = i;
                if (c.toId == tgtId) {
                    if (c.arr < bestTarget) {
                        bestTarget = c.arr;
                    }
                }
            }
        }

        if (best[tgtId] == INF) {
            return null;
        }

        // reconstruct connection chain -> produce list of GATrips
        List<GATrip> path = new ArrayList<>();
        int curStop = tgtId;
        Set<Integer> seenStops = new HashSet<>(); // safety to avoid cycles (shouldn't occur)
        while (curStop != srcId && prevConn[curStop] != -1 && !seenStops.contains(curStop)) {
            seenStops.add(curStop);
            Connection used = connections.get(prevConn[curStop]);
            path.add(used.trip);
            curStop = used.fromId;
        }
        Collections.reverse(path);

        // arrival time as LocalTime (modulo 24h)
        int arrivalMinute = best[tgtId];
        int arrivalMinuteMod = arrivalMinute % (24 * 60);
        LocalTime arrivalLocal = LocalTime.of(arrivalMinuteMod / 60, arrivalMinuteMod % 60);

        // build and return a GABusJourney (same constructor signature your code expects)
        GABusJourney journey = new GABusJourney(source, target, departureTime, arrivalLocal, path);
        return journey;
    }


    /**
     * RAPTOR algorithm implementation for GA bus network.
     * RAPTOR works in rounds, updating arrival times by scanning routes.
     */
    public static class GARaptor {

        private final GABusGraph GAGraph;
        private GABusGraph.Result result;

        public GARaptor(GABusGraph graph) {
            this.GAGraph = graph;
        }

        public GABusJourney runRaptor(String sourceName, String targetName, LocalTime departureTime, int maxRounds) {
            GAStop source = GAGraph.findStop(sourceName);
            GAStop target = GAGraph.findStop(targetName);

            if (source == null || target == null) {
                LOGGER.log(Level.WARNING, "Source or target stop not found: {0} -> {1}", new Object[]{sourceName, targetName});
                this.result = new GABusGraph.Result(-1, Collections.emptyList());
                return null;
            }

            Map<GAStop, Integer> bestArrival = new HashMap<>();
            for (GAStop stop : GAGraph.getGAStops()) {
                bestArrival.put(stop, Integer.MAX_VALUE);
            }
            bestArrival.put(source, 0);

            Map<GAStop, GAStop> previousStop = new HashMap<>();
            Map<GAStop, GATrip> previousTrip = new HashMap<>();

            Set<GAStop> markedStops = new HashSet<>();
            markedStops.add(source);

            for (int round = 0; round < maxRounds; round++) {
                Set<GAStop> nextMarkedStops = new HashSet<>();

                for (GAStop marked : markedStops) {
                    for (GATrip trip : marked.getGATrips()) {
                        LocalTime depTime = trip.getDepartureTime();
                        int tripDepMinutes = getMinutesSinceStart(departureTime, depTime);

                        if (tripDepMinutes < bestArrival.get(marked)) {
                            continue;
                        }

                        int arrMinutes = tripDepMinutes + trip.getDuration();
                        GAStop dest = (GAStop) trip.getDestinationStop();

                        if (arrMinutes < bestArrival.get(dest)) {
                            bestArrival.put(dest, arrMinutes);
                            previousStop.put(dest, marked);
                            previousTrip.put(dest, trip);
                            nextMarkedStops.add(dest);
                        }
                    }
                }

                if (nextMarkedStops.isEmpty()) {
                    break;
                }
                markedStops = nextMarkedStops;
            }

            Integer arrivalMinutes = bestArrival.get(target);
            if (arrivalMinutes == null || arrivalMinutes == Integer.MAX_VALUE) {
                result = new GABusGraph.Result(-1, Collections.emptyList());
                return null;
            }
            LocalTime arrival = departureTime.plusMinutes(arrivalMinutes);
            List<GATrip> trips = new ArrayList<>();
            GAStop step = target;
            Set<GAStop> visited = new HashSet<>();
            while (previousStop.containsKey(step) && !visited.contains(step)) {
                visited.add(step);
                GATrip trip = previousTrip.get(step);
                trips.add(trip);
                step = previousStop.get(step);
            }

            Collections.reverse(trips);

            GABusJourney journey = new GABusJourney(source, target, departureTime, arrival, trips);
            result = new GABusGraph.Result((int) journey.getTotalDurationMinutes(), new ArrayList<>(journey.getTrips()));
            return journey;
        }

        private int getMinutesSinceStart(final LocalTime startTime,final LocalTime currentTime) {
            if (currentTime.isBefore(startTime)) {
                // Assume next day
                return (int) Duration.between(startTime, LocalTime.MAX).toMinutes() + 
                    (int) Duration.between(LocalTime.MIN, currentTime).toMinutes() + 1;
            }
            return (int) Duration.between(startTime, currentTime).toMinutes();
         }
    }
    
    public static void main(String[] args) {
        try {
            GABusGraph graph = new GABusGraph();
            System.out.println("GA graph loaded with " + graph.getGAStops().size() + " stops and "
                    + graph.getGATrips().size() + " trips.");

            GARaptor raptor = new GARaptor(graph);
            String source = "CAPE TOWN";
            String target = "BELLVILLE";
            LocalTime depTime = LocalTime.of(15, 30);

            System.out.println("--- Running RAPTOR ---");
            GABusJourney journey = raptor.runRaptor(source, target, depTime, 5);
            if (journey == null) {
                System.out.println("No journey found between " + source + " and " + target);
            } else {
                System.out.println("RAPTOR journey found:");
                System.out.println(journey);
            }

            // Example run of CSA:
            System.out.println("--- Running CSA (Connection Scan) ---");
            GABusJourney csaJourney = graph.runConnectionScan(source, target, depTime);
            if (csaJourney == null) {
                System.out.println("CSA: No journey found between " + source + " and " + target);
            } else {
                System.out.println("CSA journey found:");
                System.out.println(csaJourney);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
