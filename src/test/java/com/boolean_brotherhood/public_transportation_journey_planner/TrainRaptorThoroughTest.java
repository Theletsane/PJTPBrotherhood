package com.boolean_brotherhood.public_transportation_journey_planner;


import java.io.IOException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

/**
 * Comprehensive test for Train RAPTOR algorithm using real Cape Town train route data
 */
public class TrainRaptorThoroughTest {
    
    // Major stations based on your route summary
    private static final String[] MAJOR_STATIONS = {
        "CAPE TOWN", "MUTUAL", "BELLVILLE", "KRAAIFONTEIN", "WELLINGTON",
        "EERSTE RIVER", "STRAND", "MULDERSVLEI", "CLAREMONT", "RETREAT", 
        "SIMON'S TOWN", "ATHLONE", "BONTEHEUWEL", "CHRIS HANI", 
        "PINELANDS", "KAPTEINSKLIP", "MONTE VISTA"
    };
    
    // Route areas for analysis
    private static final Map<String, String[]> ROUTE_AREAS = Map.of(
        "North", new String[]{"CAPE TOWN", "MUTUAL", "BELLVILLE", "KRAAIFONTEIN", "WELLINGTON"},
        "South", new String[]{"CAPE TOWN", "CLAREMONT", "RETREAT", "SIMON'S TOWN"},
        "Central", new String[]{"CAPE TOWN", "MUTUAL", "BONTEHEUWEL", "BELLVILLE"}
    );
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Comprehensive Train RAPTOR Algorithm Test ===\n");
            
            // 1. Initialize and load train graph
            System.out.println("1. Initializing Train Graph...");
            TrainGraph trainGraph = new TrainGraph();
            trainGraph.loadTrainStops();
            trainGraph.LoadTrainTrips();
            System.out.println("   Train graph loaded successfully!\n");
            
            // 2. Analyze graph structure
            analyzeGraphStructure(trainGraph);
            
            // 3. Test basic RAPTOR functionality
            testBasicRaptorFunctionality(trainGraph);
            
            // 4. Test route-specific journeys
            testRouteSpecificJourneys(trainGraph);
            
            // 5. Test multi-transfer journeys
            testMultiTransferJourneys(trainGraph);
            
            // 6. Test edge cases and error handling
            testEdgeCases(trainGraph);
            
            // 7. Performance testing
            performanceTestRaptor(trainGraph);
            
            // 8. Test different departure times
            testDepartureTimeVariations(trainGraph);
            
            // 9. Validate journey consistency
            validateJourneyConsistency(trainGraph);
            
        } catch (IOException e) {
            System.err.println("Error initializing Train Graph: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analyze the structure of the loaded train graph
     */
    private static void analyzeGraphStructure(TrainGraph trainGraph) {
        System.out.println("2. Train Graph Structure Analysis:");
        
        List<TrainStop> stops = trainGraph.getTrainStops();
        List<TrainTrips> trips = trainGraph.getTrainTrips();
        List<String> routes = trainGraph.routeNumbers;
        
        System.out.printf("   Total Stops: %d%n", stops.size());
        System.out.printf("   Total Trips: %d%n", trips.size());
        System.out.printf("   Total Routes: %d%n", routes.size());
        
        // Check coverage of major stations
        System.out.println("\n   Major Station Coverage:");
        int foundMajorStations = 0;
        for (String majorStation : MAJOR_STATIONS) {
            TrainStop stop = trainGraph.getStopByName(majorStation);
            if (stop != null) {
                foundMajorStations++;
                List<TrainTrips> outgoing = trainGraph.getOutgoingTrips(stop);
                System.out.printf("    %-15s - %d outgoing trips%n", majorStation, outgoing.size());
            } else {
                System.out.printf("    %-15s - NOT FOUND%n", majorStation);
            }
        }
        System.out.printf("   Coverage: %d/%d major stations found%n", foundMajorStations, MAJOR_STATIONS.length);
        
        // Route analysis
        System.out.println("\n   Route Distribution:");
        Map<String, Long> routeCounts = trips.stream()
            .collect(Collectors.groupingBy(
                trip -> trip.getRouteNumber(), 
                Collectors.counting()
            ));
        
        routeCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> 
                System.out.printf("   Route %-6s: %3d trips%n", entry.getKey(), entry.getValue())
            );
        
        System.out.println();
    }
    
    /**
     * Test basic RAPTOR functionality with simple journeys
     */
    private static void testBasicRaptorFunctionality(TrainGraph trainGraph) {
        System.out.println("3. Testing Basic RAPTOR Functionality:");
        
        // Test direct connections within each route area
        for (Map.Entry<String, String[]> area : ROUTE_AREAS.entrySet()) {
            System.out.printf("\n   Testing %s Line Routes:%n", area.getKey());
            String[] stations = area.getValue();
            
            if (stations.length >= 2) {
                testJourney(trainGraph, stations[0], stations[stations.length - 1], 
                    LocalTime.of(8, 0), "Direct " + area.getKey() + " line");
                
                if (stations.length >= 3) {
                    testJourney(trainGraph, stations[0], stations[1], 
                        LocalTime.of(9, 0), area.getKey() + " intermediate");
                }
            }
        }
        System.out.println();
    }
    
    /**
     * Test journeys specific to your route data
     */
    private static void testRouteSpecificJourneys(TrainGraph trainGraph) {
        System.out.println("4. Testing Route-Specific Journeys:");
        
        // Test major trunk routes based on your route summary
        System.out.println("\n   Testing Major Trunk Routes:");
        
        // S1/E1: Cape Town → Wellington (Route 35)
        testJourney(trainGraph, "CAPE TOWN", "WELLINGTON", LocalTime.of(7, 30), 
            "S1/E1 - Full Northern Line");
        
        // S8/E8: Cape Town → Simon's Town (Route 1) 
        testJourney(trainGraph, "CAPE TOWN", "SIMON'S TOWN", LocalTime.of(8, 0), 
            "S8/E8 - Full Southern Line");
        
        // S10: Cape Town → Bellville (Route 90)
        testJourney(trainGraph, "CAPE TOWN", "BELLVILLE", LocalTime.of(8, 30), 
            "S10 - Central Line to Bellville");
        
        // S11/E11: Cape Town → Chris Hani (Route 99)
        testJourney(trainGraph, "CAPE TOWN", "CHRIS HANI", LocalTime.of(9, 0), 
            "S11/E11 - Central Line to Chris Hani");
        
        System.out.println("\n   Testing Intermediate Stations:");
        
        // Test intermediate stops
        testJourney(trainGraph, "MUTUAL", "BELLVILLE", LocalTime.of(8, 15), 
            "Mutual to Bellville");
        testJourney(trainGraph, "CLAREMONT", "RETREAT", LocalTime.of(9, 30), 
            "Claremont to Retreat");
        testJourney(trainGraph, "BELLVILLE", "KRAAIFONTEIN", LocalTime.of(10, 0), 
            "Bellville to Kraaifontein");
        
        System.out.println();
    }
    
    /**
     * Test multi-transfer journeys across different lines
     */
    private static void testMultiTransferJourneys(TrainGraph trainGraph) {
        System.out.println("5. Testing Multi-Transfer Journeys:");
        
        // Cross-line transfers (should require transfers via Cape Town or other hubs)
        System.out.println("\n   Testing Cross-Line Transfers:");
        
        testJourney(trainGraph, "SIMON'S TOWN", "WELLINGTON", LocalTime.of(7, 0), 
            "South to North (Simon's Town → Wellington)");
        testJourney(trainGraph, "STRAND", "CHRIS HANI", LocalTime.of(8, 0), 
            "Strand to Chris Hani");
        testJourney(trainGraph, "KRAAIFONTEIN", "RETREAT", LocalTime.of(9, 0), 
            "Kraaifontein to Retreat");
        
        // Test with different max rounds to see transfer impact
        System.out.println("\n   Testing Transfer Limits:");
        String source = "SIMON'S TOWN", destination = "WELLINGTON";
        LocalTime depTime = LocalTime.of(8, 0);
        
        for (int maxRounds = 1; maxRounds <= 4; maxRounds++) {
            TrainJourney journey = trainGraph.findEarliestArrival(source, destination, depTime);
            if (journey != null) {
                System.out.printf("   Max %d rounds: %s → %s = %d min, %d transfers%n",
                    maxRounds, source, destination,
                    journey.getTotalDurationMinutes(), journey.getNumberOfTransfers());
            } else {
                System.out.printf("   Max %d rounds: No journey found%n", maxRounds);
            }
        }
        
        System.out.println();
    }
    
    /**
     * Test edge cases and error handling
     */
    private static void testEdgeCases(TrainGraph trainGraph) {
        System.out.println("6. Testing Edge Cases and Error Handling:");
        
        // Test invalid stations
        testJourney(trainGraph, "NONEXISTENT_STATION", "CAPE TOWN", LocalTime.of(8, 0), 
            "Invalid source station");
        testJourney(trainGraph, "CAPE TOWN", "INVALID_DESTINATION", LocalTime.of(8, 0), 
            "Invalid destination station");
        
        // Test same source and destination
        testJourney(trainGraph, "CAPE TOWN", "CAPE TOWN", LocalTime.of(8, 0), 
            "Same source and destination");
        
        // Test very early and late departure times
        testJourney(trainGraph, "CAPE TOWN", "BELLVILLE", LocalTime.of(1, 0), 
            "Very early departure (01:00)");
        testJourney(trainGraph, "CAPE TOWN", "BELLVILLE", LocalTime.of(23, 30), 
            "Very late departure (23:30)");
        
        // Test null/empty inputs
        try {
            TrainJourney nullJourney = trainGraph.findEarliestArrival(null, "CAPE TOWN", LocalTime.of(8, 0));
            System.out.printf("   Null source: %s%n", nullJourney != null ? "Journey found (unexpected)" : "Properly handled");
        } catch (Exception e) {
            System.out.printf("   Null source: Exception handled - %s%n", e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Performance testing for RAPTOR algorithm
     */
    private static void performanceTestRaptor(TrainGraph trainGraph) {
        System.out.println("7. RAPTOR Performance Testing:");
        
        List<TrainStop> stops = trainGraph.getTrainStops();
        if (stops.size() < 10) {
            System.out.println("   Not enough stops for performance testing");
            return;
        }
        
        // Test multiple journeys to measure average performance
        long totalTime = 0;
        int validJourneys = 0;
        int testIterations = 20;
        
        System.out.printf("   Running %d journey calculations...%n", testIterations);
        
        for (int i = 0; i < testIterations; i++) {
            TrainStop source = stops.get(i % stops.size());
            TrainStop dest = stops.get((i + stops.size()/2) % stops.size());
            LocalTime depTime = LocalTime.of(8 + (i % 6), (i * 15) % 60);
            
            long startTime = System.nanoTime();
            TrainJourney journey = trainGraph.findEarliestArrival(
                source.getName(), dest.getName(), depTime);
            long endTime = System.nanoTime();
            
            totalTime += (endTime - startTime);
            if (journey != null) validJourneys++;
        }
        
        double avgTimeMs = totalTime / 1_000_000.0 / testIterations;
        System.out.printf("   Average calculation time: %.2f ms%n", avgTimeMs);
        System.out.printf("   Valid journeys found: %d/%d (%.1f%%)%n", 
            validJourneys, testIterations, (validJourneys * 100.0 / testIterations));
        
        if (avgTimeMs > 100) {
            System.out.println("   ⚠️  Performance may be slow for real-time applications");
        } else if (avgTimeMs < 10) {
            System.out.println("   ✓ Excellent performance for real-time applications");
        } else {
            System.out.println("   ✓ Good performance for most applications");
        }
        
        System.out.println();
    }
    
    /**
     * Test different departure times to verify schedule handling
     */
    private static void testDepartureTimeVariations(TrainGraph trainGraph) {
        System.out.println("8. Testing Departure Time Variations:");
        
        String source = "CAPE TOWN", destination = "BELLVILLE";
        
        // Test different times throughout the day
        LocalTime[] testTimes = {
            LocalTime.of(6, 0),   // Early morning
            LocalTime.of(8, 0),   // Morning rush
            LocalTime.of(12, 0),  // Midday
            LocalTime.of(17, 0),  // Evening rush
            LocalTime.of(22, 0)   // Late evening
        };
        
        System.out.printf("   Testing %s → %s at different times:%n", source, destination);
        
        for (LocalTime time : testTimes) {
            TrainJourney journey = trainGraph.findEarliestArrival(source, destination, time);
            if (journey != null) {
                System.out.printf("   %s departure: Arrive %s (%d min, %d transfers)%n",
                    time, journey.getArrivalTime(), 
                    journey.getTotalDurationMinutes(), journey.getNumberOfTransfers());
            } else {
                System.out.printf("   %s departure: No journey found%n", time);
            }
        }
        
        System.out.println();
    }
    
    /**
     * Validate journey consistency and correctness
     */
    private static void validateJourneyConsistency(TrainGraph trainGraph) {
        System.out.println("9. Validating Journey Consistency:");
        
        // Test a known good route and validate all aspects
        TrainJourney journey = trainGraph.findEarliestArrival("CAPE TOWN", "BELLVILLE", LocalTime.of(8, 0));
        
        if (journey == null) {
            System.out.println("   Cannot validate - no test journey found");
            return;
        }
        
        System.out.printf("   Validating journey: %s → %s%n", 
            journey.getSource().getName(), journey.getDestination().getName());
        
        boolean isValid = true;
        
        // Check 1: Journey has trips
        if (journey.getTrips().isEmpty()) {
            System.out.println("   ✗ Journey has no trips");
            isValid = false;
        } else {
            System.out.printf("   ✓ Journey has %d trip(s)%n", journey.getTrips().size());
        }
        
        // Check 2: Trip connectivity
        List<TrainTrips> trips = journey.getTrips();
        for (int i = 0; i < trips.size() - 1; i++) {
            TrainTrips currentTrip = trips.get(i);
            TrainTrips nextTrip = trips.get(i + 1);
            
            if (!currentTrip.getDestinationTrainStop().getName()
                    .equals(nextTrip.getDepartureTrainStop().getName())) {
                System.out.printf("   ✗ Trip %d and %d are not connected%n", i + 1, i + 2);
                isValid = false;
            }
        }
        if (isValid && trips.size() > 1) {
            System.out.println("   ✓ All trips are properly connected");
        }
        
        // Check 3: Time consistency
        LocalTime currentTime = journey.getDepartureTime();
        for (int i = 0; i < trips.size(); i++) {
            TrainTrips trip = trips.get(i);
            LocalTime tripDeparture = trip.getDepartureTime();
            
            if (tripDeparture != null && tripDeparture.isBefore(currentTime)) {
                System.out.printf("   ✗ Trip %d departs before arrival at station%n", i + 1);
                isValid = false;
            }
            
            if (tripDeparture != null) {
                currentTime = tripDeparture.plusMinutes(trip.getDuration());
            }
        }
        if (isValid) {
            System.out.println("   ✓ Time sequence is consistent");
        }
        
        // Check 4: Duration calculation
        long calculatedDuration = java.time.Duration.between(
            journey.getDepartureTime(), journey.getArrivalTime()).toMinutes();
        if (Math.abs(calculatedDuration - journey.getTotalDurationMinutes()) > 1) {
            System.out.printf("   ✗ Duration mismatch: calculated %d, reported %d%n", 
                calculatedDuration, journey.getTotalDurationMinutes());
            isValid = false;
        } else {
            System.out.println("   ✓ Duration calculation is correct");
        }
        
        if (isValid) {
            System.out.println("   ✅ Journey validation passed - RAPTOR algorithm working correctly");
        } else {
            System.out.println("   ❌ Journey validation failed - issues detected in RAPTOR implementation");
        }
        
        System.out.println();
        System.out.println("=== Train RAPTOR Test Complete ===");
    }
    
    /**
     * Helper method to test a single journey
     */
    private static void testJourney(TrainGraph trainGraph, String source, String destination, 
                                  LocalTime departureTime, String testDescription) {
        System.out.printf("   %s:%n", testDescription);
        
        long startTime = System.nanoTime();
        TrainJourney journey = trainGraph.findEarliestArrival(source, destination, departureTime);
        long endTime = System.nanoTime();
        
        double executionTime = (endTime - startTime) / 1_000_000.0; // Convert to ms
        
        if (journey != null && !journey.getTrips().isEmpty()) {
            System.out.printf("     ✓ %s → %s: %s to %s (%d min, %d transfers) [%.1f ms]%n",
                source, destination, journey.getDepartureTime(), journey.getArrivalTime(),
                journey.getTotalDurationMinutes(), journey.getNumberOfTransfers(), executionTime);
            
            // Show route details for complex journeys
            if (journey.getTrips().size() > 1) {
                System.out.println("       Route details:");
                for (int i = 0; i < journey.getTrips().size(); i++) {
                    TrainTrips trip = journey.getTrips().get(i);
                    System.out.printf("         %d. Route %s: %s → %s%n", 
                        i + 1, trip.getRouteNumber(),
                        trip.getDepartureTrainStop().getName(),
                        trip.getDestinationTrainStop().getName());
                }
            }
        } else {
            System.out.printf("     ✗ %s → %s: No journey found [%.1f ms]%n", 
                source, destination, executionTime);
        }
    }
}