package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiTrip;
import com.fasterxml.jackson.databind.JsonNode;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/taxi")
public class TaxiController {

    private final Graph graph;

    // Store response metrics: key = endpoint, value = list of response times in ms
    private final Map<String, List<Long>> responseMetrics = new HashMap<>();

    public TaxiController() {
        this.graph = new Graph();
        graph.LoadTaxiData(); // Load taxi data at startup
    }

    private void recordResponseTime(String endpoint, long timeMs) {
        responseMetrics.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(timeMs);
    }

    /**
     * Get multiple nearest taxi stops
     */
    @PostMapping("/nearest-stops")
    public List<Map<String, Object>> getNearestTaxiStops(@RequestBody JsonNode request) {
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
                "Distance", stop.getDistanceBetween(lat, lon),
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
        long startTime = System.currentTimeMillis();

        List<TaxiStop> stops = graph.getNearestTaxiStops(0, 0, Integer.MAX_VALUE);
        List<Map<String, Object>> response = new ArrayList<>();
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
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> response = new ArrayList<>();
        for (TaxiTrip trip : graph.getTaxiGraph().getTaxiTrips()) {
            response.add(Map.of(
                "from", trip.getDeparture().getName(),
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
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : responseMetrics.entrySet()) {
            List<Long> times = entry.getValue();
            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
            result.put(entry.getKey(), Map.of(
                "count", times.size(),
                "average_ms", avg,
                "all_times_ms", times
            ));
        }
        return result;
    }
}
