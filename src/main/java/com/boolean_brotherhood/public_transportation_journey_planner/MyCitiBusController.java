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

import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

@RestController
@RequestMapping("/myciti")
public class MyCitiBusController {

    private final MyCitiBusGraph graph;
    private final MyCitiBusGraph.MyCitiRaptor raptor;

    public MyCitiBusController(MyCitiBusGraph graph) {
        this.graph = graph;
        this.raptor = new MyCitiBusGraph.MyCitiRaptor(graph);
    }

    /**
     * Get system metrics (stops, trips, load times, etc.)
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Map<String, Long> MyCitimetrics = graph.getMetrics();
        for(String key: MyCitimetrics.keySet()){
            metrics.put(key, MyCitimetrics.get(key));
        }
        return metrics;
    }

    /**
     * Get all stops
     */
    @GetMapping("/stops")
    public List<MyCitiStop> getStops() {
        return graph.getMyCitiStops();
    }

    /**
     * Get all trips
     */
    @GetMapping("/trips")
    public List<MyCitiTrip> getTrips() {
        return graph.getMyCitiTrips();
    }

    @GetMapping("/logs")
    public List<String> getLogs() {
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
            @RequestParam(defaultValue = "4") int maxRounds) {

        LocalTime departureTime;
        try {
            departureTime = LocalTime.parse(departure);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid departure time format. Use HH:mm");
            return error;
        }

        MyCitiBusJourney journey = raptor.runRaptor(source, target, departureTime, maxRounds);

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



}
