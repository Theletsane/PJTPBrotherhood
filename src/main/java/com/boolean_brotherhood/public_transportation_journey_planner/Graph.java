package com.boolean_brotherhood.public_transportation_journey_planner;

import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemLog;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Unified multimodal graph that merges taxi, train, and bus networks and exposes
 * multimodal RAPTOR journey planning.
 */
public class Graph {

    private static final double WALKING_DISTANCE_THRESHOLD_KM = 0.5;  // 500 metres
    private static final double WALKING_SPEED_KM_PER_MIN = 0.0833;     // ~5 km/h
    private static final int MINUTES_PER_DAY = 24 * 60;

    public enum Mode { TRAIN, MYCITI, GA, TAXI, WALKING }

    private final List<Stop> totalStops = new ArrayList<>();
    private final List<Trip> totalTrips = new ArrayList<>();

    private final TaxiGraph taxiGraph;
    private final TrainGraph trainGraph;
    private final MyCitiBusGraph myCitiBusGraph;
    private final GABusGraph gaBusGraph;
    private final BusGraph busGraph ;

    public Graph() throws IOException {
        this.taxiGraph = new TaxiGraph();
        this.trainGraph = new TrainGraph();
        this.myCitiBusGraph = new MyCitiBusGraph();
        this.gaBusGraph = new GABusGraph();
        this.busGraph = new BusGraph(myCitiBusGraph, gaBusGraph);
    }

    public void loadGraphData() throws IOException {
        long start = System.currentTimeMillis();
        taxiGraph.loadData();
        trainGraph.loadTrainStops();
        trainGraph.LoadTrainTrips();
        long elapsed = System.currentTimeMillis() - start;

        SystemLog.log_event("GRAPH", "Loaded base graph data", "INFO", Map.of(
                "elapsedMs", elapsed,
                "taxiStops", taxiGraph.getTaxiStops().size(),
                "taxiTrips", taxiGraph.getTaxiTrips().size(),
                "trainStops", trainGraph.getTrainStops().size(),
                "trainTrips", trainGraph.getTrainTrips().size(),
                "myCiTiStops", myCitiBusGraph.getMyCitiStops().size(),
                "myCiTiTrips", myCitiBusGraph.getMyCitiTrips().size(),
                "gaStops", gaBusGraph.getGAStops().size(),
                "gaTrips", gaBusGraph.getGATrips().size()
        ));
    }

    private void addTrips(List<? extends Trip> trips) {
        for (Trip trip : trips) {
            if (trip == null) {
                continue;
            }
            // Remove the problematic contains check that was causing ClassCastException
            // Instead, just add the trip (duplicates are unlikely with proper data loading)
            totalTrips.add(trip);
        }
    }


    public BusGraph getBusGraph() {
        return this.busGraph;
    }

    public void buildCombinedGraph() {
        totalStops.clear();
        totalTrips.clear();

        addStops(trainGraph.getTrainStops());
        addStops(myCitiBusGraph.getMyCitiStops());
        addStops(gaBusGraph.getGAStops());

  
        addTrips(trainGraph.getTrainTrips());
        addTrips(myCitiBusGraph.getMyCitiTrips());
        addTrips(gaBusGraph.getGATrips());

        ensureStopTripLinks();
        addWalkingTransfers();

        SystemLog.log_event("GRAPH", "Combined graph built", "INFO", Map.of(
                "totalStops", totalStops.size(),
                "totalTrips", totalTrips.size()
        ));
    }

    private void addStops(List<? extends Stop> stops) {
        for (Stop stop : stops) {
            if (stop == null) {
                continue;
            }
            if (totalStops.contains(stop)) {
                continue;
            }
            totalStops.add(stop);
            SystemLog.add_stop(stop);
        }
    }


    private void ensureStopTripLinks() {
        for (Trip trip : new ArrayList<>(totalTrips)) {
            Stop departure = trip.getDepartureStop();
            if (departure != null) {
                departure.addTrip(trip);
            }
        }
    }

    private void addWalkingTransfers() {
        Set<String> added = new HashSet<>();
        int walkingConnections = 0;
        int maxWalkingConnections = 10; // limit per stop

        for (Stop from : totalStops) {
            // find up to 10 nearby stops within threshold
            List<Stop> nearbyStops = findNearbyStops(from, WALKING_DISTANCE_THRESHOLD_KM, maxWalkingConnections);

            for (Stop to : nearbyStops) {
                if (from.equals(to)) continue;

                double distanceKm = from.distanceTo(to);
                int durationMinutes = Math.max(1, (int) Math.round(distanceKm / WALKING_SPEED_KM_PER_MIN));

                Trip walkway = new Trip(from, to, durationMinutes, Trip.DayType.WEEKDAY);
                walkway.setMode("Walking");
                Trip reverse = new Trip(to, from, durationMinutes, Trip.DayType.WEEKDAY);
                reverse.setMode("Walking");

                String forwardKey = from.getName() + "->" + to.getName();
                String reverseKey = to.getName() + "->" + from.getName();

                if (added.add(forwardKey)) {
                    totalTrips.add(walkway);
                    from.addTrip(walkway);
                    walkingConnections++;
                }
                if (added.add(reverseKey)) {
                    totalTrips.add(reverse);
                    to.addTrip(reverse);
                    walkingConnections++;
                }
            }
        }

        SystemLog.log_event("GRAPH", "Walking transfers prepared", "DEBUG", Map.of(
                "connections", walkingConnections
        ));
    }
    

    private List<Stop> findNearbyStops(Stop from, double maxDistanceKm, int maxResults) {
            List<Stop> nearby = new ArrayList<>();
            for (Stop candidate : totalStops) {
                if (candidate.equals(from)) continue;
                double distance = from.distanceTo(candidate);
                if (distance <= maxDistanceKm) {
                    nearby.add(candidate);
                }
            }
            // Sort by distance and limit
            nearby.sort((a, b) -> Double.compare(from.distanceTo(a), from.distanceTo(b)));
            if (nearby.size() > maxResults) {
                return nearby.subList(0, maxResults);
            }
            return nearby;
        }

    public List<Stop> getStops() {
        // The current implementation has a logic error - it's adding duplicates
        // Here's the corrected version:
        List<Stop> sortedStops = new ArrayList<>(totalStops);
        sortedStops.sort(Comparator.comparing(Stop::getName, String.CASE_INSENSITIVE_ORDER));   
        return Collections.unmodifiableList(sortedStops);
    }

    // Alternative implementation if you want to remove duplicates by name:
    /*
    public List<Stop> getStops() {
        Set<String> seenNames = new HashSet<>();
        List<Stop> uniqueStops = new ArrayList<>();
        
        for (Stop stop : totalStops) {
            if (stop != null && stop.getName() != null && seenNames.add(stop.getName().toUpperCase())) {
                uniqueStops.add(stop);
            }
        }
        
        uniqueStops.sort(Comparator.comparing(Stop::getName, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(uniqueStops);
    }
*/
    public List<Trip> getTrips() {
        return Collections.unmodifiableList(totalTrips);
    }

    public Raptor buildRaptorForModes(EnumSet<Mode> modes) {
        // Default: if no modes provided, allow all
        EnumSet<Mode> requested = (modes == null || modes.isEmpty())
                ? EnumSet.allOf(Mode.class)
                : EnumSet.copyOf(modes);

        // Only add walking if multimodal AND user hasn't explicitly excluded it
        if (requested.size() > 1 && (modes == null || !modes.contains(Mode.WALKING))) {
            requested.add(Mode.WALKING);
        }

        List<Trip> filteredTrips = new ArrayList<>();
        for (Trip trip : totalTrips) {
            if (isTripAllowed(trip, requested)) {
                filteredTrips.add(trip);
            }
        }

        List<Stop> filteredStops = collectStops(filteredTrips);
        return new Raptor(filteredStops, filteredTrips);
    }

    public List<Trip> runRaptor(EnumSet<Mode> modes, String from, String to, LocalTime departure, int maxRounds, Trip.DayType dayType) {
        Stop source = findStopByName(from);
        Stop target = findStopByName(to);
        if (source == null || target == null) {
            return Collections.emptyList();
        }
        Raptor raptor = buildRaptorForModes(modes);
        return raptor.compute(source, target, departure, maxRounds, dayType);
    }

    public List<Trip> runTrainAndMyCitiRaptor(String from, String to, LocalTime departure, int maxRounds, Trip.DayType dayType) {
        return runRaptor(EnumSet.of(Mode.TRAIN, Mode.MYCITI, Mode.WALKING), from, to, departure, maxRounds, dayType);
    }

    public List<Trip> runTrainAndGaRaptor(String from, String to, LocalTime departure, int maxRounds, Trip.DayType dayType) {
        return runRaptor(EnumSet.of(Mode.TRAIN, Mode.GA, Mode.WALKING), from, to, departure, maxRounds, dayType);
    }

    public List<Trip> runAllModesRaptor(String from, String to, LocalTime departure, int maxRounds, Trip.DayType dayType) {
        return runRaptor(EnumSet.of(Mode.TRAIN, Mode.MYCITI, Mode.GA, Mode.WALKING), from, to, departure, maxRounds, dayType);
    }

    public Stop findStopByName(String name) {
        if (name == null) {
            return null;
        }
        String want = name.trim().toUpperCase();
        for (Stop stop : totalStops) {
            if (stop.getName() != null && stop.getName().trim().toUpperCase().equals(want)) {
                return stop;
            }
        }
        return null;
    }

    private List<Stop> collectStops(List<Trip> trips) {
        LinkedHashSet<Stop> set = new LinkedHashSet<>();
        for (Trip trip : trips) {
            if (trip.getDepartureStop() != null) {
                set.add(trip.getDepartureStop());
            }
            if (trip.getDestinationStop() != null) {
                set.add(trip.getDestinationStop());
            }
        }
        return new ArrayList<>(set);
    }

    private boolean isTripAllowed(Trip trip, EnumSet<Mode> modes) {
        if (trip == null) {
            return false;
        }
        if (trip instanceof TrainTrips) {
            return modes.contains(Mode.TRAIN);
        }
        if (trip instanceof MyCitiTrip) {
            return modes.contains(Mode.MYCITI);
        }
        if (trip instanceof GATrip) {
            return modes.contains(Mode.GA);
        }
        String mode = trip.getMode();
        if (mode != null && mode.equalsIgnoreCase("Walking")) {
            return modes.contains(Mode.WALKING);
        }
        return false;
    }

    private static boolean isTripOnRequestedDay(Trip trip, Trip.DayType requested) {
        Trip.DayType tripDay = trip.getDayType();
        if (requested == null || tripDay == null) {
            return true;
        }
        return tripDay == requested;
    }

    private static int toMinutes(LocalTime time) {
        if (time == null) {
            return 0;
        }
        return time.getHour() * 60 + time.getMinute();
    }

    public class Raptor {
        private final List<Stop> stops;
        private final Map<Stop, List<Trip>> outgoing;

        public Raptor(List<Stop> stops, List<Trip> trips) {
            this.stops = new ArrayList<>(Objects.requireNonNullElse(stops, List.of()));
            this.outgoing = new HashMap<>();
            for (Trip trip : trips) {
                Stop departure = trip.getDepartureStop();
                if (departure == null) {
                    continue;
                }
                outgoing.computeIfAbsent(departure, key -> new ArrayList<>()).add(trip);
            }
            for (List<Trip> legs : outgoing.values()) {
                legs.sort(Comparator.comparing(t -> t.getDepartureTime() == null ? LocalTime.MIN : t.getDepartureTime()));
            }
        }

        public List<Trip> compute(Stop source, Stop target, LocalTime departureTime, int maxRounds) {
            return compute(source, target, departureTime, maxRounds, null);
        }

        public List<Trip> compute(Stop source, Stop target, LocalTime departureTime, int maxRounds, Trip.DayType dayType) {
            if (source == null || target == null) {
                return Collections.emptyList();
            }
            if (!outgoing.containsKey(source) && !source.equals(target)) {
                return Collections.emptyList();
            }

            LocalTime anchor = departureTime == null ? LocalTime.MIN : departureTime;
            int anchorMinutes = toMinutes(anchor);

            Map<Stop, Integer> bestArrival = new HashMap<>();
            Map<Stop, Trip> parentTrip = new HashMap<>();
            Map<Stop, Stop> parentStop = new HashMap<>();

            for (Stop stop : stops) {
                bestArrival.put(stop, Integer.MAX_VALUE);
            }
            bestArrival.put(source, anchorMinutes);

            Set<Stop> marked = new HashSet<>();
            marked.add(source);

            int rounds = Math.max(1, maxRounds);
            for (int round = 0; round < rounds && !marked.isEmpty(); round++) {
                Set<Stop> nextMarked = new HashSet<>();

                for (Stop current : marked) {
                    int arrivalAtCurrent = bestArrival.getOrDefault(current, Integer.MAX_VALUE);
                    if (arrivalAtCurrent == Integer.MAX_VALUE) {
                        continue;
                    }

                    for (Trip trip : outgoing.getOrDefault(current, Collections.emptyList())) {
                        if (!isTripOnRequestedDay(trip, dayType)) {
                            continue;
                        }

                        int departureMinutes;
                        int arrivalMinutes;
                        LocalTime legDeparture = trip.getDepartureTime();

                        if (legDeparture == null) {
                            departureMinutes = arrivalAtCurrent;
                            arrivalMinutes = departureMinutes + Math.max(1, trip.getDuration());
                        } else {
                            departureMinutes = toMinutes(legDeparture);
                            if (departureMinutes < anchorMinutes || departureMinutes < arrivalAtCurrent) {
                                continue;
                            }
                            arrivalMinutes = departureMinutes + Math.max(1, trip.getDuration());
                        }

                        if (arrivalMinutes >= MINUTES_PER_DAY) {
                            continue;
                        }

                        Stop destination = trip.getDestinationStop();
                        if (destination == null) {
                            continue;
                        }

                        int existing = bestArrival.getOrDefault(destination, Integer.MAX_VALUE);
                        if (arrivalMinutes < existing) {
                            bestArrival.put(destination, arrivalMinutes);
                            parentTrip.put(destination, trip);
                            parentStop.put(destination, current);
                            nextMarked.add(destination);
                        }
                    }
                }

                marked = nextMarked;
            }

            int arrival = bestArrival.getOrDefault(target, Integer.MAX_VALUE);
            if (arrival == Integer.MAX_VALUE) {
                return Collections.emptyList();
            }

            List<Trip> path = new ArrayList<>();
            Stop cursor = target;
            Set<Stop> guard = new HashSet<>();
            while (parentTrip.containsKey(cursor) && guard.add(cursor)) {
                Trip leg = parentTrip.get(cursor);
                path.add(leg);
                cursor = parentStop.get(cursor);
                if (cursor == null) {
                    break;
                }
            }
            Collections.reverse(path);
            return path;
        }
    }

    public List<TaxiStop> getTaxiStops() { return taxiGraph.getTaxiStops(); }
    public TaxiGraph getTaxiGraph() { return taxiGraph; }
    public TaxiStop getNearestTaxiStart(double lat, double lon) { return taxiGraph.getNearestTaxiStop(lat, lon); }
    public List<TaxiStop> getNearestTaxiStops(double lat, double lon, int max) { return taxiGraph.getNearestTaxiStops(lat, lon, max); }
    public TrainStop getNearestTrainStart(double lat, double lon) { return trainGraph.getNearestTrainStop(lat, lon); }
    public TrainGraph getTrainGraph() { return trainGraph; }
    public MyCitiBusGraph getMyCitiBusGraph() { return myCitiBusGraph; }
    public GABusGraph getGABusGraph() { return gaBusGraph; }
}
