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

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.Graph;
import com.boolean_brotherhood.public_transportation_journey_planner.Graph.Mode;
import com.boolean_brotherhood.public_transportation_journey_planner.Journey;
import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.System.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip.DayType;
import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.RouteCoordinateExtractor;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin
public class GraphController {

    private final Graph graph;
    private final RouteCoordinateExtractor routeExtractor;

    public GraphController(Graph graph) throws IOException {
        this.graph = graph;
        this.routeExtractor = new RouteCoordinateExtractor();
        
        // Initialize route extractor
        try {
            routeExtractor.loadRailwayGeoJson();
        } catch (IOException e) {
            System.err.println("Warning: Could not load railway GeoJSON data: " + e.getMessage());
        }
    }

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

        // Create Journey object to get proper transfer calculation
        Stop source = graph.findStopByName(from);
        Stop destination = graph.findStopByName(to);
        
        // Calculate arrival time
        LocalTime arrivalTime = departureTime;
        for (Trip trip : path) {
            if (arrivalTime != null) {
                arrivalTime = arrivalTime.plusMinutes(trip.getDuration());
            }
        }
        
        Journey journey = new Journey(source, destination, departureTime, arrivalTime, path);

        Map<String, Object> response = new HashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("departure", journey.getDepartureTime().toString());
        response.put("arrival", journey.getArrivalTime().toString());
        response.put("durationMinutes", journey.getTotalDurationMinutes());
        response.put("transfers", journey.getNumberOfTransfers());

        // Add start and end stop coordinates
        if (source != null) {
            Map<String, Object> startStopCoords = new HashMap<>();
            startStopCoords.put("name", source.getName());
            startStopCoords.put("latitude", source.getLatitude());
            startStopCoords.put("longitude", source.getLongitude());
            if (source.getAddress() != null && !source.getAddress().isEmpty()) {
                startStopCoords.put("address", source.getAddress());
            }
            response.put("startStopCoordinates", startStopCoords);
        }
        
        if (destination != null) {
            Map<String, Object> endStopCoords = new HashMap<>();
            endStopCoords.put("name", destination.getName());
            endStopCoords.put("latitude", destination.getLatitude());
            endStopCoords.put("longitude", destination.getLongitude());
            if (destination.getAddress() != null && !destination.getAddress().isEmpty()) {
                endStopCoords.put("address", destination.getAddress());
            }
            response.put("endStopCoordinates", endStopCoords);
        }

        List<Map<String, Object>> miniTrips = new ArrayList<>();
        for (Trip t : path) {
            Map<String, Object> tripMap = new HashMap<>();
            tripMap.put("from", t.getDepartureStop().getName());
            tripMap.put("to", t.getDestinationStop().getName());
            tripMap.put("departure", t.getDepartureTime() != null ? t.getDepartureTime().toString() : null);
            tripMap.put("durationMinutes", t.getDuration());
            tripMap.put("mode", getEffectiveMode(t));
            
            // Add route information if available
            String route = getRouteNumber(t);
            if (route != null) {
                tripMap.put("route", route);
            }
            
            // Add start and end stop coordinates for each trip segment
            addTripStopCoordinates(tripMap, t);
            
            // Add additional coordinates based on trip type
            if (isTrainTrip(t)) {
                addTrainCoordinates(tripMap, t);
            } else if (isWalkingTrip(t)) {
                addWalkingDistanceInfo(tripMap, t);
            }
            
            miniTrips.add(tripMap);
        }
        response.put("trips", miniTrips);
        
        // Add journey summary similar to TrainJourney toString
        response.put("summary", buildJourneySummary(journey));
        
        return response;
    }

    /** Plan a journey with coordinates (enhanced endpoint) */
    @GetMapping("/journey/with-coordinates")
    public Map<String, Object> planJourneyWithCoordinates(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String time,
            @RequestParam(required = false) List<String> modes,
            @RequestParam(required = false) String day) {
        
        // This is essentially the same as the regular journey endpoint now
        // since we've enhanced it to include coordinates for all trips
        return planJourney(from, to, time, modes, day);
    }

    /** Helper: Add start and end stop coordinates for individual trip segments */
    private void addTripStopCoordinates(Map<String, Object> tripMap, Trip trip) {
        Stop fromStop = trip.getDepartureStop();
        Stop toStop = trip.getDestinationStop();
        
        // Add departure stop coordinates
        if (fromStop != null) {
            Map<String, Object> fromCoords = new HashMap<>();
            fromCoords.put("name", fromStop.getName());
            fromCoords.put("latitude", fromStop.getLatitude());
            fromCoords.put("longitude", fromStop.getLongitude());
            if (fromStop.getAddress() != null && !fromStop.getAddress().isEmpty()) {
                fromCoords.put("address", fromStop.getAddress());
            }
            tripMap.put("fromStopCoordinates", fromCoords);
        } else {
            tripMap.put("fromStopCoordinates", null);
        }
        
        // Add destination stop coordinates
        if (toStop != null) {
            Map<String, Object> toCoords = new HashMap<>();
            toCoords.put("name", toStop.getName());
            toCoords.put("latitude", toStop.getLatitude());
            toCoords.put("longitude", toStop.getLongitude());
            if (toStop.getAddress() != null && !toStop.getAddress().isEmpty()) {
                toCoords.put("address", toStop.getAddress());
            }
            tripMap.put("toStopCoordinates", toCoords);
        } else {
            tripMap.put("toStopCoordinates", null);
        }
        
        // Calculate straight-line distance between stops
        if (fromStop != null && toStop != null) {
            double distance = calculateDistance(
                fromStop.getLatitude(), fromStop.getLongitude(),
                toStop.getLatitude(), toStop.getLongitude()
            );
            tripMap.put("straightLineDistance", Math.round(distance * 100.0) / 100.0);
        } else {
            tripMap.put("straightLineDistance", 0.0);
        }
    }

    /** Helper: Add walking-specific distance information */
    private void addWalkingDistanceInfo(Map<String, Object> tripMap, Trip trip) {
        Stop fromStop = trip.getDepartureStop();
        Stop toStop = trip.getDestinationStop();
        
        if (fromStop != null && toStop != null) {
            double distance = calculateDistance(
                fromStop.getLatitude(), fromStop.getLongitude(),
                toStop.getLatitude(), toStop.getLongitude()
            );
            
            // Estimate walking time based on average walking speed (5 km/h)
            double estimatedWalkingMinutes = (distance / 5.0) * 60.0;
            tripMap.put("estimatedWalkingTime", Math.round(estimatedWalkingMinutes * 100.0) / 100.0);
            
            // Add walking path type indicator
            tripMap.put("walkingType", "direct");
        }
    }

    /** Helper: Check if trip is a train trip */
    private boolean isTrainTrip(Trip trip) {
        return trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;
    }

    /** Helper: Add train coordinates to trip map */
    private void addTrainCoordinates(Map<String, Object> tripMap, Trip trip) {
        try {
            if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) {
                com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips trainTrip = 
                    (com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) trip;
                
                RouteCoordinateExtractor.RouteSegment segment = routeExtractor.getRouteCoordinates(trainTrip);
                
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
            }
        } catch (IOException e) {
            // Fallback to empty coordinates on error
            tripMap.put("coordinates", "[]");
            tripMap.put("coordinateCount", 0);
            tripMap.put("segmentDistance", 0);
            tripMap.put("routeName", "Unknown");
            tripMap.put("coordinateError", "Could not load coordinates");
        }
    }

    /** Helper: Calculate distance between two points using Haversine formula */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** Helper: Check if trip is a walking trip */
    private boolean isWalkingTrip(Trip trip) {
        String mode = getEffectiveMode(trip);
        return "WALKING".equals(mode) || "Walking".equals(mode) || 
               (trip.getMode() != null && trip.getMode().toUpperCase().contains("WALKING"));
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
            return EnumSet.of(Mode.TRAIN, Mode.MYCITI, Mode.GA, Mode.WALKING); // Default multimodal
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

    /** Helper: Get effective mode of a trip */
    private String getEffectiveMode(Trip trip) {
        if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) {
            return "TRAIN";
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip) {
            return "MYCITI";
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip) {
            return "GA";
        } else {
            return trip.getMode();
        }
    }

    /** Helper: Get route number from a trip if available */
    private String getRouteNumber(Trip trip) {
        if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) {
            return ((com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) trip).getRouteNumber();
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip) {
            return ((com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip) trip).getRouteName();
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip) {
            return ((com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip) trip).getRouteName();
        }
        return null;
    }

    /** Helper: Build journey summary similar to TrainJourney */
    private String buildJourneySummary(Journey journey) {
        StringBuilder sb = new StringBuilder();
        sb.append("Journey from ").append(journey.getSource().getName())
          .append(" to ").append(journey.getDestination().getName()).append("\n");
        sb.append("Departure: ").append(journey.getDepartureTime())
          .append(" | Arrival: ").append(journey.getArrivalTime())
          .append(" | Duration: ").append(journey.getTotalDurationMinutes()).append(" minutes\n");
        sb.append("Transfers: ").append(journey.getNumberOfTransfers()).append("\n");
        sb.append("---- Trips ----\n");

        List<Trip> trips = journey.getTrips();
        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            sb.append(i + 1).append(". [").append(getEffectiveMode(trip));
            String route = getRouteNumber(trip);
            if (route != null) {
                sb.append(" - ").append(route);
            }
            sb.append("] ");
            sb.append(trip.getDepartureStop().getName())
              .append(" -> ").append(trip.getDestinationStop().getName())
              .append(" | Dep: ").append(trip.getDepartureTime())
              .append(" | Dur: ").append(trip.getDuration()).append(" min\n");
        }
        return sb.toString();
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
            tripMap.put("departure", t.getDepartureTime() != null ? t.getDepartureTime().toString() : null);
            tripMap.put("durationMinutes", t.getDuration());
            tripMap.put("mode", getEffectiveMode(t));
            
            String route = getRouteNumber(t);
            if (route != null) {
                tripMap.put("route", route);
            }
            
            // Add trip stop coordinates for individual trip segments
            addTripStopCoordinates(tripMap, t);
            
            // Add additional coordinates based on trip type
            if (isTrainTrip(t)) {
                addTrainCoordinates(tripMap, t);
            } else if (isWalkingTrip(t)) {
                addWalkingDistanceInfo(tripMap, t);
            }
            
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