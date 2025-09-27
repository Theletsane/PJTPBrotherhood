
package com.boolean_brotherhood.public_transportation_journey_planner.Controllers;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.Graph;
import com.boolean_brotherhood.public_transportation_journey_planner.Graph.Mode;
import com.boolean_brotherhood.public_transportation_journey_planner.System.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemLog;
import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip.DayType;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin
public class GraphController {

    private final Graph graph;

    public GraphController(Graph graph) throws IOException {
        this.graph = graph;
    }


    // Removed getGraphStatus endpoint due to missing healthMonitor dependency.
    /** Get all stops across all modes */
    @GetMapping("/stops")
    public List<Map<String, Object>> getAllStops() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Stop> sortedStops = new ArrayList<>(graph.getStops());
        sortedStops.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (Stop stop : sortedStops) {
            result.add(stopToMap(stop));
        }
        return result;
    }

    /** Get stop by name */
    @GetMapping("/stops/{name}")
    public Map<String, Object> getStopByName(@PathVariable String name) {
        Stop stop = graph.findStopByName(name);
        if (stop == null)
            return Map.of("error", "Stop not found");
        return stopToMap(stop);
    }

    /** Find nearest stop for given coordinates */
    @GetMapping("/stops/nearest")
    public Map<String, Object> getNearestStop(@RequestParam double lat, @RequestParam double lon) {
        Stop nearest = null;
        int modeCount = 0;

        // prioritize by Train -> MyCiti -> GA -> Taxi
        nearest = graph.getNearestTrainStart(lat, lon);
        if (nearest != null)
            modeCount++;

        if (nearest == null)
            nearest = graph.getMyCitiBusGraph().getNearestMyCitiStop(lat, lon);
        if (nearest != null)
            modeCount++;

        if (nearest == null)
            nearest = graph.getGABusGraph().getNearestGAStop(lat, lon);
        if (nearest != null)
            modeCount++;

        if (nearest == null)
            nearest = graph.getNearestTaxiStart(lat, lon);
        if (nearest != null)
            modeCount++;

        if (nearest == null)
            return Map.of("error", "No stops found");

        Map<String, Object> map = stopToMap(nearest);
        map.put("modeCount", modeCount);
        return map;
    }

    /** Plan a journey using multimodal RAPTOR */
    @GetMapping("/journey")
    public Map<String, Object> planJourney(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String time,
            @RequestParam(required = false) List<String> modes,
            @RequestParam(required = false) String day) {
        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(time);
        } catch (Exception e) {
            return Map.of("error", "Invalid time format. Use HH:mm");
        }

        EnumSet<Mode> modeSet = parseModes(modes);
        DayType dayType = parseDay(day);

        List<Trip> path = graph.runRaptor(modeSet, from, to, departureTime, 5, dayType);
        if (path.isEmpty())
            return Map.of("error", "No journey found");

        Map<String, Object> response = new HashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("departure", path.get(0).getDepartureStop().getName());
        response.put("arrival", path.get(path.size() - 1).getDestinationStop().getName());
        response.put("durationMinutes", path.stream().mapToInt(Trip::getDuration).sum());
        //response.put("transfers", path.size() - 1);

        List<Map<String, Object>> miniTrips = new ArrayList<>();
        for (Trip t : path) {
            Map<String, Object> tripMap = new HashMap<>();
            tripMap.put("from", t.getDepartureStop().getName());
            tripMap.put("to", t.getDestinationStop().getName());
            tripMap.put("durationMinutes", t.getDuration());
            tripMap.put("mode", t.getMode());
            miniTrips.add(tripMap);
        }
        response.put("miniTrips", miniTrips);
        return response;
    }

    /** Helper: Convert Stop to Map */
    private Map<String, Object> stopToMap(Stop stop) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", stop.getName());
        map.put("latitude", stop.getLatitude());
        map.put("longitude", stop.getLongitude());
        return map;
    }

    /** Helper: Parse modes list from request */
    private EnumSet<Mode> parseModes(List<String> modes) {
        if (modes == null || modes.isEmpty())
            return EnumSet.allOf(Mode.class);
        EnumSet<Mode> set = EnumSet.noneOf(Mode.class);
        for (String m : modes) {
            try {
                set.add(Mode.valueOf(m.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (!set.contains(Mode.WALKING))
            set.add(Mode.WALKING); // always allow walking
        return set;
    }

    /** Helper: Parse DayType from request */
    private DayType parseDay(String day) {
        if (day == null)
            return DayType.WEEKDAY;
        try {
            return DayType.valueOf(day.toUpperCase());
        } catch (Exception e) {
            return DayType.WEEKDAY;
        }
    }

    /** Get all trips departing from a stop */
    @GetMapping("/stops/{name}/trips")
    public List<Map<String, Object>> getTripsFromStop(@PathVariable String name) {
        Stop stop = graph.findStopByName(name);
        if (stop == null)
            return Collections.emptyList();
        List<Map<String, Object>> trips = new ArrayList<>();
        for (Trip t : stop.getTrips()) {
            Map<String, Object> tripMap = new HashMap<>();
            tripMap.put("from", t.getDepartureStop().getName());
            tripMap.put("to", t.getDestinationStop().getName());
            tripMap.put("durationMinutes", t.getDuration());
            tripMap.put("mode", t.getMode());
            trips.add(tripMap);
        }
        return trips;
    }

    /** Metrics endpoint */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("totalStops", graph.getStops().size());
        snapshot.put("totalTrips", graph.getTrips().size());

        Map<String, Object> stopsByMode = new LinkedHashMap<>();
        stopsByMode.put("train", graph.getTrainGraph().getTrainStops().size());
        stopsByMode.put("myciti", graph.getMyCitiBusGraph().getMyCitiStops().size());
        stopsByMode.put("ga", graph.getGABusGraph().getGAStops().size());
        stopsByMode.put("taxi", graph.getTaxiGraph().getTaxiStops().size());
        snapshot.put("stopsByMode", stopsByMode);

        Map<String, Object> tripsByMode = new LinkedHashMap<>();
        tripsByMode.put("train", graph.getTrainGraph().getTrainTrips().size());
        tripsByMode.put("myciti", graph.getMyCitiBusGraph().getMyCitiTrips().size());
        tripsByMode.put("ga", graph.getGABusGraph().getGATrips().size());
        tripsByMode.put("taxi", graph.getTaxiGraph().getTaxiTrips().size());
        snapshot.put("tripsByMode", tripsByMode);

        return MetricsResponseBuilder.build("graph", snapshot, "/api/graph");
    }
}
