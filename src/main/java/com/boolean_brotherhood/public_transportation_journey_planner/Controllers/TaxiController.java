package com.boolean_brotherhood.public_transportation_journey_planner.Controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.SystemLog;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiTrip;
import com.fasterxml.jackson.databind.JsonNode;


@RestController
@RequestMapping("/api/taxi")

public class TaxiController {

    private final TaxiGraph graph;

    // Store response metrics: key = endpoint, value = list of response times in ms
    private final Map<String, List<Long>> responseMetrics = new HashMap<>();

    public TaxiController() throws IOException {
        this.graph = new TaxiGraph();
    }

    private void recordResponseTime(String endpoint, long timeMs) {
        responseMetrics.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(timeMs);
    }

    /**
     * Get multiple nearest taxi stops
     */
    @PostMapping("/nearest-stops")
    
    public List<Map<String, Object>> getNearestTaxiStops(@RequestBody JsonNode request) {
        SystemLog.log_endpoint("api/taxi/nearest-stops");
        long startTime = System.currentTimeMillis();

        double lat = request.get("location").get("latitude").asDouble();
        double lon = request.get("location").get("longitude").asDouble();
        int max = request.has("max") ? request.get("max").asInt() : 10;

        List<TaxiStop> stops = graph.getNearestTaxiStops(lat, lon, max);
        List<Map<String, Object>> response = new ArrayList<>();
        for (TaxiStop stop : stops) {
            response.add(Map.of(
                "name", stop.getName(),
                "latitude", stop.getLatitude(),
                "longitude", stop.getLongitude(),
                "Distance", stop.distanceTo(lat, lon),
                "Address", stop.getAddress()
            ));
        }

        long elapsed = System.currentTimeMillis() - startTime;
        recordResponseTime("/nearest-stops", elapsed);

        return response;
    }

    /**
     * List all taxi stops
     */
    @GetMapping("/all-stops")
    public List<Map<String, Object>> getAllTaxiStops() {
        SystemLog.log_endpoint("api/taxi/all-stops");
        long startTime = System.currentTimeMillis();
        List<TaxiStop> stops = graph.getTaxiStops();
        List<Map<String, Object>> response = new ArrayList<>();
        stops.sort(Comparator.comparing(TaxiStop::getName, String.CASE_INSENSITIVE_ORDER));
        for (TaxiStop stop : stops) {
            response.add(Map.of(
                "name", stop.getName(),
                "latitude", stop.getLatitude(),
                "longitude", stop.getLongitude()
            ));
        }

        long elapsed = System.currentTimeMillis() - startTime;
        recordResponseTime("/all-stops", elapsed);

        return response;
    }

    /**
     * List all taxi trips
     */
    @GetMapping("/all-trips")
    public List<Map<String, Object>> getAllTaxiTrips() {
        SystemLog.log_endpoint("api/taxi/all-trips");
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> response = new ArrayList<>();
        for (TaxiTrip trip : graph.getTaxiTrips()) {
            response.add(Map.of(
                "from", trip.getDepartureStop().getName(),
                "to", trip.getDestinationStop().getName(),
                "duration", trip.getDuration()
            ));
        }

        long elapsed = System.currentTimeMillis() - startTime;
        recordResponseTime("/all-trips", elapsed);

        return response;
    }

        /**
     * Return recorded API response metrics
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        SystemLog.log_endpoint("api/taxi/metrics");
        Map<String, Object> metrics = new LinkedHashMap<>();
        graph.getMetrics().forEach(metrics::put);
        metrics.put("stopCount", graph.getTaxiStops().size());
        metrics.put("tripCount", graph.getTaxiTrips().size());

        if (!responseMetrics.isEmpty()) {
            Map<String, Object> timingSnapshot = new LinkedHashMap<>();
            for (Map.Entry<String, List<Long>> entry : responseMetrics.entrySet()) {
                List<Long> times = entry.getValue();
                if (times.isEmpty()) {
                    continue;
                }
                long max = times.stream().mapToLong(Long::longValue).max().orElse(0L);
                long min = times.stream().mapToLong(Long::longValue).min().orElse(0L);
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                long last = times.get(times.size() - 1);
                timingSnapshot.put(entry.getKey(), Map.of(
                        "count", times.size(),
                        "averageMs", Math.round(avg * 100.0) / 100.0,
                        "maxMs", max,
                        "minMs", min,
                        "lastMs", last));
            }
            metrics.put("localTimings", timingSnapshot);
        }

        return MetricsResponseBuilder.build("taxi", metrics, "/api/taxi/");
    }

}
