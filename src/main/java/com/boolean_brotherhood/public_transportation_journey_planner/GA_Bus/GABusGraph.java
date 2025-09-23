package com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

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

    public GABusGraph() throws IOException {
        loadStopsFromCSV(STOPS_RESOURCE);
        loadGATrips();
    }

    public void printStops(int limit) {
        for (int i = 0; i < limit && i < totalStops.size(); i++) {
            GAStop stop = totalStops.get(i);
            System.out.println("Stop Code: " + stop.getStopCode());
            System.out.println("Stop Name: " + stop.getName());
            System.out.println("Coordinates: (" + stop.getLatitude() + ", " + stop.getLongitude() + ")");
            System.out.println("Address: " + stop.getAddress());
            System.out.println("Routes: " + stop.getRouteCodes());
            System.out.println("Trips: " + stop.getGATrips().size());
            System.out.println("---------------------------");
        }
    }

    public List<GAStop> getGAStops() {
        return Collections.unmodifiableList(totalStops);
    }

    public List<GATrip> getGATrips() {
        return Collections.unmodifiableList(totalTrips);
    }

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
                    totalStops.add(stop);
                    stopsByCode.put(codeKey, stop);
                    stopsByName.putIfAbsent(normalizeNameKey(displayName), stop);
                    stopsByName.putIfAbsent(normalizeNameKey(stopCode), stop);
                    if (!classification.isEmpty()) {
                        stopsByName.putIfAbsent(normalizeNameKey(classification), stop);
                    }
                }

                if (!routeCode.isEmpty()) {
                    stop.addRouteCode(routeCode);
                }
                if (!routeName.isEmpty()) {
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
                    GAStop from = orderedStops.get(i);
                    GAStop to = orderedStops.get(i + 1);
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

                    totalTrips.add(trip);
                    from.addGATrip(trip);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading GA schedule {0}: {1}", new Object[]{scheduleId, e.getMessage()});
            LOGGER.log(Level.FINEST, "Exception details", e);
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
            LOGGER.log(Level.FINER, "Unknown GA day type '{0}', defaulting to WEEKDAY", raw);
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
                LOGGER.warning("Source or target stop not found: " + sourceName + " -> " + targetName);
                result = new GABusGraph.Result(-1, Collections.emptyList());
                return null;
            }

            Map<GAStop, LocalTime> bestArrival = new HashMap<>();
            for (GAStop stop : GAGraph.getGAStops()) {
                bestArrival.put(stop, LocalTime.MAX);
            }
            bestArrival.put(source, departureTime);

            Map<GAStop, GAStop> previousStop = new HashMap<>();
            Map<GAStop, GATrip> previousTrip = new HashMap<>();

            Set<GAStop> markedStops = new HashSet<>();
            markedStops.add(source);

            for (int round = 0; round < maxRounds; round++) {
                Set<GAStop> nextMarkedStops = new HashSet<>();

                for (GAStop marked : markedStops) {
                    for (GATrip trip : marked.getGATrips()) {
                        LocalTime depTime = trip.getDepartureTime();
                        if (depTime.isBefore(bestArrival.get(marked))) {
                            continue;
                        }

                        LocalTime arrTime = depTime.plusMinutes(trip.getDuration());
                        GAStop dest = (GAStop) trip.getDestinationStop();

                        if (arrTime.isBefore(bestArrival.get(dest))) {
                            bestArrival.put(dest, arrTime);
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

            LocalTime arrival = bestArrival.get(target);
            if (arrival == null || arrival.equals(LocalTime.MAX)) {
                result = new GABusGraph.Result(-1, Collections.emptyList());
                return null;
            }

            List<GATrip> trips = new ArrayList<>();
            GAStop step = target;
            while (previousStop.containsKey(step)) {
                GATrip trip = previousTrip.get(step);
                trips.add(trip);
                step = previousStop.get(step);
            }
            Collections.reverse(trips);

            GABusJourney journey = new GABusJourney(source, target, departureTime, arrival, trips);
            result = new GABusGraph.Result((int) journey.getTotalDurationMinutes(), new ArrayList<>(journey.getTrips()));
            return journey;
        }
    }

    public static void main(String[] args) {
        try {
            GABusGraph graph = new GABusGraph();
            System.out.println("GA graph loaded with " + graph.getGAStops().size() + " stops and "
                    + graph.getGATrips().size() + " trips.");

            GARaptor raptor = new GARaptor(graph);
            String source = "Bellville";
            String target = "Golden Acre";
            LocalTime depTime = LocalTime.of(8, 0);

            System.out.println("--- Running RAPTOR ---");
            GABusJourney journey = raptor.runRaptor(source, target, depTime, 5);

            if (journey == null) {
                System.out.println("No journey found between " + source + " and " + target);
            } else {
                System.out.println("Journey found:");
                System.out.println(journey);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
