package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip.DayType;

/*
 * Combining MyCiti data and GA data to form 1 graph.
 * How - Create graphs of both, get stops and trips from both, make raptor and csa
 */
public class BusGraph {

    MyCitiBusGraph myCitiGraph;
    GABusGraph gaGraph;
    List<Stop> allBusStops = new ArrayList<>();
    List<Trip> allBusTrips = new ArrayList<>();

    public BusGraph (MyCitiBusGraph mycitiGraph, GABusGraph gaBusGraph){
        try{
            myCitiGraph = mycitiGraph;
            gaGraph = gaBusGraph;
              
        }catch(Exception e){
            System.err.println("Error Loading");
        }
        loadStops();
        loadTrips();
    }

    public List<Stop> getAllBusStops(){
        return Collections.unmodifiableList(allBusStops);
    }
    public List<Trip> getAllBusTrips(){
        return Collections.unmodifiableList(allBusTrips);
    }

    public MyCitiBusGraph getMyCitiGraph(){
        return myCitiGraph;
    }

    public GABusGraph getGaGraph(){
        return gaGraph;
    }

    private void loadStops(){
        //Loop through all stops
        //keep (name, coords,company)
        //instead of using list.addTo(otherList) use this instead so i know the format of my data
        List<MyCitiStop> myCitiStops = myCitiGraph.getMyCitiStops();
        List<GAStop> GAstops = gaGraph.getGAStops();
        for (MyCitiStop citiStop : myCitiStops){
            String name = citiStop.getName();
            Double x_cord = citiStop.getLatitude();
            Double y_cord = citiStop.getLongitude();
            allBusStops.add(new Stop(name,x_cord,y_cord,"MYCITI")); //made Stop not abstract
        }
        for (GAStop ga : GAstops){
            String name = ga.getName();
            Double x_cord = ga.getLatitude();
            Double y_cord = ga.getLongitude();
            allBusStops.add(new Stop(name,x_cord,y_cord,"GA"));
        }
    }

    private void loadTrips(){
        //need (start,end,depature_time,duration,weekday)
        List<MyCitiTrip> myTrips = myCitiGraph.getMyCitiTrips();
        List<GATrip> GATrips = gaGraph.getGATrips();
        for (MyCitiTrip trip : myTrips){
            Stop dep = trip.getDepartureStop();
            Stop dest = trip.getDestinationStop();
            LocalTime departureTime = trip.getDepartureTime();
            int dur = trip.getDuration();
            DayType day = trip.getDayType();
            allBusTrips.add(new Trip(dep, dest, departureTime,dur, day));

        }
        for (GATrip tr : GATrips){
            Stop dep = tr.getDepartureStop();
            Stop dest = tr.getDestinationStop();
            LocalTime departureTime = tr.getDepartureTime();
            int dur = tr.getDuration();
            DayType day = tr.getDayType();
            allBusTrips.add(new Trip(dep, dest, departureTime,dur, day));
        }

    }

    /**
     * The result object returned by routing queries.
     */
    public static class RouterResult {
        public final List<Trip> trips; // ordered trips (legs)
        public final LocalTime departure;
        public final LocalTime arrival;
        public final int totalMinutes;
        public final int transfers;

        public RouterResult(List<Trip> trips, LocalTime departure, LocalTime arrival, int totalMinutes, int transfers) {
            this.trips = Collections.unmodifiableList(new ArrayList<>(trips));
            this.departure = departure;
            this.arrival = arrival;
            this.totalMinutes = totalMinutes;
            this.transfers = transfers;
        }
    }

    /**
     * RaptorRouter
     *
     * A reasonably efficient RAPTOR implementation built to work on a list of Stops and Trips.
     *
     * - Preprocesses trips into per-stop outgoing TripInstances (each trip duplicated for next-day
     *   by adding 1440 minutes where necessary). This allows RAPTOR to operate in an absolute
     *   minutes domain and avoids tricky modular arithmetic at query time.
     * - Uses binary search to find the first potentially catchable trip in an outgoing list.
     * - Returns a RouterResult containing the sequence of Trips, departure/arrival LocalTime, duration and transfers.
     *
     * Assumptions about Trip API (must be present in your project):
     * - Trip.getDepartureStop() -> Stop
     * - Trip.getDestinationStop() -> Stop
     * - Trip.getDepartureTime() -> LocalTime
     * - Trip.getDuration() -> int (minutes)
     * - Trip.getDayType() -> Trip.DayType (enum)
     */
    public static class RaptorRouter {

        private static final Logger LOGGER = Logger.getLogger(RaptorRouter.class.getName());
        private final Map<Stop, Integer> stopToId = new HashMap<>();
        private final List<Stop> idToStop = new ArrayList<>();
        // per-stop outgoing list of TripInstance sorted by dep (minutes)
        private final List<List<TripInstance>> outgoing;

        /**
         * Lightweight container used internally by the router. Immutable.
         */
        private static class TripInstance {
            final int fromId;
            final int toId;
            final int dep;   // minutes since midnight, possibly >= 1440 for next-day copy
            final int arr;   // minutes since midnight, arr >= dep
            final Trip trip;

            TripInstance(int fromId, int toId, int dep, int arr, Trip trip) {
                this.fromId = fromId;
                this.toId = toId;
                this.dep = dep;
                this.arr = arr;
                this.trip = trip;
            }
        }



        /**
         * Create a RaptorRouter for the provided stops & trips.
         * This constructor builds internal indices and outgoing lists.
         *
         * @param stops list of Stop objects (all stops used by trips should be present)
         * @param trips list of Trip objects (each Trip must have departureStop/destinationStop)
         */
        public RaptorRouter(List<Stop> stops, List<Trip> trips) {
            // build id mapping
            for (int i = 0; i < stops.size(); i++) {
                Stop s = stops.get(i);
                stopToId.put(s, i);
                idToStop.add(s);
            }

            int n = idToStop.size();
            outgoing = new ArrayList<>(n);
            for (int i = 0; i < n; i++) outgoing.add(new ArrayList<>());

            // convert trips to TripInstance; duplicate to next day to handle midnight wrap
            for (Trip t : trips) {
                Stop f = t.getDepartureStop();
                Stop to = t.getDestinationStop();
                Integer fromId = stopToId.get(f);
                Integer toId = stopToId.get(to);
                if (fromId == null || toId == null) {
                    // skip trips whose stops are not in stops list
                    LOGGER.log(Level.FINER, "Skipping trip because stop mapping missing: {0}", t);
                    continue;
                }
                LocalTime depLT = t.getDepartureTime();
                if (depLT == null) continue;
                int dep = depLT.getHour() * 60 + depLT.getMinute();
                int arr = dep + t.getDuration();
                if (arr <= dep) arr += 24 * 60; // cross-midnight single-leg

                TripInstance inst = new TripInstance(fromId, toId, dep, arr, t);
                outgoing.get(fromId).add(inst);

                // add next-day duplicate so queries late in day will still see next-day departures
                TripInstance nextDay = new TripInstance(fromId, toId, dep + 24 * 60, arr + 24 * 60, t);
                outgoing.get(fromId).add(nextDay);
            }

            // sort outgoing lists by departure time
            for (List<TripInstance> list : outgoing) {
                list.sort(Comparator.comparingInt(a -> a.dep));
            }
        }

        /**
         * Run RAPTOR to find an earliest-arrival path from source to target starting at or after departureTime.
         *
         * @param source       starting Stop (must be present in the stops list used at construction)
         * @param target       destination Stop
         * @param departureTime desired departure LocalTime
         * @param maxRounds    maximum number of rounds (controls number of transfers allowed)
         * @param dayType      Trip.DayType to filter trips (WEEKDAY/SATURDAY/SUNDAY or your enum)
         * @return RouterResult containing journey details, or null if no path found
         */
        public RouterResult runRaptor(Stop source, Stop target, LocalTime departureTime, int maxRounds, Trip.DayType dayType) {
            Integer srcId = stopToId.get(source);
            Integer tgtId = stopToId.get(target);
            if (srcId == null || tgtId == null) {
                LOGGER.log(Level.WARNING, "RAPTOR: source or target not in stop index: {0} -> {1}", new Object[]{source, target});
                return null;
            }

            final int n = idToStop.size();
            final int INF = Integer.MAX_VALUE / 4;
            int[] bestArrival = new int[n];
            Arrays.fill(bestArrival, INF);
            bestArrival[srcId] = minutesOfDay(departureTime); // we keep absolute minutes reference, allowing values >1440 from duplicates

            int[] prevStop = new int[n]; // previous stop id in path
            Trip[] prevTrip = new Trip[n]; // previous trip used to reach stop
            Arrays.fill(prevStop, -1);

            Set<Integer> marked = new HashSet<>();
            marked.add(srcId);

            for (int round = 0; round < Math.max(1, maxRounds); round++) {
                Set<Integer> nextMarked = new HashSet<>();

                for (int markedId : marked) {
                    List<TripInstance> list = outgoing.get(markedId);
                    if (list == null || list.isEmpty()) continue;

                    // binary search to find first instance with dep >= bestArrival[markedId]
                    int earliest = bestArrival[markedId];
                    int idx = lowerBoundTripInstance(list, earliest);

                    for (int i = idx; i < list.size(); i++) {
                        TripInstance inst = list.get(i);

                        // filter by day type
                        if (inst.trip.getDayType() != dayType) continue;

                        // if this departure is earlier than our arrival at marked stop, skip
                        if (inst.dep < earliest) continue;

                        int dest = inst.toId;
                        int arr = inst.arr;

                        if (arr < bestArrival[dest]) {
                            bestArrival[dest] = arr;
                            prevStop[dest] = markedId;
                            prevTrip[dest] = inst.trip;
                            nextMarked.add(dest);
                        }
                    }
                }

                if (nextMarked.isEmpty()) break;
                marked = nextMarked;
            }

            if (bestArrival[tgtId] >= Integer.MAX_VALUE / 8) {
                return null; // unreachable
            }

            // reconstruct path (trip list)
            List<Trip> path = new ArrayList<>();
            int cur = tgtId;
            Set<Integer> seen = new HashSet<>();
            while (cur != srcId && prevStop[cur] != -1 && !seen.contains(cur)) {
                seen.add(cur);
                Trip t = prevTrip[cur];
                if (t == null) break;
                path.add(t);
                cur = prevStop[cur];
            }
            Collections.reverse(path);

            int arrivalMinute = bestArrival[tgtId];
            LocalTime arrivalLocal = LocalTime.of((arrivalMinute % (24 * 60)) / 60, arrivalMinute % 60);
            int totalMinutes = arrivalMinute - minutesOfDay(departureTime);
            if (totalMinutes < 0) totalMinutes += 24 * 60;

            int transfers = Math.max(0, path.size() - 1);

            return new RouterResult(path, departureTime, arrivalLocal, totalMinutes, transfers);
        }

        // --- helpers ---

        /**
         * Convert LocalTime to minutes since midnight.
         */
        private static int minutesOfDay(LocalTime t) {
            return t.getHour() * 60 + t.getMinute();
        }

        /**
         * Lower bound in a sorted TripInstance list for first element with dep >= target.
         */
        private static int lowerBoundTripInstance(List<TripInstance> list, int target) {
            int left = 0, right = list.size();
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (list.get(mid).dep < target) left = mid + 1;
                else right = mid;
            }
            return left;
        }
    }


/**
 * ConnectionScanRouter
 *
 * Implements the Connection Scan Algorithm for earliest-arrival queries.
 *
 * - Builds a sorted connection list from the provided Trip list. Each connection is duplicated
 *   for the next day (dep+1440 / arr+1440) so queries late in the day find next-day departures.
 * - runConnectionScan performs a linear scan starting at the first connection with dep >= requested departure.
 * - Returns a RouterResult (same structure as RaptorRouter.RouterResult).
 *
 * Assumptions about Trip API:
 *  - Trip.getDepartureStop(), Trip.getDestinationStop(), Trip.getDepartureTime(), Trip.getDuration(), Trip.getDayType()
 */
public static class ConnectionScanRouter {

private static final Logger LOGGER = Logger.getLogger(ConnectionScanRouter.class.getName());

private final Map<Stop, Integer> stopToId = new HashMap<>();
private final List<Stop> idToStop = new ArrayList<>();
private final List<Connection> connections = new ArrayList<>();

private static class Connection implements Comparable<Connection> {
    final int from;
    final int to;
    final int dep; // departure minutes (may be >=1440 for next-day copy)
    final int arr; // arrival minutes (>= dep)
    final Trip trip;

    Connection(int from, int to, int dep, int arr, Trip trip) {
        this.from = from;
        this.to = to;
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
 * Construct router from stops and trips (stops list should include all stops referenced by trips).
 *
 * @param stops list of Stop
 * @param trips list of Trip
 */
public ConnectionScanRouter(List<Stop> stops, List<Trip> trips) {
    for (int i = 0; i < stops.size(); i++) {
        Stop s = stops.get(i);
        stopToId.put(s, i);
        idToStop.add(s);
    }

    // Build connections list. For each Trip we create an entry for the service-day and a duplicate next-day copy.
    for (Trip t : trips) {
        Stop f = t.getDepartureStop();
        Stop to = t.getDestinationStop();
        Integer fromId = stopToId.get(f);
        Integer toId = stopToId.get(to);
        if (fromId == null || toId == null) {
            LOGGER.log(Level.FINER, "CSA skipping trip due to missing stop mapping: {0}", t);
            continue;
        }
        LocalTime depLT = t.getDepartureTime();
        if (depLT == null) continue;
        int dep = depLT.getHour() * 60 + depLT.getMinute();
        int arr = dep + t.getDuration();
        if (arr <= dep) arr += 24 * 60; // single-leg crosses midnight -> push arrival next day

        // day 0 copy
        connections.add(new Connection(fromId, toId, dep, arr, t));
        // day 1 copy (so queries near midnight will see the next-day departures)
        connections.add(new Connection(fromId, toId, dep + 24 * 60, arr + 24 * 60, t));
    }

    // sort by departure ascending
    Collections.sort(connections);
}

/**
 * Run CSA earliest-arrival query.
 *
 * @param source      starting Stop
 * @param target      destination Stop
 * @param departureTime LocalTime desired departure (service-day baseline)
 * @param dayType     Trip.DayType filter (WEEKDAY/SATURDAY/etc.)
 * @return RouterResult or null if no itinerary found
 */
public RouterResult runConnectionScan(Stop source, Stop target, LocalTime departureTime, Trip.DayType dayType) {
    Integer srcId = stopToId.get(source);
    Integer tgtId = stopToId.get(target);
    if (srcId == null || tgtId == null) {
        LOGGER.log(Level.WARNING, "CSA: source/target not in index: {0} -> {1}", new Object[]{source, target});
        return null;
    }

    final int n = idToStop.size();
    final int INF = Integer.MAX_VALUE / 4;
    int[] best = new int[n];
    int[] prevConn = new int[n];
    Arrays.fill(best, INF);
    Arrays.fill(prevConn, -1);

    int depMinutes = minutesOfDay(departureTime);
    best[srcId] = depMinutes;

    // find start index (first connection with dep >= depMinutes)
    int start = lowerBoundConnection(depMinutes);

    int bestTarget = INF;

    // scan forward
    for (int i = start; i < connections.size(); i++) {
        Connection c = connections.get(i);

        // pruning: if we already have an arrival to target earlier than this connection's departure, safe to stop
        if (c.dep > bestTarget) break;

        // filter by day type of the underlying trip
        if (c.trip.getDayType() != dayType) continue;

        if (c.dep < best[c.from]) continue; // can't catch this connection

        if (c.arr < best[c.to]) {
            best[c.to] = c.arr;
            prevConn[c.to] = i;
            if (c.to == tgtId && c.arr < bestTarget) {
                bestTarget = c.arr;
            }
        }
    }

    if (best[tgtId] >= INF) {
        return null;
    }

    // reconstruct trip chain
    List<Trip> path = new ArrayList<>();
    int cur = tgtId;
    Set<Integer> seen = new HashSet<>();
    while (cur != srcId && prevConn[cur] != -1 && !seen.contains(cur)) {
        seen.add(cur);
        Connection used = connections.get(prevConn[cur]);
        path.add(used.trip);
        cur = used.from;
    }
    Collections.reverse(path);

    int arrivalMinute = best[tgtId];
    LocalTime arrivalLocal = LocalTime.of((arrivalMinute % (24 * 60)) / 60, arrivalMinute % 60);
    int totalMins = arrivalMinute - depMinutes;
    if (totalMins < 0) totalMins += 24 * 60;
    int transfers = Math.max(0, path.size() - 1);

    return new RouterResult(path, departureTime, arrivalLocal, totalMins, transfers);
}

    // helpers
    private static int minutesOfDay(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private int lowerBoundConnection(int depMinutes) {
        int left = 0, right = connections.size();
        while (left < right) {
            int mid = (left + right) >>> 1;
            if (connections.get(mid).dep < depMinutes) left = mid + 1;
            else right = mid;
        }
        return left;
    }
    }

}

