package com.boolean_brotherhood.public_transportation_journey_planner.Controllers;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.RouteCoordinateExtractor;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph.TrainRaptor;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

@RestController
@RequestMapping("/api/train")

public class TrainController {

    private final TrainGraph trainGraph;
    private final TrainRaptor raptor;
    private final RouteCoordinateExtractor routeExtractor;

    public TrainController(TrainGraph graph) {
        this.trainGraph = graph;
        this.raptor = new TrainGraph.TrainRaptor(graph);
        this.routeExtractor = new RouteCoordinateExtractor();
        
        // Initialize route extractor
        try {
            routeExtractor.loadRailwayGeoJson();
        } catch (IOException e) {
            System.err.println("Warning: Could not load railway GeoJSON data: " + e.getMessage());
        }
    }

    /** Get all train stops */
    @GetMapping("/stops")
    public List<Map<String, Object>> getAllStops() {
        List<Map<String, Object>> stopMaps = new ArrayList<>();
        List<TrainStop> stops = trainGraph.getTrainStops();
        stops.sort(Comparator.comparing(TrainStop::getName, String.CASE_INSENSITIVE_ORDER));
        for (TrainStop stop : stops) {
            Map<String, Object> stopMap = new HashMap<>();
            stopMap.put("name", stop.getName());
            stopMap.put("latitude", stop.getLatitude());
            stopMap.put("longitude", stop.getLongitude());
            stopMaps.add(stopMap);
        }
        return stopMaps;
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

    /** Plan a journey with route coordinates for mapping */
    @GetMapping("/journey/with-coordinates")
    public Map<String, Object> planJourneyWithCoordinates(
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

        // Mini trips with coordinates
        List<Map<String, Object>> miniTripsWithCoordinates = new ArrayList<>();
        try {
            List<RouteCoordinateExtractor.RouteSegment> segments = routeExtractor.getJourneyRouteCoordinates(journey);
            
            for (int i = 0; i < journey.getTrips().size(); i++) {
                TrainTrips trip = journey.getTrips().get(i);
                RouteCoordinateExtractor.RouteSegment segment = i < segments.size() ? segments.get(i) : null;
                
                Map<String, Object> tripMap = new HashMap<>();
                tripMap.put("tripId", trip.getTripID());
                tripMap.put("route", trip.getRouteNumber());
                tripMap.put("from", trip.getDepartureTrainStop().getName());
                tripMap.put("to", trip.getDestinationTrainStop().getName());
                tripMap.put("departure", trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "N/A");
                tripMap.put("durationMinutes", trip.getDuration());
                
                if (segment != null && !segment.isEmpty()) {
                    tripMap.put("coordinates", routeExtractor.getRouteCoordinatesAsJson(segment));
                    tripMap.put("coordinateCount", segment.getCoordinates().size());
                    tripMap.put("segmentDistance", segment.getTotalDistance());
                    tripMap.put("routeName", segment.getRouteName());
                } else {
                    tripMap.put("coordinates", "[]");
                    tripMap.put("coordinateCount", 0);
                    tripMap.put("segmentDistance", 0);
                    tripMap.put("routeName", "Unknown");
                }
                
                miniTripsWithCoordinates.add(tripMap);
            }
        } catch (IOException e) {
            // Fallback to regular mini trips without coordinates
            for (TrainTrips trip : journey.getTrips()) {
                Map<String, Object> tripMap = new HashMap<>();
                tripMap.put("tripId", trip.getTripID());
                tripMap.put("route", trip.getRouteNumber());
                tripMap.put("from", trip.getDepartureTrainStop().getName());
                tripMap.put("to", trip.getDestinationTrainStop().getName());
                tripMap.put("departure", trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "N/A");
                tripMap.put("durationMinutes", trip.getDuration());
                tripMap.put("coordinates", "[]");
                tripMap.put("coordinateCount", 0);
                tripMap.put("segmentDistance", 0);
                tripMap.put("routeName", "Unknown");
                tripMap.put("error", "Could not load coordinates");
                miniTripsWithCoordinates.add(tripMap);
            }
        }
        
        response.put("miniTrips", miniTripsWithCoordinates);
        return response;
    }

    /** Get route coordinates for a specific trip */
    @GetMapping("/trip/{tripId}/coordinates")
    public Map<String, Object> getTripCoordinates(@PathVariable String tripId) {
        // Find trip by ID
        TrainTrips trip = trainGraph.getTrainTrips().stream()
                .filter(t -> tripId.equals(t.getTripID()))
                .findFirst()
                .orElse(null);
                
        if (trip == null) {
            return Map.of("error", "Trip not found");
        }
        
        try {
            RouteCoordinateExtractor.RouteSegment segment = routeExtractor.getRouteCoordinates(trip);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tripId", trip.getTripID());
            response.put("route", trip.getRouteNumber());
            response.put("from", trip.getDepartureTrainStop().getName());
            response.put("to", trip.getDestinationTrainStop().getName());
            response.put("coordinates", routeExtractor.getRouteCoordinatesAsJson(segment));
            response.put("coordinateCount", segment.getCoordinates().size());
            response.put("distance", segment.getTotalDistance());
            response.put("routeName", segment.getRouteName());
            response.put("lineId", segment.getLineId());
            
            return response;
        } catch (IOException e) {
            return Map.of("error", "Could not load coordinates: " + e.getMessage());
        }
    }

    /** Get coordinates between two stops */
    @GetMapping("/coordinates")
    public Map<String, Object> getCoordinatesBetweenStops(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String route
    ) {
        TrainStop fromStop = trainGraph.getStopByName(from);
        TrainStop toStop = trainGraph.getStopByName(to);
        
        if (fromStop == null) {
            return Map.of("error", "From stop not found");
        }
        if (toStop == null) {
            return Map.of("error", "To stop not found");
        }
        
        String routeNumber = route != null ? route : 
                (trainGraph.routeNumbers.isEmpty() ? "Unknown" : trainGraph.routeNumbers.get(0));
        
        try {
            RouteCoordinateExtractor.RouteSegment segment = 
                    routeExtractor.getRouteCoordinates(fromStop, toStop, routeNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("from", fromStop.getName());
            response.put("to", toStop.getName());
            response.put("route", routeNumber);
            response.put("coordinates", routeExtractor.getRouteCoordinatesAsJson(segment));
            response.put("coordinateCount", segment.getCoordinates().size());
            response.put("distance", segment.getTotalDistance());
            response.put("routeName", segment.getRouteName());
            response.put("lineId", segment.getLineId());
            
            return response;
        } catch (IOException e) {
            return Map.of("error", "Could not load coordinates: " + e.getMessage());
        }
    }

    /** Get complete route coordinates for a specific route */
    @GetMapping("/routes/{routeName}/coordinates")
    public Map<String, Object> getCompleteRouteCoordinates(@PathVariable String routeName) {
        try {
            RouteCoordinateExtractor.RouteSegment segment = 
                    routeExtractor.getCompleteRouteCoordinates(routeName);
            
            if (segment.isEmpty()) {
                return Map.of("error", "Route not found or has no coordinates");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("routeName", segment.getRouteName());
            response.put("lineId", segment.getLineId());
            response.put("coordinates", routeExtractor.getRouteCoordinatesAsJson(segment));
            response.put("coordinateCount", segment.getCoordinates().size());
            response.put("totalDistance", segment.getTotalDistance());
            
            // Add first and last coordinates for reference
            List<RouteCoordinateExtractor.Coordinate> coords = segment.getCoordinates();
            if (!coords.isEmpty()) {
                RouteCoordinateExtractor.Coordinate first = coords.get(0);
                RouteCoordinateExtractor.Coordinate last = coords.get(coords.size() - 1);
                
                response.put("startPoint", Map.of(
                    "latitude", first.getLatitude(),
                    "longitude", first.getLongitude()
                ));
                response.put("endPoint", Map.of(
                    "latitude", last.getLatitude(),
                    "longitude", last.getLongitude()
                ));
            }
            
            return response;
        } catch (IOException e) {
            return Map.of("error", "Could not load route coordinates: " + e.getMessage());
        }
    }

    /** Get all available railway routes from GeoJSON */
    @GetMapping("/routes/available")
    public Map<String, Object> getAvailableRailwayRoutes() {
        try {
            List<String> routes = routeExtractor.getAvailableRoutes();
            Map<String, Object> response = new HashMap<>();
            response.put("routes", routes);
            response.put("count", routes.size());
            return response;
        } catch (IOException e) {
            return Map.of("error", "Could not load available routes: " + e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        trainGraph.getMetrics().forEach(metrics::put);
        metrics.put("stopCount", trainGraph.getTrainStops().size());
        metrics.put("tripCount", trainGraph.getTrainTrips().size());
        metrics.put("registeredRoutes", trainGraph.routeNumbers.size());
        return MetricsResponseBuilder.build("train", metrics, "/api/train/");
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

    /** Get all trips departing from a stop with coordinates */
    @GetMapping("/stops/{name}/trips/with-coordinates")
    public List<Map<String, Object>> getTripsFromStopWithCoordinates(@PathVariable String name) {
        TrainStop stop = trainGraph.getStopByName(name);
        if (stop == null) return Collections.emptyList();

        return trainGraph.getOutgoingTrips(stop).stream().map(trip -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tripId", trip.getTripID());
            map.put("route", trip.getRouteNumber());
            map.put("departure", trip.getDepartureTime().toString());
            map.put("durationMinutes", trip.getDuration());
            map.put("destination", trip.getDestinationTrainStop().getName());
            
            try {
                RouteCoordinateExtractor.RouteSegment segment = routeExtractor.getRouteCoordinates(trip);
                map.put("coordinates", routeExtractor.getRouteCoordinatesAsJson(segment));
                map.put("coordinateCount", segment.getCoordinates().size());
                map.put("distance", segment.getTotalDistance());
            } catch (IOException e) {
                map.put("coordinates", "[]");
                map.put("coordinateCount", 0);
                map.put("distance", 0);
                map.put("coordinateError", "Could not load coordinates");
            }
            
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