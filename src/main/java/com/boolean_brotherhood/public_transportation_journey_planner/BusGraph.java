package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;

/**
 * Combined bus graph that merges MyCiTi and Golden Arrow data and exposes
 * day-aware RAPTOR and CSA routing.
 */
public class BusGraph {

    private static final Logger LOGGER = Logger.getLogger(BusGraph.class.getName());
    private static final int MINUTES_PER_DAY = 24 * 60;

    private final MyCitiBusGraph myCitiGraph;
    private final GABusGraph gaGraph;

    private final List<Stop> combinedStops = new ArrayList<>();
    private final List<Trip> combinedTrips = new ArrayList<>();

    private final RaptorRouter raptorRouter;
    private final ConnectionScanRouter connectionRouter;

    public BusGraph() {
        this(createMyCitiGraph(), createGAGraph());
    }

    public BusGraph(MyCitiBusGraph myCitiGraph, GABusGraph gaGraph) {
        this.myCitiGraph = Objects.requireNonNull(myCitiGraph, "myCitiGraph");
        this.gaGraph = Objects.requireNonNull(gaGraph, "gaGraph");
        buildCombinedNetwork();
        this.raptorRouter = new RaptorRouter(combinedStops, combinedTrips);
        this.connectionRouter = new ConnectionScanRouter(combinedStops, combinedTrips);
    }

    private static MyCitiBusGraph createMyCitiGraph() {
        try {
            return new MyCitiBusGraph();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Unable to initialise MyCiTi bus graph", ioe);
            throw new IllegalStateException("Failed to load MyCiTi bus data", ioe);
        }
    }

    private static GABusGraph createGAGraph() {
        try {
            return new GABusGraph();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Unable to initialise Golden Arrow bus graph", ioe);
            throw new IllegalStateException("Failed to load Golden Arrow bus data", ioe);
        }
    }

    private void buildCombinedNetwork() {
        combinedStops.clear();
        combinedTrips.clear();

        addStops(myCitiGraph.getMyCitiStops());
        addStops(gaGraph.getGAStops());

        addTrips(myCitiGraph.getMyCitiTrips());
        addTrips(gaGraph.getGATrips());

        LOGGER.info(() -> String.format("Combined bus graph ready: %d stops, %d trips", combinedStops.size(),
                combinedTrips.size()));
    }

    private void addStops(List<? extends Stop> stops) {
        for (Stop stop : stops) {
            if (stop == null) {
                continue;
            }
            combinedStops.add(stop);
            SystemLog.add_stop(stop);
        }
    }

    private void addTrips(List<? extends Trip> trips) {
        for (Trip trip : trips) {
            if (trip == null) {
                continue;
            }

            if (trip instanceof MyCitiTrip myCitiTrip) {
                myCitiTrip.setMode("MyCiTi Bus");
                if (myCitiTrip.getRouteName() != null) {
                    SystemLog.add_active_route(myCitiTrip.getRouteName());
                }
            } else if (trip instanceof GATrip gaTrip) {
                gaTrip.setMode("Golden Arrow Bus");
                if (gaTrip.getRouteName() != null) {
                    SystemLog.add_active_route(gaTrip.getRouteName());
                }
            }

            combinedTrips.add(trip);
        }
    }

    public List<Stop> getStops() {
        return Collections.unmodifiableList(combinedStops);
    }

    public List<Trip> getTrips() {
        return Collections.unmodifiableList(combinedTrips);
    }

    public RaptorRouter getRaptorRouter() {
        return raptorRouter;
    }

    public ConnectionScanRouter getConnectionRouter() {
        return connectionRouter;
    }

    public RouterResult runRaptor(Stop source, Stop target, LocalTime departureTime, int maxRounds,
            Trip.DayType dayType) {
        return raptorRouter.runRaptor(source, target, departureTime, maxRounds, dayType);
    }

    public RouterResult runConnectionScan(Stop source, Stop target, LocalTime departureTime, Trip.DayType dayType) {
        return connectionRouter.runConnectionScan(source, target, departureTime, dayType);
    }

    public MyCitiBusGraph getMyCitiGraph() {
        return myCitiGraph;
    }

    public GABusGraph getGaGraph() {
        return gaGraph;
    }

    public static class RouterResult {
        public final List<Trip> trips;
        public final LocalTime departure;
        public final LocalTime arrival;
        public final int totalMinutes;
        public final int transfers;

        public RouterResult(List<Trip> trips, LocalTime departure, LocalTime arrival, int totalMinutes, int transfers) {
            this.trips = trips == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(trips));
            this.departure = departure;
            this.arrival = arrival;
            this.totalMinutes = totalMinutes;
            this.transfers = transfers;
        }
    }

    public static final class RaptorRouter {
        private final List<Stop> stops;
        private final Map<Stop, List<Trip>> outgoing;

        public RaptorRouter(List<Stop> stops, List<Trip> trips) {
            this.stops = new ArrayList<>(new LinkedHashSet<>(stops));
            this.outgoing = new HashMap<>();
            for (Trip trip : trips) {
                Stop departure = trip.getDepartureStop();
                if (departure == null) {
                    continue;
                }
                outgoing.computeIfAbsent(departure, key -> new ArrayList<>()).add(trip);
            }
            for (List<Trip> legs : outgoing.values()) {
                legs.sort(Comparator
                        .comparing(trip -> trip.getDepartureTime() == null ? LocalTime.MIN : trip.getDepartureTime()));
            }
        }

        public RouterResult runRaptor(Stop source, Stop target, LocalTime requestedDeparture, int maxRounds,
                Trip.DayType dayType) {
            if (source == null || target == null || !outgoing.containsKey(source)) {
                return null;
            }

            LocalTime anchor = requestedDeparture == null ? LocalTime.MIN : requestedDeparture;
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
            for (int round = 0; round < rounds; round++) {
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
                            continue; // crosses into next day
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

                if (nextMarked.isEmpty()) {
                    break;
                }
                marked = nextMarked;
            }

            int arrivalMinutes = bestArrival.getOrDefault(target, Integer.MAX_VALUE);
            if (arrivalMinutes == Integer.MAX_VALUE) {
                return null;
            }

            List<Trip> path = reconstructPath(target, parentTrip, parentStop);
            LocalTime arrivalTime = fromMinutes(arrivalMinutes);
            int transfers = Math.max(0, path.size() - 1);
            int totalDuration = Math.max(0, arrivalMinutes - anchorMinutes);

            return new RouterResult(path, anchor, arrivalTime, totalDuration, transfers);
        }

        private List<Trip> reconstructPath(Stop target, Map<Stop, Trip> parentTrip, Map<Stop, Stop> parentStop) {
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

    public static final class ConnectionScanRouter {
        private final List<Connection> connections;
        private final Map<Stop, Integer> stopToId;
        private final List<Stop> idToStop;

        public ConnectionScanRouter(List<Stop> stops, List<Trip> trips) {
            this.stopToId = new HashMap<>();
            this.idToStop = new ArrayList<>();
            int index = 0;
            for (Stop stop : new LinkedHashSet<>(stops)) {
                stopToId.put(stop, index++);
                idToStop.add(stop);
            }

            this.connections = new ArrayList<>();
            for (Trip trip : trips) {
                LocalTime dep = trip.getDepartureTime();
                if (dep == null) {
                    continue;
                }
                Integer from = stopToId.get(trip.getDepartureStop());
                Integer to = stopToId.get(trip.getDestinationStop());
                if (from == null || to == null) {
                    continue;
                }
                int depMinutes = toMinutes(dep);
                int arrMinutes = depMinutes + Math.max(1, trip.getDuration());
                if (arrMinutes >= MINUTES_PER_DAY) {
                    continue;
                }
                connections.add(new Connection(from, to, depMinutes, arrMinutes, trip));
            }
            connections.sort(Comparator.naturalOrder());
        }

        public RouterResult runConnectionScan(Stop source, Stop target, LocalTime requestedDeparture,
                Trip.DayType dayType) {
            Integer srcId = stopToId.get(source);
            Integer tgtId = stopToId.get(target);
            if (srcId == null || tgtId == null) {
                return null;
            }

            int startMinutes = toMinutes(requestedDeparture == null ? LocalTime.MIN : requestedDeparture);
            int n = idToStop.size();
            int[] best = new int[n];
            int[] prev = new int[n];
            Arrays.fill(best, Integer.MAX_VALUE);
            Arrays.fill(prev, -1);
            best[srcId] = startMinutes;

            int startIndex = lowerBound(startMinutes);
            int bestTarget = Integer.MAX_VALUE;

            for (int i = startIndex; i < connections.size(); i++) {
                Connection conn = connections.get(i);
                if (conn.departure >= MINUTES_PER_DAY) {
                    break;
                }
                if (conn.departure > bestTarget) {
                    break; // future departures are later than best known arrival
                }
                if (!isTripOnRequestedDay(conn.trip, dayType)) {
                    continue;
                }
                if (conn.departure < best[conn.from]) {
                    continue;
                }
                if (conn.arrival < best[conn.to]) {
                    best[conn.to] = conn.arrival;
                    prev[conn.to] = i;
                    if (conn.to == tgtId) {
                        bestTarget = conn.arrival;
                    }
                }
            }

            if (best[tgtId] == Integer.MAX_VALUE) {
                return null;
            }

            List<Trip> legs = new ArrayList<>();
            int cursor = tgtId;
            Set<Integer> guard = new HashSet<>();
            while (cursor != srcId && prev[cursor] != -1 && guard.add(cursor)) {
                Connection used = connections.get(prev[cursor]);
                legs.add(used.trip);
                cursor = used.from;
            }
            Collections.reverse(legs);

            int arrival = best[tgtId];
            LocalTime arrivalTime = fromMinutes(arrival);
            LocalTime departureTime = fromMinutes(startMinutes);
            int duration = Math.max(0, arrival - startMinutes);
            int transfers = Math.max(0, legs.size() - 1);

            return new RouterResult(legs, departureTime, arrivalTime, duration, transfers);
        }

        private int lowerBound(int departureMinutes) {
            int left = 0;
            int right = connections.size();
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (connections.get(mid).departure < departureMinutes) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
            return left;
        }

        private static final class Connection implements Comparable<Connection> {
            final int from;
            final int to;
            final int departure;
            final int arrival;
            final Trip trip;

            Connection(int from, int to, int departure, int arrival, Trip trip) {
                this.from = from;
                this.to = to;
                this.departure = departure;
                this.arrival = arrival;
                this.trip = trip;
            }

            @Override
            public int compareTo(Connection other) {
                int cmp = Integer.compare(this.departure, other.departure);
                if (cmp != 0) {
                    return cmp;
                }
                return Integer.compare(this.arrival, other.arrival);
            }
        }
    }

    private static boolean isTripOnRequestedDay(Trip trip, Trip.DayType dayType) {
        Trip.DayType tripDay = trip.getDayType();
        if (dayType == null || tripDay == null) {
            return true;
        }
        return tripDay == dayType;
    }

    private static int toMinutes(LocalTime time) {
        if (time == null) {
            return 0;
        }
        return time.getHour() * 60 + time.getMinute();
    }

    private static LocalTime fromMinutes(int minutes) {
        int safe = Math.max(0, Math.min(minutes, MINUTES_PER_DAY - 1));
        return LocalTime.of(safe / 60, safe % 60);
    }
}
