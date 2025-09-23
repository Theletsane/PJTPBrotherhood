package com.boolean_brotherhood.public_transportation_journey_planner;


import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

/**
 * Test class for Train Graph functionality with focus on stops without trips analysis
 */
public class TrainGraphStopsTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Train Graph Stops Analysis Test ===\n");
            
            // 1. Initialize the train graph
            System.out.println("1. Loading Train Graph...");
            TrainGraph trainGraph = new TrainGraph();
            trainGraph.loadTrainStops();
            trainGraph.LoadTrainTrips();
            System.out.println("   Train graph loaded successfully!\n");
            
            // 2. Display basic statistics
            displayGraphStatistics(trainGraph);
            
            // 3. Test stop loading and lookup
            testStopFunctionality(trainGraph);
            
            // 4. Test trip loading
            testTripFunctionality(trainGraph);
            
            // 5. Analyze stops without outgoing trips (main focus)
            analyzeStopsWithoutTrips(trainGraph);
            
            // 6. Test journey planning with available stops
            testJourneyPlanning(trainGraph);
            
            // 7. Test nearest stop functionality
            testNearestStop(trainGraph);
            
        } catch (IOException e) {
            System.err.println("Error initializing Train Graph: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Display basic train graph statistics
     */
    private static void displayGraphStatistics(TrainGraph trainGraph) {
        System.out.println("2. Train Graph Statistics:");
        Map<String, Object> metrics = trainGraph.getMetrics();
        
        // Display available metrics (adapt based on what TrainGraph.getMetrics() returns)
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            System.out.println("   " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println();
    }
    
    /**
     * Test stop loading and lookup functionality
     */
    private static void testStopFunctionality(TrainGraph trainGraph) {
        System.out.println("3. Testing Train Stop Functionality:");
        
        List<TrainStop> stops = trainGraph.getTrainStops();
        System.out.println("   Loaded " + stops.size() + " train stops");
        
        if (!stops.isEmpty()) {
            System.out.println("   First 3 train stops:");
            for (int i = 0; i < Math.min(3, stops.size()); i++) {
                TrainStop stop = stops.get(i);
                System.out.printf("   - %s (Code: %s) at (%.6f, %.6f)%n", 
                    stop.getName(), stop.getStopCode(), 
                    stop.getLatitude(), stop.getLongitude());
                System.out.printf("     Address: %s%n", stop.getAddress());
            }
            
            // Test stop lookup by name
            TrainStop firstStop = stops.get(0);
            TrainStop foundStop = trainGraph.getStopByName(firstStop.getName());
            if (foundStop != null) {
                System.out.println("   ✓ Stop lookup by name successful for: " + firstStop.getName());
            } else {
                System.out.println("   ✗ Stop lookup by name failed for: " + firstStop.getName());
            }
        }
        System.out.println();
    }
    
    /**
     * Test trip loading functionality
     */
    private static void testTripFunctionality(TrainGraph trainGraph) {
        System.out.println("4. Testing Train Trip Functionality:");
        
        List<TrainTrips> trips = trainGraph.getTrainTrips();
        System.out.println("   Loaded " + trips.size() + " train trips");
        
        if (!trips.isEmpty()) {
            System.out.println("   First 3 train trips:");
            for (int i = 0; i < Math.min(3, trips.size()); i++) {
                TrainTrips trip = trips.get(i);
                System.out.printf("   - Route %s: %s → %s at %s (%d min)%n",
                    trip.getRouteNumber(),
                    trip.getDepartureTrainStop().getName(),
                    trip.getDestinationTrainStop().getName(),
                    trip.getDepartureTime() != null ? trip.getDepartureTime().toString() : "N/A",
                    trip.getDuration());
            }
            
            // Test outgoing trips for a stop
            List<TrainStop> stops = trainGraph.getTrainStops();
            if (!stops.isEmpty()) {
                TrainStop testStop = stops.get(0);
                List<TrainTrips> outgoingTrips = trainGraph.getOutgoingTrips(testStop);
                System.out.printf("   Stop '%s' has %d outgoing trips%n", 
                    testStop.getName(), outgoingTrips.size());
            }
        }
        System.out.println();
    }
    
    /**
     * Main analysis: Find and analyze stops without outgoing trips
     */
    private static void analyzeStopsWithoutTrips(TrainGraph trainGraph) {
        System.out.println("5. Analyzing Train Stops Without Outgoing Trips:");
        
        List<TrainStop> allStops = trainGraph.getTrainStops();
        List<TrainTrips> allTrips = trainGraph.getTrainTrips();
        
        // Find stops with no outgoing trips
        List<TrainStop> stopsWithoutTrips = allStops.stream()
            .filter(stop -> trainGraph.getOutgoingTrips(stop).isEmpty())
            .collect(Collectors.toList());
        
        System.out.printf("   Found %d train stops without outgoing trips (out of %d total stops)%n", 
            stopsWithoutTrips.size(), allStops.size());
        
        if (!stopsWithoutTrips.isEmpty()) {
            double percentageWithoutTrips = (double) stopsWithoutTrips.size() / allStops.size() * 100;
            System.out.printf("   This represents %.1f%% of all train stops%n", percentageWithoutTrips);
            
            // Warning thresholds
            if (percentageWithoutTrips > 50) {
                System.out.println("   WARNING: More than 50% of train stops have no outgoing trips!");
                System.out.println("      This might indicate a major data loading issue.");
            } else if (percentageWithoutTrips > 20) {
                System.out.println("   CAUTION: High percentage of train stops without trips.");
                System.out.println("      Some might be valid terminus stations.");
            } else {
                System.out.println("   This percentage seems reasonable for a train network.");
            }
            
            // Display sample stops without trips
            System.out.println("\n   Train stops without outgoing trips:");
            int displayCount = Math.min(15, stopsWithoutTrips.size());
            for (int i = 0; i < displayCount; i++) {
                TrainStop stop = stopsWithoutTrips.get(i);
                System.out.printf("   - %-30s (Code: %-10s) at (%.6f, %.6f)%n",
                    stop.getName(), stop.getStopCode(),
                    stop.getLatitude(), stop.getLongitude());
            }
            
            if (stopsWithoutTrips.size() > displayCount) {
                System.out.printf("   ... and %d more%n", stopsWithoutTrips.size() - displayCount);
            }
            
            // Advanced analysis
            analyzeTrainStopUsage(trainGraph, stopsWithoutTrips, allTrips);
            
        } else {
            System.out.println("   ✓ All train stops have outgoing trips - excellent data consistency!");
        }
        System.out.println();
    }
    
    /**
     * Analyze how stops without outgoing trips are used in the network
     */
    private static void analyzeTrainStopUsage(TrainGraph trainGraph, List<TrainStop> stopsWithoutTrips, List<TrainTrips> allTrips) {
        System.out.println("\n   Analysis of train stops without outgoing trips:");
        
        // Check if these stops are used as destinations
        int stopsUsedAsDestinations = 0;
        int completelyUnusedStops = 0;
        
        for (TrainStop problemStop : stopsWithoutTrips) {
            boolean isDestination = allTrips.stream()
                .anyMatch(trip -> trip.getDestinationTrainStop().getName()
                    .equalsIgnoreCase(problemStop.getName()));
            
            if (isDestination) {
                stopsUsedAsDestinations++;
            } else {
                completelyUnusedStops++;
            }
        }
        
        System.out.printf("   - %d stops are used as destinations (likely terminus stations)%n", 
            stopsUsedAsDestinations);
        System.out.printf("   - %d stops appear to be completely unused%n", completelyUnusedStops);
        
        // Route analysis
        if (!trainGraph.routeNumbers.isEmpty()) {
            System.out.printf("   - Total routes in system: %d%n", trainGraph.routeNumbers.size());
            System.out.println("   - Sample routes: " + 
                trainGraph.routeNumbers.stream()
                    .limit(5)
                    .collect(Collectors.joining(", ")));
        }
        
        // Provide insights
        System.out.println("\n   Potential explanations:");
        if (stopsUsedAsDestinations > 0) {
            System.out.println("   → Stops used only as destinations are likely valid terminus stations");
        }
        if (completelyUnusedStops > 0) {
            System.out.println("   → Completely unused stops might indicate:");
            System.out.println("     * Stops from discontinued routes");
            System.out.println("     * Name mismatches between stops and trips data");
            System.out.println("     * Stations under construction/renovation");
            System.out.println("     * Data synchronization issues");
        }
        
        if (completelyUnusedStops > stopsUsedAsDestinations) {
            System.out.println("   ⚠️  Recommendation: Review data consistency between stops and trips files");
        }
    }
    
    /**
     * Test journey planning with available stops
     */
    private static void testJourneyPlanning(TrainGraph trainGraph) {
        System.out.println("6. Testing Train Journey Planning:");
        
        List<TrainStop> stops = trainGraph.getTrainStops();
        if (stops.size() < 2) {
            System.out.println("   Not enough stops for journey planning test");
            return;
        }
        
        // Find two stops that have outgoing trips
        TrainStop source = null, destination = null;
        for (TrainStop stop : stops) {
            if (!trainGraph.getOutgoingTrips(stop).isEmpty()) {
                if (source == null) {
                    source = stop;
                } else if (!stop.getName().equals(source.getName())) {
                    destination = stop;
                    break;
                }
            }
        }
        
        if (source != null && destination != null) {
            System.out.printf("   Testing journey: %s → %s%n", 
                source.getName(), destination.getName());
            
            LocalTime departureTime = LocalTime.of(8, 0); // 08:00
            TrainJourney journey = trainGraph.findEarliestArrival(
                source.getName(), 
                destination.getName(), 
                departureTime
            );
            
            if (journey != null && !journey.getTrips().isEmpty()) {
                System.out.println("   ✓ Journey found:");
                System.out.printf("     Departure: %s, Arrival: %s%n", 
                    journey.getDepartureTime(), journey.getArrivalTime());
                System.out.printf("     Duration: %d minutes, Transfers: %d%n",
                    journey.getTotalDurationMinutes(), journey.getNumberOfTransfers());
                System.out.printf("     Trip segments: %d%n", journey.getTrips().size());
                
                // Show trip details
                for (int i = 0; i < journey.getTrips().size(); i++) {
                    TrainTrips trip = journey.getTrips().get(i);
                    System.out.printf("     %d. Route %s: %s → %s%n", 
                        i + 1, trip.getRouteNumber(),
                        trip.getDepartureTrainStop().getName(),
                        trip.getDestinationTrainStop().getName());
                }
            } else {
                System.out.println("   ✗ No journey found between these stops");
            }
        } else {
            System.out.println("   Could not find suitable stops with outgoing trips for testing");
        }
        System.out.println();
    }
    
    /**
     * Test nearest stop functionality
     */
    private static void testNearestStop(TrainGraph trainGraph) {
        System.out.println("7. Testing Nearest Train Stop Functionality:");
        
        // Cape Town central coordinates
        double testLat = -33.9249;
        double testLon = 18.4241;
        
        TrainStop nearest = trainGraph.getNearestTrainStop(testLat, testLon);
        if (nearest != null) {
            double distance = nearest.distanceTo(testLat, testLon);
            System.out.printf("   Nearest train stop to (%.4f, %.4f):%n", testLat, testLon);
            System.out.printf("   - %s (Code: %s)%n", nearest.getName(), nearest.getStopCode());
            System.out.printf("   - Distance: %.2f km%n", distance);
            System.out.printf("   - Location: (%.6f, %.6f)%n", 
                nearest.getLatitude(), nearest.getLongitude());
            System.out.printf("   - Address: %s%n", nearest.getAddress());
            
            // Check if this stop has outgoing trips
            List<TrainTrips> outgoingTrips = trainGraph.getOutgoingTrips(nearest);
            System.out.printf("   - Outgoing trips: %d%n", outgoingTrips.size());
            
        } else {
            System.out.println("   ✗ No nearest train stop found");
        }
        System.out.println();
        
        System.out.println("=== Train Graph Analysis Complete ===");
    }
}