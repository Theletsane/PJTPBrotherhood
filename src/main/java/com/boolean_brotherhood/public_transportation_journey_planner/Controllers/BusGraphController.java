

package com.boolean_brotherhood.public_transportation_journey_planner.Controllers;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
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

import com.boolean_brotherhood.public_transportation_journey_planner.BusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.System.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemLog;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

@RestController
@RequestMapping("/api/bus")
@CrossOrigin
public class BusGraphController {

    private final BusGraph busGraph;
    private final BusGraph.RaptorRouter raptorRouter;
    private final BusGraph.ConnectionScanRouter csaRouter;

    public BusGraphController(BusGraph busGraph) {
        this.busGraph = busGraph;
        this.raptorRouter = new BusGraph.RaptorRouter(this.busGraph.getAllBusStops(), this.busGraph.getAllBusTrips());
        this.csaRouter = new BusGraph.ConnectionScanRouter(this.busGraph.getAllBusStops(), this.busGraph.getAllBusTrips());
    }

    /** Get all bus stops (MyCiTi + Golden Arrow combined) */
    @GetMapping("/stops")
    public List<Map<String, Object>> getAllStops() {
        SystemLog.log_endpoint("/api/bus/stops");
        List<Map<String, Object>> stops = new ArrayList<>();
        
        for (Stop stop : busGraph.getAllBusStops()) {
            Map<String, Object> stopMap = new HashMap<>();
            stopMap.put("id", stop.getStopCode());
            stopMap.put("name", stop.getName());
            stopMap.put("latitude", stop.getLatitude());
            stopMap.put("longitude", stop.getLongitude());
            stopMap.put("address", stop.getAddress());
            
            // Add company info if it's a BusStop
            if (stop instanceof com.boolean_brotherhood.public_transportation_journey_planner.BusStop busStop) {
                stopMap.put("company", busStop.getCompany());
                stopMap.put("routes", busStop.getRouteCodes());
            }
            
            stops.add(stopMap);
        }
        
        return stops;
    }

    /** Get a single stop by name */
    @GetMapping("/stops/{name}")
    public Map<String, Object> getStopByName(@PathVariable String name) {
        SystemLog.log_endpoint("/api/bus/stops/{name}");
        
        Stop stop = busGraph.getAllBusStops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
                
        if (stop == null) {
            return Map.of("error", "Stop not found");
        }
        
        Map<String, Object> stopMap = new HashMap<>();
        stopMap.put("id", stop.getStopCode());
        stopMap.put("name", stop.getName());
        stopMap.put("latitude", stop.getLatitude());
        stopMap.put("longitude", stop.getLongitude());
        stopMap.put("address", stop.getAddress());
        
        if (stop instanceof com.boolean_brotherhood.public_transportation_journey_planner.BusStop busStop) {
            stopMap.put("company", busStop.getCompany());
            stopMap.put("routes", busStop.getRouteCodes());
        }
        
        return stopMap;
    }

    /** Plan a journey using RAPTOR with mini trips and full journey summary */
    @GetMapping("/journey")
    public Map<String, Object> planJourney(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String time,
            @RequestParam(defaultValue = "4") int maxRounds,
            @RequestParam(required = false) String day
    ) {
        SystemLog.log_endpoint("/api/bus/journey");
        
        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(time);
        } catch (Exception e) {
            return Map.of("error", "Invalid time format. Use HH:mm");
        }

        Trip.DayType dayType = parseDay(day);
        
        // Find source and target stops
        Stop source = findStopByName(from);
        Stop target = findStopByName(to);
        
        if (source == null) {
            return Map.of("error", "Source stop not found: " + from);
        }
        if (target == null) {
            return Map.of("error", "Target stop not found: " + to);
        }

        BusGraph.RouterResult result = raptorRouter.runRaptor(source, target, departureTime, maxRounds, dayType);
        
        if (result == null || result.trips.isEmpty()) {
            return Map.of("error", "No journey found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("from", source.getName());
        response.put("to", target.getName());
        response.put("departure", result.departure.toString());
        response.put("arrival", result.arrival.toString());
        response.put("durationMinutes", result.totalMinutes);
        response.put("transfers", result.transfers);
        response.put("algorithm", "RAPTOR");

        // Mini trips: each leg between stops
        List<Map<String, Object>> miniTrips = new ArrayList<>();
        for (Trip trip : result.trips) {
            Map<String, Object> miniTripMap = new HashMap<>();
            miniTripMap.put("from", trip.getDepartureStop().getName());
            miniTripMap.put("to", trip.getDestinationStop().getName());
            miniTripMap.put("departure", trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "immediate");
            miniTripMap.put("durationMinutes", trip.getDuration());
            miniTripMap.put("mode", trip.getMode());
            
            // Add route information if available
            if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip myCitiTrip) {
                miniTripMap.put("route", myCitiTrip.getRouteName());
                miniTripMap.put("tripId", myCitiTrip.getTripID());
                miniTripMap.put("company", "MyCiTi Bus");
            } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip gaTrip) {
                miniTripMap.put("route", gaTrip.getRouteName());
                miniTripMap.put("tripId", gaTrip.getTripID());
                miniTripMap.put("company", "Golden Arrow");
            }
            
            miniTrips.add(miniTripMap);
        }
        response.put("miniTrips", miniTrips);

        // Full journey summary
        Map<String, Object> fullJourney = new HashMap<>();
        fullJourney.put("totalDurationMinutes", result.totalMinutes);
        fullJourney.put("totalTransfers", result.transfers);
        fullJourney.put("path", result.trips.stream()
                .map(t -> t.getDepartureStop().getName() + " -> " + t.getDestinationStop().getName())
                .toList());
        response.put("fullJourney", fullJourney);

        return response;
    }

    /** Plan a journey using Connection Scan Algorithm (CSA) */
    @GetMapping("/journey/csa")
    public Map<String, Object> planJourneyCSA(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String time,
            @RequestParam(required = false) String day
    ) {
        SystemLog.log_endpoint("/api/bus/journey/csa");
        
        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(time);
        } catch (Exception e) {
            return Map.of("error", "Invalid time format. Use HH:mm");
        }

        Trip.DayType dayType = parseDay(day);
        
        Stop source = findStopByName(from);
        Stop target = findStopByName(to);
        
        if (source == null) {
            return Map.of("error", "Source stop not found: " + from);
        }
        if (target == null) {
            return Map.of("error", "Target stop not found: " + to);
        }

        BusGraph.RouterResult result = csaRouter.runConnectionScan(source, target, departureTime, dayType);
        
        if (result == null || result.trips.isEmpty()) {
            return Map.of("error", "No journey found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("from", source.getName());
        response.put("to", target.getName());
        response.put("departure", result.departure.toString());
        response.put("arrival", result.arrival.toString());
        response.put("durationMinutes", result.totalMinutes);
        response.put("transfers", result.transfers);
        response.put("algorithm", "Connection Scan Algorithm");

        // Mini trips
        List<Map<String, Object>> miniTrips = new ArrayList<>();
        for (Trip trip : result.trips) {
            Map<String, Object> miniTripMap = new HashMap<>();
            miniTripMap.put("from", trip.getDepartureStop().getName());
            miniTripMap.put("to", trip.getDestinationStop().getName());
            miniTripMap.put("departure", trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "immediate");
            miniTripMap.put("durationMinutes", trip.getDuration());
            miniTripMap.put("mode", trip.getMode());
            
            // Add route information
            if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip myCitiTrip) {
                miniTripMap.put("route", myCitiTrip.getRouteName());
                miniTripMap.put("company", "MyCiTi Bus");
            } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip gaTrip) {
                miniTripMap.put("route", gaTrip.getRouteName());
                miniTripMap.put("company", "Golden Arrow");
            }
            
            miniTrips.add(miniTripMap);
        }
        response.put("miniTrips", miniTrips);

        return response;
    }

    /** Compare RAPTOR vs CSA algorithms */
    @GetMapping("/journey/compare")
    public Map<String, Object> compareAlgorithms(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String time,
            @RequestParam(defaultValue = "4") int maxRounds,
            @RequestParam(required = false) String day
    ) {
        SystemLog.log_endpoint("/api/bus/journey/compare");
        
        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(time);
        } catch (Exception e) {
            return Map.of("error", "Invalid time format. Use HH:mm");
        }

        Trip.DayType dayType = parseDay(day);
        
        Stop source = findStopByName(from);
        Stop target = findStopByName(to);
        
        if (source == null || target == null) {
            return Map.of("error", "Source or target stop not found");
        }

        Map<String, Object> comparison = new HashMap<>();
        
        // Run RAPTOR
        long raptorStart = System.currentTimeMillis();
        BusGraph.RouterResult raptorResult = raptorRouter.runRaptor(source, target, departureTime, maxRounds, dayType);
        long raptorTime = System.currentTimeMillis() - raptorStart;
        
        // Run CSA
        long csaStart = System.currentTimeMillis();
        BusGraph.RouterResult csaResult = csaRouter.runConnectionScan(source, target, departureTime, dayType);
        long csaTime = System.currentTimeMillis() - csaStart;
        
        // RAPTOR results
        Map<String, Object> raptorData = new HashMap<>();
        if (raptorResult != null && !raptorResult.trips.isEmpty()) {
            raptorData.put("found", true);
            raptorData.put("durationMinutes", raptorResult.totalMinutes);
            raptorData.put("transfers", raptorResult.transfers);
            raptorData.put("trips", raptorResult.trips.size());
            raptorData.put("arrival", raptorResult.arrival.toString());
        } else {
            raptorData.put("found", false);
        }
        raptorData.put("computeTimeMs", raptorTime);
        comparison.put("raptor", raptorData);
        
        // CSA results
        Map<String, Object> csaData = new HashMap<>();
        if (csaResult != null && !csaResult.trips.isEmpty()) {
            csaData.put("found", true);
            csaData.put("durationMinutes", csaResult.totalMinutes);
            csaData.put("transfers", csaResult.transfers);
            csaData.put("trips", csaResult.trips.size());
            csaData.put("arrival", csaResult.arrival.toString());
        } else {
            csaData.put("found", false);
        }
        csaData.put("computeTimeMs", csaTime);
        comparison.put("csa", csaData);
        
        // Summary
        comparison.put("query", Map.of(
            "from", from,
            "to", to,
            "departure", time,
            "maxRounds", maxRounds,
            "dayType", dayType.toString()
        ));
        
        return comparison;
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        SystemLog.log_endpoint("/api/bus/metrics");
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        metrics.put("totalStops", busGraph.getAllBusStops().size());
        metrics.put("totalTrips", busGraph.getAllBusTrips().size());
        
        // Break down by company
        long myCitiStops = busGraph.getAllBusStops().stream()
                .filter(s -> s instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop)
                .count();
        long gaStops = busGraph.getAllBusStops().stream()
                .filter(s -> s instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop)
                .count();
                
        long myCitiTrips = busGraph.getAllBusTrips().stream()
                .filter(t -> t instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip)
                .count();
        long gaTrips = busGraph.getAllBusTrips().stream()
                .filter(t -> t instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip)
                .count();
        
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("myCitiStops", myCitiStops);
        breakdown.put("myCitiTrips", myCitiTrips);
        breakdown.put("gaStops", gaStops);
        breakdown.put("gaTrips", gaTrips);
        metrics.put("companyBreakdown", breakdown);
        
        // Add underlying graph metrics
        Map<String, Long> myCitiMetrics = busGraph.getMyCitiGraph().getMetrics();
        Map<String, Long> gaMetrics = busGraph.getGaGraph().getMetrics();
        
        metrics.put("myCitiGraphMetrics", myCitiMetrics);
        metrics.put("gaGraphMetrics", gaMetrics);
        
        metrics.put("algorithmsSupported", List.of("RAPTOR", "Connection Scan Algorithm"));
        
        return MetricsResponseBuilder.build("combinedBus", metrics, "/api/bus/");
    }

    /** Find the nearest stop given coordinates */
    @GetMapping("/nearest")
    public Map<String, Object> getNearestStop(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        SystemLog.log_endpoint("/api/bus/nearest");
        
        Stop nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Stop stop : busGraph.getAllBusStops()) {
            double distance = stop.distanceTo(lat, lon);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = stop;
            }
        }
        
        if (nearest == null) {
            return Map.of("error", "No stops found");
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", nearest.getStopCode());
        map.put("name", nearest.getName());
        map.put("latitude", nearest.getLatitude());
        map.put("longitude", nearest.getLongitude());
        map.put("address", nearest.getAddress());
        map.put("distance", minDistance);
        
        if (nearest instanceof com.boolean_brotherhood.public_transportation_journey_planner.BusStop busStop) {
            map.put("company", busStop.getCompany());
            map.put("routes", busStop.getRouteCodes());
        }
        
        return map;
    }

    /** Get all trips departing from a stop */
    @GetMapping("/stops/{name}/trips")
    public List<Map<String, Object>> getTripsFromStop(@PathVariable String name) {
        SystemLog.log_endpoint("/api/bus/stops/{name}/trips");
        
        Stop stop = findStopByName(name);
        if (stop == null) return Collections.emptyList();

        return stop.getTrips().stream().map(trip -> {
            Map<String, Object> map = new HashMap<>();
            map.put("from", trip.getDepartureStop().getName());
            map.put("to", trip.getDestinationStop().getName());
            map.put("departure", trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "immediate");
            map.put("durationMinutes", trip.getDuration());
            map.put("mode", trip.getMode());
            map.put("dayType", trip.getDayType());
            
            // Add route-specific info
            if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip myCitiTrip) {
                map.put("route", myCitiTrip.getRouteName());
                map.put("tripId", myCitiTrip.getTripID());
                map.put("company", "MyCiTi Bus");
            } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip gaTrip) {
                map.put("route", gaTrip.getRouteName());
                map.put("tripId", gaTrip.getTripID());
                map.put("company", "Golden Arrow");
            }
            
            return map;
        }).collect(Collectors.toList());
    }

    /** List all available routes */
    @GetMapping("/routes")
    public Map<String, Object> getAllRoutes() {
        SystemLog.log_endpoint("/api/bus/routes");
        
        Map<String, List<String>> routesByCompany = new HashMap<>();
        
        // Get MyCiTi routes
        List<String> myCitiRoutes = busGraph.getAllBusTrips().stream()
                .filter(t -> t instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip)
                .map(t -> ((com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip) t).getRouteName())
                .filter(route -> route != null && !route.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        routesByCompany.put("MyCiTi", myCitiRoutes);
        
        // Get GA routes
        List<String> gaRoutes = busGraph.getAllBusTrips().stream()
                .filter(t -> t instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip)
                .map(t -> ((com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip) t).getRouteName())
                .filter(route -> route != null && !route.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        routesByCompany.put("GoldenArrow", gaRoutes);
        
        Map<String, Object> response = new HashMap<>();
        response.put("routesByCompany", routesByCompany);
        response.put("totalRoutes", myCitiRoutes.size() + gaRoutes.size());
        response.put("companies", List.of("MyCiTi", "GoldenArrow"));
        
        return response;
    }

    // Helper methods
    private Stop findStopByName(String name) {
        if (name == null) return null;
        String searchName = name.trim().toUpperCase();
        
        return busGraph.getAllBusStops().stream()
                .filter(stop -> stop.getName() != null && stop.getName().trim().toUpperCase().equals(searchName))
                .findFirst()
                .orElse(null);
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