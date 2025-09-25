package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiTrip;

@RestController
@RequestMapping("/api/GA")
public class GABusController {
    private final GABusGraph graph;
    private final GABusGraph.GARaptor raptor;

    public GABusController(GABusGraph graph) {
        this.graph = graph;
        this.raptor = new GABusGraph.GARaptor(graph);
    }

    /**
     * Get system metrics (stops, trips, load times, etc.)
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        
        SystemLog.log_endpoint("/api/GA/metrics");  
        Map<String, Object> metrics = new HashMap<>();
        Map<String, Long> GAmetrics = graph.getMetrics();
        for(String key: GAmetrics.keySet()){
            metrics.put(key, GAmetrics.get(key));
        }
        return metrics;
    }

    /**
     * Get all stops
     */
    @GetMapping("/stops")
    public List<Map<String, Object>> getStops() {
        SystemLog.log_endpoint("/api/GA/stops");
  
        List<GAStop> stops = graph.getGAStops();
        List<Map<String, Object>> response = new ArrayList<>();
        for (GAStop stop : stops) {
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
        SystemLog.log_endpoint("/api/GA/trips");
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> response = new ArrayList<>();
        List<GATrip> trips = graph.getGATrips();
        for (GATrip trip : trips) {
            response.add(Map.of(
                "from", trip.getDepartureStop().getName(),
                "to", trip.getDestinationStop().getName(),
                "duration", trip.getDuration()
            ));
        }

        long elapsed = System.currentTimeMillis() - startTime;

        return response;
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
            @RequestParam(defaultValue = "4") int maxRounds) {
        
        SystemLog.log_endpoint("/api/GA/journey");
        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(departure);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid departure time format. Use HH:mm");
            return error;
        }

        GABusJourney journey = raptor.runRaptor(source, target, departureTime, maxRounds);

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
        for (GATrip trip : journey.getTrips()) {
            Map<String, Object> miniTripMap = new HashMap<>();
            miniTripMap.put("tripId", trip.getTripID());
            miniTripMap.put("route", trip.getRouteName());
            miniTripMap.put("from", trip.getDepartureGAStop().getName());
            miniTripMap.put("to", trip.getDestinationGAStop().getName());
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
                .map(t -> t.getDepartureGAStop().getName() + " -> " + t.getDestinationGAStop().getName())
                .toList());
        response.put("fullJourney", fullJourney);

        return response;
    }


    
}
