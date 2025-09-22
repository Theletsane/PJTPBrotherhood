package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph.TrainRaptor;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/train")
public class TrainController {

    private final TrainGraph trainGraph;
    private final TrainRaptor raptor;

    public TrainController(TrainGraph graph) {
        this.trainGraph = graph;
        this.raptor = new TrainGraph.TrainRaptor(graph);
    }

    /** Get all train stops */
    @GetMapping("/stops")
    public List<Map<String, Object>> getAllStops() {
        List<Map<String, Object>> stops = new ArrayList<>();
        for (TrainStop stop : trainGraph.getTrainStops()) {
            Map<String, Object> stopMap = new HashMap<>();
            stopMap.put("id", stop.getStopCode());
            stopMap.put("name", stop.getName());
            stopMap.put("latitude", stop.getLatitude());
            stopMap.put("longitude", stop.getLongitude());
            stopMap.put("address", stop.getAddress());
            stops.add(stopMap);
        }
        return stops;
    }

    /** Get a single stop by name */
    @GetMapping("/stops/{name}")
    public Map<String, Object> getStopByName(@PathVariable String name) {
        TrainStop stop = trainGraph.getStopByName(name);
        if (stop == null) {
            return Map.of("error", "Stop not found");
        }
        Map<String, Object> stopMap = new HashMap<>();
        stopMap.put("id", stop.getStopCode());
        stopMap.put("name", stop.getName());
        stopMap.put("latitude", stop.getLatitude());
        stopMap.put("longitude", stop.getLongitude());
        stopMap.put("address", stop.getAddress());
        return stopMap;
    }

    /** Plan a journey using RAPTOR with mini trips and full journey summary */
    @GetMapping("/journey")
    public Map<String, Object> planJourney(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String time
    ) {
        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(time);
        } catch (Exception e) {
            return Map.of("error", "Invalid time format. Use HH:mm");
        }

        TrainJourney journey = trainGraph.findEarliestArrival(from, to, departureTime);
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
        for (TrainTrips trip : journey.getTrips()) {
            Map<String, Object> miniTripMap = new HashMap<>();
            miniTripMap.put("tripId", trip.getTripID());
            miniTripMap.put("route", trip.getRouteNumber());
            miniTripMap.put("from", trip.getDepartureTrainStop().getName());
            miniTripMap.put("to", trip.getDestinationTrainStop().getName());
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
                .map(t -> t.getDepartureTrainStop().getName() + " -> " + t.getDestinationTrainStop().getName())
                .toList());
        response.put("fullJourney", fullJourney);

        return response;
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return trainGraph.getMetrics();
    }


    /** Get all trips departing from a stop */
    @GetMapping("/stops/{name}/trips")
    public List<Map<String, Object>> getTripsFromStop(@PathVariable String name) {
        TrainStop stop = trainGraph.getStopByName(name);
        if (stop == null) return Collections.emptyList();

        return trainGraph.getOutgoingTrips(stop).stream().map(trip -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tripId", trip.getTripID());
            map.put("route", trip.getRouteNumber());
            map.put("departure", trip.getDepartureTime().toString());
            map.put("durationMinutes", trip.getDuration());
            map.put("destination", trip.getDestinationTrainStop().getName());
            return map;
        }).collect(Collectors.toList());
    }

    /** Find the nearest stop given coordinates */
    @GetMapping("/nearest")
    public Map<String, Object> getNearestStop(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        TrainStop nearest = trainGraph.getNearestTrainStop(lat, lon);
        if (nearest == null) return Map.of("error", "No stops found");
        Map<String, Object> map = new HashMap<>();
        map.put("id", nearest.getStopCode());
        map.put("name", nearest.getName());
        map.put("latitude", nearest.getLatitude());
        map.put("longitude", nearest.getLongitude());
        map.put("address", nearest.getAddress());
        return map;
    }

    /** List all route numbers */
    @GetMapping("/routes")
    public List<String> getAllRoutes() {
        return trainGraph.routeNumbers;
    }
}
