package com.boolean_brotherhood.public_transportation_journey_planner.MyCitiTests;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiStop;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;


public class MyCitiBusGraphTest {
    
    private MyCitiBusGraph myCitiBusGraph;
    
    @BeforeEach
    void setUp() throws Exception {
        myCitiBusGraph = new MyCitiBusGraph();
        // Assuming the graph loads data automatically in constructor
    }
    
    @Test
    void testRoute101StationsExist() {
        // Test outbound direction stations from your CSV
        String[] outboundStations = {
            "Civic Centre", "Adderley", "Lower Plein", "Lower Buitenkant",
            "Roeland", "Roodehek", "Gardens", "Upper Buitenkant",
            "Highlands", "Herzlia", "Exner", "Wexford"
        };
        
        for (String stationName : outboundStations) {
            MyCitiStop stop = myCitiBusGraph.getMyCitiStops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(stationName))
                .findFirst()
                .orElse(null);
            assertNotNull(stop, "Station " + stationName + " should exist in route 101");
        }
    }
    
    @Test
    void testRoute101InboundStationsExist() {
        // Test inbound direction stations (return journey)
        String[] inboundStations = {
            "Wexford", "St James", "Gardenia", "Nazareth", "Gardens",
            "Annandale", "Government Ave", "Michaelis", "Upper Loop",
            "Leeuwen", "Church", "Mid Loop", "Riebeeck", "Adderley", "Civic Centre"
        };
        
        for (String stationName : inboundStations) {
            MyCitiStop stop = myCitiBusGraph.getMyCitiStops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(stationName))
                .findFirst()
                .orElse(null);
            assertNotNull(stop, "Station " + stationName + " should exist in route 101 return");
        }
    }
    
    @Test
    void testRoute101FirstDeparture() {
        // Test first departure at 05:50 from Civic Centre
        MyCitiStop civicCentre = myCitiBusGraph.getMyCitiStops().stream()
            .filter(s -> s.getName().equalsIgnoreCase("Civic Centre"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(civicCentre, "Civic Centre stop should exist");
        
        List<MyCitiTrip> trips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDepartureMyCitiStop().equals(civicCentre))
            .filter(t -> t.getRouteName().equals("101"))
            .toList();
        
        assertFalse(trips.isEmpty(), "Should have trips from Civic Centre on route 101");
        
        LocalTime earliestTime = trips.stream()
            .map(MyCitiTrip::getDepartureTime)
            .filter(time -> time != null)
            .min(LocalTime::compareTo)
            .orElse(null);
            
        assertEquals(LocalTime.of(5, 50), earliestTime, 
            "First departure should be 05:50");
    }
    
    @Test
    void testRoute101LastDeparture() {
        // Test last departure at 18:50 from Civic Centre
        MyCitiStop civicCentre = myCitiBusGraph.getMyCitiStops().stream()
            .filter(s -> s.getName().equalsIgnoreCase("Civic Centre"))
            .findFirst()
            .orElse(null);
        
        List<MyCitiTrip> trips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDepartureMyCitiStop().equals(civicCentre))
            .filter(t -> t.getRouteName().equals("101"))
            .toList();
        
        LocalTime latestTime = trips.stream()
            .map(MyCitiTrip::getDepartureTime)
            .filter(time -> time != null)
            .max(LocalTime::compareTo)
            .orElse(null);
            
        assertEquals(LocalTime.of(18, 50), latestTime, 
            "Last departure should be 18:50");
    }
    
    @Test
    void testRoute101Frequency() {
        // Route 101 runs hourly from 05:50 to 18:50 = 14 trips per day type
        MyCitiStop civicCentre = myCitiBusGraph.getMyCitiStops().stream()
            .filter(s -> s.getName().equalsIgnoreCase("Civic Centre"))
            .findFirst()
            .orElse(null);
        
        // Count weekday trips
        long weekdayTrips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDepartureMyCitiStop().equals(civicCentre))
            .filter(t -> t.getRouteName().equals("101"))
            .filter(t -> t.getDayType() == Trip.DayType.WEEKDAY)
            .count();
        
        assertEquals(14, weekdayTrips, "Should have 14 weekday departures per direction");
        
        // Count Saturday trips
        long saturdayTrips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDepartureMyCitiStop().equals(civicCentre))
            .filter(t -> t.getRouteName().equals("101"))
            .filter(t -> t.getDayType() == Trip.DayType.SATURDAY)
            .count();
        
        assertEquals(14, saturdayTrips, "Should have 14 Saturday departures per direction");
        
        // Count Sunday trips
        long sundayTrips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDepartureMyCitiStop().equals(civicCentre))
            .filter(t -> t.getRouteName().equals("101"))
            .filter(t -> t.getDayType() == Trip.DayType.SUNDAY)
            .count();
        
        assertEquals(14, sundayTrips, "Should have 14 Sunday departures per direction");
    }
    
    @Test
    void testRoute101TripDuration() {
        // Test trip duration from Civic Centre to Wexford (22 minutes based on CSV)
        MyCitiStop civicCentre = myCitiBusGraph.getMyCitiStops().stream()
            .filter(s -> s.getName().equalsIgnoreCase("Civic Centre"))
            .findFirst().orElse(null);
        
        MyCitiStop wexford = myCitiBusGraph.getMyCitiStops().stream()
            .filter(s -> s.getName().equalsIgnoreCase("Wexford"))
            .findFirst().orElse(null);
        
        assertNotNull(civicCentre, "Civic Centre should exist");
        assertNotNull(wexford, "Wexford should exist");
        
        // Find a trip from Civic Centre to Wexford
        MyCitiTrip trip = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDepartureMyCitiStop().equals(civicCentre))
            .filter(t -> t.getDestinationMyCitiStop().equals(wexford))
            .filter(t -> t.getRouteName().equals("101"))
            .findFirst()
            .orElse(null);
        
        if (trip != null) {
            // From CSV: 05:50 to 06:12 = 22 minutes
            assertEquals(22, trip.getDuration(), 
                "Trip from Civic Centre to Wexford should take 22 minutes");
        }
    }
    
    @Test
    void testRoute101RoundTripTime() {
        // Test complete round trip duration
        // Outbound: 05:50 to 06:12 (22 min)
        // Inbound: 06:12 to 06:44 (32 min) = Total 54 minutes
        MyCitiStop civicCentre = myCitiBusGraph.getMyCitiStops().stream()
            .filter(s -> s.getName().equalsIgnoreCase("Civic Centre"))
            .findFirst().orElse(null);
        
        List<MyCitiTrip> roundTripCandidates = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDepartureMyCitiStop().equals(civicCentre))
            .filter(t -> t.getDestinationMyCitiStop().equals(civicCentre))
            .filter(t -> t.getRouteName().equals("101"))
            .toList();
        
        if (!roundTripCandidates.isEmpty()) {
            MyCitiTrip roundTrip = roundTripCandidates.get(0);
            assertEquals(54, roundTrip.getDuration(), 
                "Complete round trip should take 54 minutes");
        }
    }
    

    
    @Test
    void testSpecificTimes() {
        // Test specific departure times from CSV data
        MyCitiStop adderley = myCitiBusGraph.getMyCitiStops().stream()
            .filter(s -> s.getName().equalsIgnoreCase("Adderley"))
            .findFirst().orElse(null);
        
        assertNotNull(adderley, "Adderley stop should exist");
        
        // From CSV: bus should arrive at Adderley at 05:53, 06:53, 07:53, etc.
        List<MyCitiTrip> adderlayArrivals = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getDestinationMyCitiStop().equals(adderley))
            .filter(t -> t.getRouteName().equals("101"))
            .filter(t -> t.getDayType() == Trip.DayType.WEEKDAY)
            .toList();
        
        // Check if we have expected arrival times
        boolean hasExpectedTimes = adderlayArrivals.stream()
            .anyMatch(t -> {
                LocalTime arrivalTime = t.getDepartureTime().plusMinutes(t.getDuration());
                return arrivalTime.equals(LocalTime.of(5, 53)) ||
                       arrivalTime.equals(LocalTime.of(6, 53)) ||
                       arrivalTime.equals(LocalTime.of(7, 53));
            });
        
        assertTrue(hasExpectedTimes, "Should have expected arrival times at Adderley");
    }
    
    @Test
    void testRoute101IsCircular() {
        // Test that route 101 is circular (starts and ends at same place)
        List<MyCitiTrip> route101Trips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getRouteName().equals("101"))
            .toList();
        
        assertFalse(route101Trips.isEmpty(), "Should have route 101 trips");
        
        // Check if there are trips that start and end at Civic Centre
        boolean hasCircularTrips = route101Trips.stream()
            .anyMatch(t -> t.getDepartureMyCitiStop().getName().equalsIgnoreCase("Civic Centre") &&
                          t.getDestinationMyCitiStop().getName().equalsIgnoreCase("Civic Centre"));
        
        assertTrue(hasCircularTrips, "Route 101 should have circular trips");
    }
    
    @Test
    void testDayTypeVariations() {
        // Test that all day types have the same schedule for route 101
        List<MyCitiTrip> weekdayTrips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getRouteName().equals("101"))
            .filter(t -> t.getDayType() == Trip.DayType.WEEKDAY)
            .toList();
        
        List<MyCitiTrip> saturdayTrips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getRouteName().equals("101"))
            .filter(t -> t.getDayType() == Trip.DayType.SATURDAY)
            .toList();
        
        List<MyCitiTrip> sundayTrips = myCitiBusGraph.getMyCitiTrips().stream()
            .filter(t -> t.getRouteName().equals("101"))
            .filter(t -> t.getDayType() == Trip.DayType.SUNDAY)
            .toList();
        
        // All day types should have same number of trips for route 101
        assertEquals(weekdayTrips.size(), saturdayTrips.size(), 
            "Saturday should have same number of trips as weekday");
        assertEquals(weekdayTrips.size(), sundayTrips.size(), 
            "Sunday should have same number of trips as weekday");
    }
    
    @Test
    void testMetrics() {
        // Test MyCiTi graph metrics
        Map<String, Long> metrics = myCitiBusGraph.getMetrics();
        assertNotNull(metrics, "Metrics should not be null");
        
        long stopCount = myCitiBusGraph.getMyCitiStops().size();
        long tripCount = myCitiBusGraph.getMyCitiTrips().size();
        
        assertTrue(stopCount > 0, "Should have loaded MyCiTi stops");
        assertTrue(tripCount > 0, "Should have loaded MyCiTi trips");
        
        // Should have at least the route 101 stops (27 unique stops from both directions)
        assertTrue(stopCount >= 27, "Should have at least 27 stops for route 101");
    }
}