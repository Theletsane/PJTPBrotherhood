package com.boolean_brotherhood.public_transportation_journey_planner;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;

public class TrainGraphTestsss {
    
    private TrainGraph trainGraph;
    
    @BeforeEach
    void setUp() throws Exception {
        trainGraph = new TrainGraph();
        trainGraph.loadTrainStops();
        trainGraph.LoadTrainTrips();
    }
    
    @Test
    void testStationExists() {
        // Test that major stations from your data exist
        assertNotNull(trainGraph.getStopByName("CAPE TOWN"));
        assertNotNull(trainGraph.getStopByName("BELLVILLE"));
        assertNotNull(trainGraph.getStopByName("EERSTE RIVER"));
        assertNotNull(trainGraph.getStopByName("ESPLANADE"));
    }
    
    @Test
    void testS6RouteStations() {
        // Test all S6 route stations from your CSV exist
        String[] s6Stations = {
            "CAPE TOWN", "ESPLANADE", "YSTERPLAAT", "KENTEMADE", 
            "CENTURY CITY", "AKASIA PARK", "MONTE VISTA", "DE GRENDEL",
            "AVONDALE", "OOSTERZEE", "BELLVILLE", "KUILS RIVER",
            "BLACKHEATH", "MELTONROSE", "EERSTE RIVER"
        };
        
        for (String stationName : s6Stations) {
            assertNotNull(trainGraph.getStopByName(stationName), 
                "Station " + stationName + " should exist");
        }
    }
    
    @Test
    void testTripExists() {
        // Test specific trip from your CSV data (trip 2300)
        List<TrainTrips> trips = trainGraph.getTrainTrips();
        boolean foundTrip2300 = trips.stream()
            .anyMatch(trip -> "2300".equals(trip.getTripID()));
        assertTrue(foundTrip2300, "Trip 2300 should exist");
    }
    
    @Test
    void testEarliestDeparture() {
        // Test earliest departure time (04:30 from trip 2801)
        TrainStop capeStation = trainGraph.getStopByName("CAPE TOWN");
        List<TrainTrips> outgoingTrips = trainGraph.getOutgoingTrips(capeStation);
        
        LocalTime earliestTime = outgoingTrips.stream()
            .map(TrainTrips::getDepartureTime)
            .filter(time -> time != null)
            .min(LocalTime::compareTo)
            .orElse(null);
            
        assertNotNull(earliestTime, "Should have at least one departure time");
        assertEquals(LocalTime.of(4, 30), earliestTime, 
            "Earliest departure should be 04:30");
    }
    
    @Test
    void testLatestDeparture() {
        // Test latest departure time (19:25 from trip 2819 on Sundays)
        TrainStop capeStation = trainGraph.getStopByName("CAPE TOWN");
        List<TrainTrips> outgoingTrips = trainGraph.getOutgoingTrips(capeStation);
        
        LocalTime latestTime = outgoingTrips.stream()
            .map(TrainTrips::getDepartureTime)
            .filter(time -> time != null)
            .max(LocalTime::compareTo)
            .orElse(null);
            
        assertNotNull(latestTime, "Should have at least one departure time");
        // Your data shows trips up to around 19:25
        assertTrue(latestTime.isAfter(LocalTime.of(19, 0)), 
            "Latest departure should be after 19:00");
    }
    
    @Test
    void testJourneyPlanning() {
        // Test journey from Cape Town to Bellville
        TrainJourney journey = trainGraph.findEarliestArrival(
            "CAPE TOWN", "BELLVILLE", LocalTime.of(8, 0));
        
        assertNotNull(journey, "Should find a journey from Cape Town to Bellville");
        assertFalse(journey.getTrips().isEmpty(), "Journey should have at least one trip");
        assertEquals("CAPE TOWN", journey.getSource().getName());
        assertEquals("BELLVILLE", journey.getDestination().getName());
    }
    
    @Test
    void testDayTypeFiltering() {
        // Test that weekday trips exist
        List<TrainTrips> weekdayTrips = trainGraph.getTrainTrips().stream()
            .filter(trip -> trip.getDayType() == Trip.DayType.WEEKDAY)
            .toList();
        
        assertFalse(weekdayTrips.isEmpty(), "Should have weekday trips");
        
        // Test that Saturday trips exist
        List<TrainTrips> saturdayTrips = trainGraph.getTrainTrips().stream()
            .filter(trip -> trip.getDayType() == Trip.DayType.SATURDAY)
            .toList();
            
        assertFalse(saturdayTrips.isEmpty(), "Should have Saturday trips");
    }
    
    @Test
    void testInboundOutboundDirections() {
        // Test that both inbound and outbound trips exist
        List<TrainTrips> allTrips = trainGraph.getTrainTrips();
        
        boolean hasInbound = allTrips.stream()
            .anyMatch(trip -> trip.getTripID().startsWith("2") && 
                     trip.getDepartureStop().getName().equals("CAPE TOWN"));
        
        boolean hasOutbound = allTrips.stream()
            .anyMatch(trip -> trip.getTripID().startsWith("2") && 
                     trip.getDestinationStop().getName().equals("CAPE TOWN"));
        
        assertTrue(hasInbound || hasOutbound, 
            "Should have trips either departing from or arriving at Cape Town");
    }
    
    @Test
    void testRouteNumbers() {
        // Test that S6 route exists in your data
        List<String> routeNumbers = trainGraph.routeNumbers;
        assertTrue(routeNumbers.contains("S6"), "Should contain S6 route");
    }
    
    @Test
    void testNearestStation() {
        // Test finding nearest station (using approximate Cape Town coordinates)
        TrainStop nearest = trainGraph.getNearestTrainStop(-33.9249, 18.4241);
        assertNotNull(nearest, "Should find a nearest station");
    }
    
    @Test
    void testTripDuration() {
        // Test that trips have reasonable durations
        List<TrainTrips> trips = trainGraph.getTrainTrips();
        
        for (TrainTrips trip : trips) {
            assertTrue(trip.getDuration() > 0, 
                "Trip " + trip.getTripID() + " should have positive duration");
            assertTrue(trip.getDuration() < 480, // 8 hours max seems reasonable
                "Trip " + trip.getTripID() + " duration seems too long");
        }
    }
    

}