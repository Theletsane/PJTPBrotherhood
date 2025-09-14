package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    public TaxiController() {
        this.graph = new Graph();
        graph.LoadTaxiData(); // Load taxi data at startup
    }

    /**
     * Get the nearest taxi stop to a given location
     * POST /api/taxi/nearest-stop
     */
    @PostMapping("/nearest-stop")
    public Map<String, Object> getNearestTaxiStop(@RequestBody JsonNode request) {
        double lat = request.get("location").get("latitude").asDouble();
        double lon = request.get("location").get("longitude").asDouble();

        TaxiStop stop = graph.getNearestTaxiStart(lat, lon);

        if (stop == null) return Map.of("error", "No nearby taxi stop found.");

        return Map.of(
            "name", stop.getName(),
            "latitude", stop.getLatitude(),
            "longitude", stop.getLongitude()
        );
    }

    /**
     * Get multiple nearest taxi stops
     * POST /api/taxi/nearest-stops
     */
    @PostMapping("/nearest-stops")
    public List<Map<String, Object>> getNearestTaxiStops(@RequestBody JsonNode request) {
        double lat = request.get("location").get("latitude").asDouble();
        double lon = request.get("location").get("longitude").asDouble();
        int max = request.has("max") ? request.get("max").asInt() : 10;

        List<TaxiStop> stops = graph.getNearestTaxiStops(lat, lon, max);
        List<Map<String, Object>> response = new ArrayList<>();
        for (TaxiStop stop : stops) {
            response.add(Map.of(
                "name", stop.getName(),
                "latitude", stop.getLatitude(),
                "longitude", stop.getLongitude()
            ));
        }
        return response;
    }

    /**
     * List all taxi stops
     * GET /api/taxi/all-stops
     */
    @GetMapping("/all-stops")
    public List<Map<String, Object>> getAllTaxiStops() {
        List<TaxiStop> stops = graph.getNearestTaxiStops(0, 0, Integer.MAX_VALUE); // return all
        List<Map<String, Object>> response = new ArrayList<>();
        for (TaxiStop stop : stops) {
            response.add(Map.of(
                "name", stop.getName(),
                "latitude", stop.getLatitude(),
                "longitude", stop.getLongitude()
            ));
        }
        return response;
    }

    /**
     * List all taxi trips
     * GET /api/taxi/all-trips
     */
    @GetMapping("/all-trips")
    public List<Map<String, Object>> getAllTaxiTrips() {
        List<Map<String, Object>> response = new ArrayList<>();
        for (TaxiTrip trip : graph.getTaxiGraph().getTaxiTrips()) {
            response.add(Map.of(
                "from", trip.getDeparture().getName(),
                "to", trip.getDestinationStop().getName(),
                "duration", trip.getDuration()
            ));
        }
        return response;
    }
}
