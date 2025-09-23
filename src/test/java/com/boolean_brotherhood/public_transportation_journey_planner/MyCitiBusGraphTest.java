package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;

/**
 * Test class for MyCiti Bus Graph functionality
 */
public class MyCitiBusGraphTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== MyCiti Bus Graph Test ===\n");
            
            // 1. Initialize the graph
            System.out.println("1. Loading MyCiti Graph...");
            MyCitiBusGraph graph = new MyCitiBusGraph();
            System.out.println("   Graph loaded successfully!\n");
            
            // 2. Display basic statistics
            displayGraphStatistics(graph);
            
            // 3. Test stop loading and lookup
            testStopFunctionality(graph);
            
            // 4. Test trip loading
            testTripFunctionality(graph);
            
            // 5. Test RAPTOR journey planning
            testJourneyPlanning(graph);
            
            // 6. Test nearest stop functionality
            testNearestStop(graph);
            
            // 7. Test for stops without outgoing trips
            testStopsWithoutTrips(graph);
            
            // 8. Display system logs
            displaySystemLogs(graph);
            
        } catch (IOException e) {
            System.err.println("Error initializing MyCiti Graph: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Display basic graph statistics
     */
    private static void displayGraphStatistics(MyCitiBusGraph graph) {
        System.out.println("2. Graph Statistics:");
        Map<String, Long> metrics = graph.getMetrics();
        
        System.out.println("   Total Stops: " + metrics.get("totalStops"));
        System.out.println("   Total Trips: " + metrics.get("totalTrips"));
        System.out.println("   Stops Load Time: " + metrics.get("stopsLoadTimeMs") + " ms");
        System.out.println("   Trips Load Time: " + metrics.get("tripsLoadTimeMs") + " ms");
        System.out.println("   Malformed Stop Lines: " + metrics.get("malformedStopLines"));
        System.out.println("   Missing Trip Stops: " + metrics.get("missingTripStops"));
        System.out.println();
    }
    
    /**
     * Test stop loading and lookup functionality
     */
    private static void testStopFunctionality(MyCitiBusGraph graph) {
        System.out.println("3. Testing Stop Functionality:");
        
        List<MyCitiStop> stops = graph.getMyCitiStops();
        System.out.println("   Loaded " + stops.size() + " stops");
        
        if (!stops.isEmpty()) {
            System.out.println("   First 3 stops:");
            for (int i = 0; i < Math.min(3, stops.size()); i++) {
                MyCitiStop stop = stops.get(i);
                System.out.printf("   - %s (Code: %s) at (%.6f, %.6f)%n", 
                    stop.getName(), stop.getStopCode(), 
                    stop.getLatitude(), stop.getLongitude());
            }
            
            // Test stop lookup
            MyCitiStop firstStop = stops.get(0);
            MyCitiStop foundStop = graph.findStop(firstStop.getName());
            if (foundStop != null) {
                System.out.println("   ✓ Stop lookup successful for: " + firstStop.getName());
            } else {
                System.out.println("   ✗ Stop lookup failed for: " + firstStop.getName());
            }
        }
        System.out.println();
    }
    
    /**
     * Test trip loading functionality
     */
    private static void testTripFunctionality(MyCitiBusGraph graph) {
        System.out.println("4. Testing Trip Functionality:");
        
        List<MyCitiTrip> trips = graph.getMyCitiTrips();
        System.out.println("   Loaded " + trips.size() + " trips");
        
        if (!trips.isEmpty()) {
            System.out.println("   First 3 trips:");
            for (int i = 0; i < Math.min(3, trips.size()); i++) {
                MyCitiTrip trip = trips.get(i);
                System.out.printf("   - Route %s: %s → %s at %s (%d min)%n",
                    trip.getRouteName(),
                    trip.getDepartureMyCitiStop().getName(),
                    trip.getDestinationMyCitiStop().getName(),
                    trip.getDepartureTime(),
                    trip.getDuration());
            }
            
            // Test trips per stop
            List<MyCitiStop> stops = graph.getMyCitiStops();
            if (!stops.isEmpty()) {
                MyCitiStop testStop = stops.get(0);
                List<MyCitiTrip> stopsTrips = testStop.getMyCitiTrips();
                System.out.printf("   Stop '%s' has %d departing trips%n", 
                    testStop.getName(), stopsTrips.size());
            }
        }
        System.out.println();
    }
    
    /**
     * Test RAPTOR journey planning
     */
    private static void testJourneyPlanning(MyCitiBusGraph graph) {
        System.out.println("5. Testing Journey Planning (RAPTOR):");
        
        List<MyCitiStop> stops = graph.getMyCitiStops();
        if (stops.size() < 2) {
            System.out.println("   Not enough stops for journey planning test");
            return;
        }
        
        MyCitiBusGraph.MyCitiRaptor raptor = new MyCitiBusGraph.MyCitiRaptor(graph);
        
        // Test with first few stops that have different names
        MyCitiStop source = null, destination = null;
        for (int i = 0; i < stops.size() && (source == null || destination == null); i++) {
            MyCitiStop stop = stops.get(i);
            if (source == null) {
                source = stop;
            } else if (!stop.getName().equals(source.getName())) {
                destination = stop;
            }
        }
        
        if (source != null && destination != null) {
            System.out.printf("   Testing journey: %s → %s%n", 
                source.getName(), destination.getName());
            
            LocalTime departureTime = LocalTime.of(8, 0); // 08:00
            MyCitiBusJourney journey = raptor.runRaptor(
                source.getName(), 
                destination.getName(), 
                departureTime, 
                4
            );
            
            if (journey != null && !journey.getTrips().isEmpty()) {
                System.out.println("   ✓ Journey found:");
                System.out.printf("     Departure: %s, Arrival: %s%n", 
                    journey.getDepartureTime(), journey.getArrivalTime());
                System.out.printf("     Duration: %d minutes, Transfers: %d%n",
                    journey.getTotalDurationMinutes(), journey.getNumberOfTransfers());
                System.out.printf("     Trips: %d%n", journey.getTrips().size());
                
                // Show trip details
                for (int i = 0; i < journey.getTrips().size(); i++) {
                    MyCitiTrip trip = journey.getTrips().get(i);
                    System.out.printf("     %d. Route %s: %s → %s%n", 
                        i + 1, trip.getRouteName(),
                        trip.getDepartureMyCitiStop().getName(),
                        trip.getDestinationMyCitiStop().getName());
                }
            } else {
                System.out.println("   ✗ No journey found");
            }
        } else {
            System.out.println("   Could not find suitable source/destination stops");
        }
        System.out.println();
    }
    
    /**
     * Test nearest stop functionality
     */
    private static void testNearestStop(MyCitiBusGraph graph) {
        System.out.println("6. Testing Nearest Stop Functionality:");
        
        // Cape Town city center coordinates
        double testLat = -33.9249;
        double testLon = 18.4241;
        
        MyCitiStop nearest = graph.getNearestMyCitiStop(testLat, testLon);
        if (nearest != null) {
            double distance = nearest.distanceTo(testLat, testLon);
            System.out.printf("   Nearest stop to (%.4f, %.4f):%n", testLat, testLon);
            System.out.printf("   - %s (Code: %s)%n", nearest.getName(), nearest.getStopCode());
            System.out.printf("   - Distance: %.2f km%n", distance);
            System.out.printf("   - Location: (%.6f, %.6f)%n", 
                nearest.getLatitude(), nearest.getLongitude());
        } else {
            System.out.println("   ✗ No nearest stop found");
        }
        System.out.println();
    }
    
    /**
     * Test for stops without outgoing trips (potential data issues)
     */
    private static void testStopsWithoutTrips(MyCitiBusGraph graph) {
        System.out.println("7. Testing for Stops Without Outgoing Trips:");
        
        List<MyCitiStop> stops = graph.getMyCitiStops();
        List<MyCitiStop> stopsWithoutTrips = stops.stream()
            .filter(stop -> stop.getMyCitiTrips().isEmpty())
            .toList();
        
        System.out.printf("   Found %d stops without outgoing trips (out of %d total stops)%n", 
            stopsWithoutTrips.size(), stops.size());
        
        if (!stopsWithoutTrips.isEmpty()) {
            double percentageWithoutTrips = (double) stopsWithoutTrips.size() / stops.size() * 100;
            System.out.printf("   This represents %.1f%% of all stops%n", percentageWithoutTrips);
            
            if (percentageWithoutTrips > 50) {
                System.out.println("   ⚠️  WARNING: More than 50% of stops have no outgoing trips!");
                System.out.println("      This might indicate a data loading issue.");
            } else if (percentageWithoutTrips > 20) {
                System.out.println("   ⚠️  CAUTION: High percentage of stops without trips.");
                System.out.println("      This might be expected for destination-only stops.");
            }
            
            System.out.println("   Stops without outgoing trips:");
            int displayCount = Math.min(10, stopsWithoutTrips.size());
            for (int i = 0; i < displayCount; i++) {
                MyCitiStop stop = stopsWithoutTrips.get(i);
                System.out.printf("   - %s (Code: %s) at (%.6f, %.6f)%n",
                    stop.getName(), stop.getStopCode(),
                    stop.getLatitude(), stop.getLongitude());
            }
            
            if (stopsWithoutTrips.size() > displayCount) {
                System.out.printf("   ... and %d more%n", stopsWithoutTrips.size() - displayCount);
            }
            
            // Analyze potential causes
            analyzeStopsWithoutTrips(graph, stopsWithoutTrips);
        } else {
            System.out.println("   ✓ All stops have outgoing trips - excellent data consistency!");
        }
        System.out.println();
    }
    
    /**
     * Analyze potential causes for stops without trips
     */
    private static void analyzeStopsWithoutTrips(MyCitiBusGraph graph, List<MyCitiStop> problematicStops) {
        System.out.println("   Analysis of stops without trips:");
        
        // Check if these stops appear as destinations in other trips
        List<MyCitiTrip> allTrips = graph.getMyCitiTrips();
        int stopsUsedAsDestinations = 0;
        
        for (MyCitiStop problemStop : problematicStops) {
            boolean isDestination = allTrips.stream()
                .anyMatch(trip -> trip.getDestinationMyCitiStop().equals(problemStop));
            
            if (isDestination) {
                stopsUsedAsDestinations++;
            }
        }
        
        System.out.printf("   - %d stops are used as destinations in other trips%n", stopsUsedAsDestinations);
        System.out.printf("   - %d stops appear to be completely unused%n", 
            problematicStops.size() - stopsUsedAsDestinations);
        
        if (stopsUsedAsDestinations > 0) {
            System.out.println("   → Stops used only as destinations might be terminus stations");
        }
        
        if (problematicStops.size() - stopsUsedAsDestinations > 0) {
            System.out.println("   → Completely unused stops might indicate:");
            System.out.println("     * Stops from routes not included in trip data");
            System.out.println("     * Stops with name mismatches between stops and trips files");
            System.out.println("     * Inactive/discontinued stops still in the stops file");
        }
    }
    private static void displaySystemLogs(MyCitiBusGraph graph) {
        System.out.println("7. System Logs:");
        List<String> logs = graph.getLogs();
        
        if (logs.isEmpty()) {
            System.out.println("   No logs available");
        } else {
            System.out.println("   Recent logs:");
            int logCount = Math.min(5, logs.size());
            for (int i = logs.size() - logCount; i < logs.size(); i++) {
                System.out.println("   " + logs.get(i));
            }
        }
        System.out.println();
        
        System.out.println("=== Test Complete ===");
    }
}