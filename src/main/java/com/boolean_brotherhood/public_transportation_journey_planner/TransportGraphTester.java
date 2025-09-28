package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boolean_brotherhood.public_transportation_journey_planner.Graph;
import com.boolean_brotherhood.public_transportation_journey_planner.BusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.Stop;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiStop;

@Component
public class TransportGraphTester {

    @Autowired
    private Graph multimodalGraph;
    
    @Autowired
    private TrainGraph trainGraph;
    
    @Autowired
    private MyCitiBusGraph myCitiGraph;
    
    @Autowired
    private GABusGraph gaGraph;
    
    @Autowired
    private TaxiGraph taxiGraph;
    
    @Autowired
    private BusGraph combinedBusGraph;
    
    private Scanner scanner = new Scanner(System.in);

    public void runInteractiveTest() {
        System.out.println("=".repeat(80));
        System.out.println("CAPE TOWN TRANSPORT GRAPH TESTER");
        System.out.println("=".repeat(80));
        
        while (true) {
            printMenu();
            int choice = getIntInput("Enter your choice: ");
            
            switch (choice) {
                case 1 -> testTrainGraph();
                case 2 -> testMyCitiGraph();
                case 3 -> testGoldenArrowGraph();
                case 4 -> testCombinedBusGraph();
                case 5 -> testTaxiGraph();
                case 6 -> testMultimodalGraph();
                case 7 -> testAllGraphs();
                case 8 -> printSystemStats();
                case 0 -> {
                    System.out.println("Exiting tester...");
                    return;
                }
                default -> System.out.println("Invalid choice! Please try again.");
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private void printMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SELECT TRANSPORT SYSTEM TO TEST:");
        System.out.println("=".repeat(50));
        System.out.println("1. Train System");
        System.out.println("2. MyCiTi Bus System");
        System.out.println("3. Golden Arrow Bus System");
        System.out.println("4. Combined Bus System (MyCiTi + GA)");
        System.out.println("5. Taxi System");
        System.out.println("6. Multimodal Graph (All Systems)");
        System.out.println("7. Test All Systems");
        System.out.println("8. Print System Statistics");
        System.out.println("0. Exit");
        System.out.println("=".repeat(50));
    }
    
    // TRAIN SYSTEM TESTING
    public void testTrainGraph() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TESTING TRAIN SYSTEM");
        System.out.println("=".repeat(60));
        
        try {
            // Print basic stats
            System.out.println("TRAIN SYSTEM STATISTICS:");
            System.out.println("- Total Stops: " + trainGraph.getTrainStops().size());
            System.out.println("- Total Trips: " + trainGraph.getTrainTrips().size());
            System.out.println("- Route Numbers: " + trainGraph.routeNumbers);
            
            // List some stops
            System.out.println("\nFIRST 50 TRAIN STOPS:");
            trainGraph.getTrainStops().stream()
                .limit(50)
                .forEach(stop -> System.out.println("  - " + stop.getName() + 
                    " (" + stop.getLatitude() + ", " + stop.getLongitude() + ")"));
            
            // Test journey
            System.out.println("\nTEST JOURNEY PLANNING:");
            String from = getStringInput("Enter departure station: ");
            String to = getStringInput("Enter destination station: ");
            String timeStr = getStringInput("Enter departure time (HH:MM): ");
            
            LocalTime departureTime = LocalTime.parse(timeStr);
            
            System.out.println("\nSEARCHING FOR JOURNEY...");
            TrainJourney journey = trainGraph.findEarliestArrival(from, to, departureTime);
            
            printTrainJourney(journey, from, to);
            
        } catch (Exception e) {
            System.out.println("ERROR TESTING TRAIN SYSTEM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void printTrainJourney(TrainJourney journey, String from, String to) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("TRAIN JOURNEY RESULTS");
        System.out.println("-".repeat(50));
        
        if (journey == null || journey.getTrips().isEmpty()) {
            System.out.println("❌ NO JOURNEY FOUND from " + from + " to " + to);
            return;
        }
        
        System.out.println("✅ JOURNEY FOUND!");
        System.out.println("From: " + journey.getSource().getName());
        System.out.println("To: " + journey.getDestination().getName());
        System.out.println("Departure: " + journey.getDepartureTime());
        System.out.println("Arrival: " + journey.getArrivalTime());
        System.out.println("Duration: " + journey.getTotalDurationMinutes() + " minutes");
        System.out.println("Transfers: " + journey.getNumberOfTransfers());
        
        System.out.println("\nDETAILED JOURNEY BREAKDOWN:");
        for (int i = 0; i < journey.getTrips().size(); i++) {
            TrainTrips trip = journey.getTrips().get(i);
            System.out.println("  TRIP " + (i + 1) + ":");
            System.out.println("    Route: " + trip.getRouteNumber());
            System.out.println("    From: " + trip.getDepartureTrainStop().getName());
            System.out.println("    To: " + trip.getDestinationTrainStop().getName());
            System.out.println("    Departure: " + trip.getDepartureTime());
            System.out.println("    Duration: " + trip.getDuration() + " minutes");
            System.out.println("    Trip ID: " + trip.getTripID());
        }
    }
    
    // MYCITI BUS TESTING
    public void testMyCitiGraph() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TESTING MYCITI BUS SYSTEM");
        System.out.println("=".repeat(60));
        
        try {
            // Print basic stats
            System.out.println("MYCITI SYSTEM STATISTICS:");
            System.out.println("- Total Stops: " + myCitiGraph.getMyCitiStops().size());
            System.out.println("- Total Trips: " + myCitiGraph.getMyCitiTrips().size());
            
            // List some stops
            System.out.println("\nFIRST 10 MYCITI STOPS:");
            myCitiGraph.getMyCitiStops().stream()
                .limit(10)
                .forEach(stop -> System.out.println("  - " + stop.getName() + 
                    " (" + stop.getLatitude() + ", " + stop.getLongitude() + ")"));
            
            // Test journey
            System.out.println("\nTEST JOURNEY PLANNING:");
            String from = getStringInput("Enter departure stop: ");
            String to = getStringInput("Enter destination stop: ");
            String timeStr = getStringInput("Enter departure time (HH:MM): ");
            String dayStr = getStringInput("Enter day type (WEEKDAY/SATURDAY/SUNDAY) [WEEKDAY]: ");
            
            LocalTime departureTime = LocalTime.parse(timeStr);
            Trip.DayType dayType = dayStr.isEmpty() ? Trip.DayType.WEEKDAY : Trip.DayType.valueOf(dayStr.toUpperCase());
            
            System.out.println("\nSEARCHING FOR JOURNEY...");
            MyCitiBusGraph.MyCitiRaptor raptor = new MyCitiBusGraph.MyCitiRaptor(myCitiGraph);
            MyCitiBusJourney journey = raptor.runRaptor(from, to, departureTime, 4, dayType);
            
            printMyCitiJourney(journey, from, to);
            
        } catch (Exception e) {
            System.out.println("ERROR TESTING MYCITI SYSTEM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void printMyCitiJourney(MyCitiBusJourney journey, String from, String to) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("MYCITI JOURNEY RESULTS");
        System.out.println("-".repeat(50));
        
        if (journey == null || journey.getTrips().isEmpty()) {
            System.out.println("❌ NO JOURNEY FOUND from " + from + " to " + to);
            return;
        }
        
        System.out.println("✅ JOURNEY FOUND!");
        System.out.println("From: " + journey.getSource().getName());
        System.out.println("To: " + journey.getDestination().getName());
        System.out.println("Departure: " + journey.getDepartureTime());
        System.out.println("Arrival: " + journey.getArrivalTime());
        System.out.println("Duration: " + journey.getTotalDurationMinutes() + " minutes");
        System.out.println("Transfers: " + journey.getNumberOfTransfers());
        
        System.out.println("\nDETAILED JOURNEY BREAKDOWN:");
        for (int i = 0; i < journey.getTrips().size(); i++) {
            MyCitiTrip trip = journey.getTrips().get(i);
            System.out.println("  TRIP " + (i + 1) + ":");
            System.out.println("    Route: " + trip.getRouteName());
            System.out.println("    From: " + trip.getDepartureMyCitiStop().getName());
            System.out.println("    To: " + trip.getDestinationMyCitiStop().getName());
            System.out.println("    Departure: " + trip.getDepartureTime());
            System.out.println("    Duration: " + trip.getDuration() + " minutes");
            System.out.println("    Trip ID: " + trip.getTripID());
            System.out.println("    Day Type: " + trip.getDayType());
        }
    }
    
    // GOLDEN ARROW TESTING
    public void testGoldenArrowGraph() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TESTING GOLDEN ARROW BUS SYSTEM");
        System.out.println("=".repeat(60));
        
        try {
            // Print basic stats
            System.out.println("GOLDEN ARROW SYSTEM STATISTICS:");
            System.out.println("- Total Stops: " + gaGraph.getGAStops().size());
            System.out.println("- Total Trips: " + gaGraph.getGATrips().size());
            
            // List some stops
            System.out.println("\nFIRST 10 GOLDEN ARROW STOPS:");
            gaGraph.getGAStops().stream()
                .limit(10)
                .forEach(stop -> System.out.println("  - " + stop.getName() + 
                    " (" + stop.getLatitude() + ", " + stop.getLongitude() + ")"));
            
            // Test journey
            System.out.println("\nTEST JOURNEY PLANNING:");
            String from = getStringInput("Enter departure stop: ");
            String to = getStringInput("Enter destination stop: ");
            String timeStr = getStringInput("Enter departure time (HH:MM): ");
            String dayStr = getStringInput("Enter day type (WEEKDAY/SATURDAY/SUNDAY) [WEEKDAY]: ");
            
            LocalTime departureTime = LocalTime.parse(timeStr);
            Trip.DayType dayType = dayStr.isEmpty() ? Trip.DayType.WEEKDAY : Trip.DayType.valueOf(dayStr.toUpperCase());
            
            System.out.println("\nSEARCHING FOR JOURNEY...");
            GABusGraph.GARaptor raptor = new GABusGraph.GARaptor(gaGraph);
            GABusJourney journey = raptor.runRaptor(from, to, departureTime, 4, dayType);
            
            printGAJourney(journey, from, to);
            
        } catch (Exception e) {
            System.out.println("ERROR TESTING GOLDEN ARROW SYSTEM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void printGAJourney(GABusJourney journey, String from, String to) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("GOLDEN ARROW JOURNEY RESULTS");
        System.out.println("-".repeat(50));
        
        if (journey == null || journey.getTrips().isEmpty()) {
            System.out.println("❌ NO JOURNEY FOUND from " + from + " to " + to);
            return;
        }
        
        System.out.println("✅ JOURNEY FOUND!");
        System.out.println("From: " + journey.getSource().getName());
        System.out.println("To: " + journey.getDestination().getName());
        System.out.println("Departure: " + journey.getDepartureTime());
        System.out.println("Arrival: " + journey.getArrivalTime());
        System.out.println("Duration: " + journey.getTotalDurationMinutes() + " minutes");
        System.out.println("Transfers: " + journey.getNumberOfTransfers());
        
        System.out.println("\nDETAILED JOURNEY BREAKDOWN:");
        for (int i = 0; i < journey.getTrips().size(); i++) {
            GATrip trip = journey.getTrips().get(i);
            System.out.println("  TRIP " + (i + 1) + ":");
            System.out.println("    Route: " + trip.getRouteName());
            System.out.println("    From: " + trip.getDepartureGAStop().getName());
            System.out.println("    To: " + trip.getDestinationGAStop().getName());
            System.out.println("    Departure: " + trip.getDepartureTime());
            System.out.println("    Duration: " + trip.getDuration() + " minutes");
            System.out.println("    Trip ID: " + trip.getTripID());
            System.out.println("    Day Type: " + trip.getDayType());
        }
    }
    
    // COMBINED BUS TESTING
    public void testCombinedBusGraph() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TESTING COMBINED BUS SYSTEM (MYCITI + GOLDEN ARROW)");
        System.out.println("=".repeat(60));
        
        try {
            // Print basic stats
            System.out.println("COMBINED BUS SYSTEM STATISTICS:");
            System.out.println("- Total Stops: " + combinedBusGraph.getAllBusStops().size());
            System.out.println("- Total Trips: " + combinedBusGraph.getAllBusTrips().size());
            
            // Test journey
            System.out.println("\nTEST JOURNEY PLANNING:");
            String from = getStringInput("Enter departure stop: ");
            String to = getStringInput("Enter destination stop: ");
            String timeStr = getStringInput("Enter departure time (HH:MM): ");
            String dayStr = getStringInput("Enter day type (WEEKDAY/SATURDAY/SUNDAY) [WEEKDAY]: ");
            String algorithm = getStringInput("Enter algorithm (RAPTOR/CSA) [RAPTOR]: ");
            
            LocalTime departureTime = LocalTime.parse(timeStr);
            Trip.DayType dayType = dayStr.isEmpty() ? Trip.DayType.WEEKDAY : Trip.DayType.valueOf(dayStr.toUpperCase());
            
            Stop source = combinedBusGraph.getAllBusStops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(from))
                .findFirst()
                .orElse(null);
            Stop target = combinedBusGraph.getAllBusStops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(to))
                .findFirst()
                .orElse(null);
            
            if (source == null || target == null) {
                System.out.println("❌ Could not find stops: " + from + " or " + to);
                return;
            }
            
            System.out.println("\nSEARCHING FOR JOURNEY...");
            BusGraph.RouterResult result;
            
            if (algorithm.isEmpty() || algorithm.equalsIgnoreCase("RAPTOR")) {
                BusGraph.RaptorRouter raptor = new BusGraph.RaptorRouter(
                    combinedBusGraph.getAllBusStops(), combinedBusGraph.getAllBusTrips());
                result = raptor.runRaptor(source, target, departureTime, 4, dayType);
            } else {
                BusGraph.ConnectionScanRouter csa = new BusGraph.ConnectionScanRouter(
                    combinedBusGraph.getAllBusStops(), combinedBusGraph.getAllBusTrips());
                result = csa.runConnectionScan(source, target, departureTime, dayType);
            }
            
            printBusGraphResult(result, from, to, algorithm);
            
        } catch (Exception e) {
            System.out.println("ERROR TESTING COMBINED BUS SYSTEM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void printBusGraphResult(BusGraph.RouterResult result, String from, String to, String algorithm) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("COMBINED BUS JOURNEY RESULTS (" + algorithm + ")");
        System.out.println("-".repeat(50));
        
        if (result == null || result.trips.isEmpty()) {
            System.out.println("❌ NO JOURNEY FOUND from " + from + " to " + to);
            return;
        }
        
        System.out.println("✅ JOURNEY FOUND!");
        System.out.println("Departure: " + result.departure);
        System.out.println("Arrival: " + result.arrival);
        System.out.println("Duration: " + result.totalMinutes + " minutes");
        System.out.println("Transfers: " + result.transfers);
        
        System.out.println("\nDETAILED JOURNEY BREAKDOWN:");
        for (int i = 0; i < result.trips.size(); i++) {
            Trip trip = result.trips.get(i);
            System.out.println("  TRIP " + (i + 1) + ":");
            System.out.println("    From: " + trip.getDepartureStop().getName());
            System.out.println("    To: " + trip.getDestinationStop().getName());
            System.out.println("    Departure: " + trip.getDepartureTime());
            System.out.println("    Duration: " + trip.getDuration() + " minutes");
            System.out.println("    Mode: " + trip.getMode());
            System.out.println("    Day Type: " + trip.getDayType());
            
            // Show route info if available
            if (trip instanceof MyCitiTrip) {
                MyCitiTrip myCitiTrip = (MyCitiTrip) trip;
                System.out.println("    Company: MyCiTi");
                System.out.println("    Route: " + myCitiTrip.getRouteName());
                System.out.println("    Trip ID: " + myCitiTrip.getTripID());
            } else if (trip instanceof GATrip) {
                GATrip gaTrip = (GATrip) trip;
                System.out.println("    Company: Golden Arrow");
                System.out.println("    Route: " + gaTrip.getRouteName());
                System.out.println("    Trip ID: " + gaTrip.getTripID());
            }
        }
    }
    
    // TAXI TESTING
    public void testTaxiGraph() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TESTING TAXI SYSTEM");
        System.out.println("=".repeat(60));
        
        try {
            // Print basic stats
            System.out.println("TAXI SYSTEM STATISTICS:");
            System.out.println("- Total Stops: " + taxiGraph.getTaxiStops().size());
            System.out.println("- Total Trips: " + taxiGraph.getTaxiTrips().size());
            
            // List some stops
            System.out.println("\nFIRST 10 TAXI STOPS:");
            taxiGraph.getTaxiStops().stream()
                .limit(10)
                .forEach(stop -> System.out.println("  - " + stop.getName() + 
                    " (" + stop.getLatitude() + ", " + stop.getLongitude() + ")"));
            
            // Test nearest stops
            double lat = getDoubleInput("Enter latitude: ");
            double lon = getDoubleInput("Enter longitude: ");
            int maxStops = getIntInput("Enter max number of stops to find: ");
            
            System.out.println("\nFINDING NEAREST TAXI STOPS...");
            List<TaxiStop> nearest = taxiGraph.getNearestTaxiStops(lat, lon, maxStops);
            
            System.out.println("\nNEAREST TAXI STOPS:");
            for (int i = 0; i < nearest.size(); i++) {
                TaxiStop stop = nearest.get(i);
                double distance = stop.distanceTo(lat, lon);
                System.out.println("  " + (i + 1) + ". " + stop.getName() + 
                    " (Distance: " + String.format("%.2f", distance) + "km)");
                System.out.println("     Location: (" + stop.getLatitude() + ", " + stop.getLongitude() + ")");
                System.out.println("     Address: " + stop.getAddress());
            }
            
        } catch (Exception e) {
            System.out.println("ERROR TESTING TAXI SYSTEM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // MULTIMODAL TESTING
    public void testMultimodalGraph() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TESTING MULTIMODAL GRAPH (ALL SYSTEMS)");
        System.out.println("=".repeat(60));
        
        try {
            // Print basic stats
            System.out.println("MULTIMODAL SYSTEM STATISTICS:");
            System.out.println("- Total Stops: " + multimodalGraph.getStops().size());
            System.out.println("- Total Trips: " + multimodalGraph.getTrips().size());
            
            // Test journey
            String from = getStringInput("Enter departure location: ");
            String to = getStringInput("Enter destination location: ");
            String timeStr = getStringInput("Enter departure time (HH:MM): ");
            
            LocalTime departureTime = LocalTime.parse(timeStr);
            
            System.out.println("\nSEARCHING FOR MULTIMODAL JOURNEY...");
            // Using default modes (all available)
            List<Trip> journey = multimodalGraph.runRaptor(
                java.util.EnumSet.allOf(Graph.Mode.class), 
                from, to, departureTime, 5, Trip.DayType.WEEKDAY);
            
            printMultimodalJourney(journey, from, to);
            
        } catch (Exception e) {
            System.out.println("ERROR TESTING MULTIMODAL SYSTEM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void printMultimodalJourney(List<Trip> journey, String from, String to) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("MULTIMODAL JOURNEY RESULTS");
        System.out.println("-".repeat(50));
        
        if (journey == null || journey.isEmpty()) {
            System.out.println("❌ NO JOURNEY FOUND from " + from + " to " + to);
            return;
        }
        
        System.out.println("✅ MULTIMODAL JOURNEY FOUND!");
        System.out.println("Total Duration: " + journey.stream().mapToInt(Trip::getDuration).sum() + " minutes");
        System.out.println("Number of Segments: " + journey.size());
        
        System.out.println("\nDETAILED JOURNEY BREAKDOWN:");
        for (int i = 0; i < journey.size(); i++) {
            Trip trip = journey.get(i);
            System.out.println("  SEGMENT " + (i + 1) + ":");
            System.out.println("    From: " + trip.getDepartureStop().getName());
            System.out.println("    To: " + trip.getDestinationStop().getName());
            System.out.println("    Mode: " + trip.getMode());
            System.out.println("    Duration: " + trip.getDuration() + " minutes");
            if (trip.getDepartureTime() != null) {
                System.out.println("    Departure: " + trip.getDepartureTime());
            }
        }
    }
    
    // TEST ALL SYSTEMS
    public void testAllGraphs() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TESTING ALL TRANSPORTATION SYSTEMS");
        System.out.println("=".repeat(80));
        
        System.out.println("This will run basic tests on all systems...\n");
        
        try {
            // Train
            System.out.println("1. TRAIN SYSTEM:");
            System.out.println("   Stops: " + trainGraph.getTrainStops().size());
            System.out.println("   Trips: " + trainGraph.getTrainTrips().size());
            System.out.println("   Status: " + (trainGraph.getTrainStops().size() > 0 ? "✅ LOADED" : "❌ EMPTY"));
            
            // MyCiTi
            System.out.println("\n2. MYCITI BUS SYSTEM:");
            System.out.println("   Stops: " + myCitiGraph.getMyCitiStops().size());
            System.out.println("   Trips: " + myCitiGraph.getMyCitiTrips().size());
            System.out.println("   Status: " + (myCitiGraph.getMyCitiStops().size() > 0 ? "✅ LOADED" : "❌ EMPTY"));
            
            // Golden Arrow
            System.out.println("\n3. GOLDEN ARROW BUS SYSTEM:");
            System.out.println("   Stops: " + gaGraph.getGAStops().size());
            System.out.println("   Trips: " + gaGraph.getGATrips().size());
            System.out.println("   Status: " + (gaGraph.getGAStops().size() > 0 ? "✅ LOADED" : "❌ EMPTY"));
            
            // Combined Bus
            System.out.println("\n4. COMBINED BUS SYSTEM:");
            System.out.println("   Stops: " + combinedBusGraph.getAllBusStops().size());
            System.out.println("   Trips: " + combinedBusGraph.getAllBusTrips().size());
            System.out.println("   Status: " + (combinedBusGraph.getAllBusStops().size() > 0 ? "✅ LOADED" : "❌ EMPTY"));
            
            // Taxi
            System.out.println("\n5. TAXI SYSTEM:");
            System.out.println("   Stops: " + taxiGraph.getTaxiStops().size());
            System.out.println("   Trips: " + taxiGraph.getTaxiTrips().size());
            System.out.println("   Status: " + (taxiGraph.getTaxiStops().size() > 0 ? "✅ LOADED" : "❌ EMPTY"));
            
            // Multimodal
            System.out.println("\n6. MULTIMODAL GRAPH:");
            System.out.println("   Stops: " + multimodalGraph.getStops().size());
            System.out.println("   Trips: " + multimodalGraph.getTrips().size());
            System.out.println("   Status: " + (multimodalGraph.getStops().size() > 0 ? "✅ LOADED" : "❌ EMPTY"));
            
        } catch (Exception e) {
            System.out.println("ERROR TESTING ALL SYSTEMS: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void printSystemStats() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("DETAILED SYSTEM STATISTICS");
        System.out.println("=".repeat(70));
        
        try {
            
            // MyCiTi metrics
            Map<String, Long> myCitiMetrics = myCitiGraph.getMetrics();
            System.out.println("\nMYCITI BUS SYSTEM:");
            myCitiMetrics.forEach((key, value) -> System.out.println("  " + key + ": " + value));
            
            // Golden Arrow metrics
            Map<String, Long> gaMetrics = gaGraph.getMetrics();
            System.out.println("\nGOLDEN ARROW BUS SYSTEM:");
            gaMetrics.forEach((key, value) -> System.out.println("  " + key + ": " + value));
            
            // Taxi metrics
            Map<String, Long> taxiMetrics = taxiGraph.getMetrics();
            System.out.println("\nTAXI SYSTEM:");
            taxiMetrics.forEach((key, value) -> System.out.println("  " + key + ": " + value));
            
        } catch (Exception e) {
            System.out.println("ERROR GETTING SYSTEM STATISTICS: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // UTILITY METHODS
    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number! Please try again.");
            }
        }
    }
    
    private double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number! Please try again.");
            }
        }
    }
    
    // MAIN METHOD FOR STANDALONE TESTING
    public static void main(String[] args) {
        System.out.println("TransportGraphTester is designed to be run within Spring Boot context.");
        System.out.println("Create a test endpoint or service to run this interactively.");
        System.out.println("\nExample usage in a controller:");
        System.out.println("@Autowired TransportGraphTester tester;");
        System.out.println("tester.runInteractiveTest();");
    }
    
    // QUICK TEST METHODS (without user input)
    public void quickTestTrain() {
        System.out.println("=== QUICK TRAIN TEST ===");
        try {
            TrainJourney journey = trainGraph.findEarliestArrival("Cape Town Station", "Bellville Station", LocalTime.of(8, 0));
            printTrainJourney(journey, "Cape Town Station", "Bellville Station");
        } catch (Exception e) {
            System.out.println("Quick train test failed: " + e.getMessage());
        }
    }
    
    public void quickTestMyCiti() {
        System.out.println("=== QUICK MYCITI TEST ===");
        try {
            MyCitiBusGraph.MyCitiRaptor raptor = new MyCitiBusGraph.MyCitiRaptor(myCitiGraph);
            MyCitiBusJourney journey = raptor.runRaptor("Civic Centre", "Wexford", LocalTime.of(9, 0), 4, Trip.DayType.WEEKDAY);
            printMyCitiJourney(journey, "Civic Centre", "Wexford");
        } catch (Exception e) {
            System.out.println("Quick MyCiti test failed: " + e.getMessage());
        }
    }
    
    public void quickTestGA() {
        System.out.println("=== QUICK GOLDEN ARROW TEST ===");
        try {
            GABusGraph.GARaptor raptor = new GABusGraph.GARaptor(gaGraph);
            GABusJourney journey = raptor.runRaptor("Bellville", "Cape Town", LocalTime.of(10, 0), 4, Trip.DayType.WEEKDAY);
            printGAJourney(journey, "Bellville", "Cape Town");
        } catch (Exception e) {
            System.out.println("Quick GA test failed: " + e.getMessage());
        }
    }
    
    public void quickTestTaxi() {
        System.out.println("=== QUICK TAXI TEST ===");
        try {
            // Cape Town city center coordinates
            List<TaxiStop> nearest = taxiGraph.getNearestTaxiStops(-33.9249, 18.4241, 5);
            System.out.println("Found " + nearest.size() + " nearest taxi stops:");
            for (int i = 0; i < nearest.size(); i++) {
                TaxiStop stop = nearest.get(i);
                System.out.println("  " + (i + 1) + ". " + stop.getName() + " (" + 
                    String.format("%.2f", stop.distanceTo(-33.9249, 18.4241)) + "km)");
            }
        } catch (Exception e) {
            System.out.println("Quick taxi test failed: " + e.getMessage());
        }
    }
    
    public void runAllQuickTests() {
        System.out.println("RUNNING ALL QUICK TESTS...\n");
        quickTestTrain();
        System.out.println();
        quickTestMyCiti();
        System.out.println();
        quickTestGA();
        System.out.println();
        quickTestTaxi();
        System.out.println("\nQUICK TESTS COMPLETED!");
    }
    
    // DEBUG METHODS
    public void debugPrintStops(String graphType) {
        System.out.println("=== DEBUG: " + graphType.toUpperCase() + " STOPS ===");
        
        switch (graphType.toLowerCase()) {
            case "train":
                trainGraph.getTrainStops().forEach(stop -> 
                    System.out.println(stop.getName() + " [" + stop.getStopCode() + "] (" + 
                        stop.getLatitude() + ", " + stop.getLongitude() + ")"));
                break;
            case "myciti":
                myCitiGraph.getMyCitiStops().forEach(stop -> 
                    System.out.println(stop.getName() + " [" + stop.getStopCode() + "] (" + 
                        stop.getLatitude() + ", " + stop.getLongitude() + ")"));
                break;
            case "ga":
                gaGraph.getGAStops().forEach(stop -> 
                    System.out.println(stop.getName() + " [" + stop.getStopCode() + "] (" + 
                        stop.getLatitude() + ", " + stop.getLongitude() + ")"));
                break;
            case "taxi":
                taxiGraph.getTaxiStops().forEach(stop -> 
                    System.out.println(stop.getName() + " [" + stop.getStopCode() + "] (" + 
                        stop.getLatitude() + ", " + stop.getLongitude() + ")"));
                break;
            case "bus":
                combinedBusGraph.getAllBusStops().forEach(stop -> 
                    System.out.println(stop.getName() + " [" + stop.getStopCode() + "] (" + 
                        stop.getLatitude() + ", " + stop.getLongitude() + ")"));
                break;
            default:
                System.out.println("Unknown graph type: " + graphType);
        }
    }
    
    public void debugPrintTrips(String graphType, int limit) {
        System.out.println("=== DEBUG: " + graphType.toUpperCase() + " TRIPS (FIRST " + limit + ") ===");
        
        switch (graphType.toLowerCase()) {
            case "train":
                trainGraph.getTrainTrips().stream().limit(limit).forEach(trip -> 
                    System.out.println(trip.getTripID() + ": " + trip.getDepartureTrainStop().getName() + 
                        " -> " + trip.getDestinationTrainStop().getName() + " (" + trip.getDuration() + "min)"));
                break;
            case "myciti":
                myCitiGraph.getMyCitiTrips().stream().limit(limit).forEach(trip -> 
                    System.out.println(trip.getTripID() + ": " + trip.getDepartureMyCitiStop().getName() + 
                        " -> " + trip.getDestinationMyCitiStop().getName() + " (" + trip.getDuration() + "min, " + 
                        trip.getRouteName() + ")"));
                break;
            case "ga":
                gaGraph.getGATrips().stream().limit(limit).forEach(trip -> 
                    System.out.println(trip.getTripID() + ": " + trip.getDepartureGAStop().getName() + 
                        " -> " + trip.getDestinationGAStop().getName() + " (" + trip.getDuration() + "min, " + 
                        trip.getRouteName() + ")"));
                break;
            case "taxi":
                taxiGraph.getTaxiTrips().stream().limit(limit).forEach(trip -> 
                    System.out.println("Taxi: " + trip.getDepartureStop().getName() + 
                        " -> " + trip.getDestinationStop().getName() + " (" + trip.getDuration() + "min)"));
                break;
            default:
                System.out.println("Unknown graph type: " + graphType);
        }
    }
}