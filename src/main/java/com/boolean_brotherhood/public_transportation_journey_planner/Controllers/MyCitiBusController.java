package com.boolean_brotherhood.public_transportation_journey_planner.Controllers;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.System.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemLog;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop;


@RestController
@RequestMapping("/api/myciti")

public class MyCitiBusController {

    private final MyCitiBusGraph graph;
    private final MyCitiBusGraph.MyCitiRaptor raptor;

    @Autowired
    public MyCitiBusController(MyCitiBusGraph graph) {
        this.graph = graph;
        this.raptor = new MyCitiBusGraph.MyCitiRaptor(graph);
    }

    /**
     * Get system metrics (stops, trips, load times, etc.)
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        SystemLog.log_endpoint("/api/myciti/metrics");
        Map<String, Object> metrics = new LinkedHashMap<>();
        graph.getMetrics().forEach(metrics::put);

        List<MyCitiStop> stops = graph.getMyCitiStops();
        List<MyCitiTrip> trips = graph.getMyCitiTrips();

        int stopCount = stops != null ? stops.size() : 0;
        int tripCount = trips != null ? trips.size() : 0;
        metrics.put("totalStops", stopCount);
        metrics.put("totalTrips", tripCount);
        metrics.put("stopsLoaded", stopCount > 0);
        metrics.put("tripsLoaded", tripCount > 0);
        metrics.put("routesTracked", trips == null ? 0 : trips.stream().map(MyCitiTrip::getRouteName).filter(java.util.Objects::nonNull).distinct().count());

        return MetricsResponseBuilder.build("mycitiBus", metrics, "/api/myciti/");
    }

    /**
     * Get all stops
     */
    @GetMapping("/stops")
    public List<Map<String, Object>> getStops() {
        SystemLog.log_endpoint("/api/myciti/stops");

        List<MyCitiStop> stops = graph.getMyCitiStops();
        List<Map<String, Object>> response = new ArrayList<>();
        stops.sort(Comparator.comparing(stop -> stop.getName(), String.CASE_INSENSITIVE_ORDER));
        
        for (MyCitiStop stop : stops) {
            response.add(Map.of(
                "name", stop.getName(),
                "latitude", stop.getLatitude(),
                "longitude", stop.getLongitude()
            ));
        }
        return response;
    }

    /**
     * Get all trips
     */
    @GetMapping("/trips")
    public List<Map<String, Object>> getTrips() {
        SystemLog.log_endpoint("/api/myciti/trips");
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> response = new ArrayList<>();
        List<MyCitiTrip> trips = graph.getMyCitiTrips();
        for (MyCitiTrip trip : trips) {
            response.add(Map.of(
                "from", trip.getDepartureStop().getName(),
                "to", trip.getDestinationStop().getName(),
                "duration", trip.getDuration()
            ));
        }

        long elapsed = System.currentTimeMillis() - startTime;

        return response;
    }

    @GetMapping("/logs")
    public List<String> getLogs() {
        SystemLog.log_endpoint("/api/myciti/logs");
        return graph.getLogs();
    }

    /**
     * Run a RAPTOR journey search
     * @param source Starting stop name
     * @param target Destination stop name
     * @param departure Departure time (HH:mm), default 08:00
     * @param maxRounds Maximum number of rounds (default 4)
     * @return Journey object or error message
     */
    @GetMapping("/journey")
    public Object getJourney(
            @RequestParam String source,
            @RequestParam String target,
            @RequestParam(defaultValue = "08:00") String departure,
            @RequestParam(defaultValue = "4") int maxRounds,
            @RequestParam(required = false) String day) {
        
        SystemLog.log_endpoint("/api/myciti/journey");
        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(departure);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid departure time format. Use HH:mm");
            return error;
        }

        Trip.DayType dayType = parseDay(day);

        MyCitiBusJourney journey = raptor.runRaptor(source, target, departureTime, maxRounds, dayType);

        if (journey == null || journey.getTrips().isEmpty()) {
            return Map.of("error", "No journey found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("from", journey.getSource().getName());
        response.put("to", journey.getDestination().getName());
        response.put("departure", journey.getDepartureTime().toString());
        response.put("arrival", journey.getArrivalTime().toString());
        response.put("durationMinutes", journey.getTotalDurationMinutes());
        response.put("transfers", journey.getNumberOfTransfers());

        // Mini trips: each leg between stops
        List<Map<String, Object>> miniTrips = new ArrayList<>();
        for (MyCitiTrip trip : journey.getTrips()) {
            Map<String, Object> miniTripMap = new HashMap<>();
            miniTripMap.put("tripId", trip.getTripID());
            miniTripMap.put("route", trip.getRouteName());
            miniTripMap.put("from", trip.getDepartureMyCitiStop().getName());
            miniTripMap.put("to", trip.getDestinationMyCitiStop().getName());
            miniTripMap.put("departure", trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "N/A");
            miniTripMap.put("durationMinutes", trip.getDuration());
            miniTrips.add(miniTripMap);
        }
        response.put("miniTrips", miniTrips);

        // Full journey summary
        Map<String, Object> fullJourney = new HashMap<>();
        fullJourney.put("totalDurationMinutes", journey.getTotalDurationMinutes());
        fullJourney.put("totalTransfers", journey.getNumberOfTransfers());
        fullJourney.put("path", journey.getTrips().stream()
                .map(t -> t.getDepartureMyCitiStop().getName() + " -> " + t.getDestinationMyCitiStop().getName())
                .toList());
        response.put("fullJourney", fullJourney);

        return response;
    }

    private Trip.DayType parseDay(String day) {
        if (day == null || day.isBlank()) {
            return Trip.DayType.WEEKDAY;
        }
        try {
            return Trip.DayType.valueOf(day.trim().toUpperCase());
        } catch (Exception e) {
            return Trip.DayType.WEEKDAY;
        }
    }

}
