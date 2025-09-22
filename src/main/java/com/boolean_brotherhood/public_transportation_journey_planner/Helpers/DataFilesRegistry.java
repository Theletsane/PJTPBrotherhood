package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;



import java.util.HashMap;
import java.util.Map;

public class DataFilesRegistry {

    // Store all your files in constants
    public static final String TAXI_STOPS = "CapeTownTransitData/TaxiStops(09_15_2025).csv";
    public static final String TAXI_TRIPS = "CapeTownTransitData/taxi-routes.csv" ;
    public static final String MYCITI_STOPS = "CapeTownTransitData/MyCiti_Data/stops_cleaned.csv";
    public static final String MYCITI_TRIPS = "CapeTownTransitData/MyCiti_Data/myciti-route_summary.csv";
    public static final String TRAIN_STOPS = "CapeTownTransitData/Train-Data/TrainStations_Corrected(09_22_2025).csv";
    public static final String TRAIN_TRIPS = "CapeTownTransitData/train-routes-summary.csv";

    // Map to record usage dynamically
    private static final Map<String, String> usageMap = new HashMap<>();

    /** 
     * Get a file path by name
     */
    public static String getFile(String key, String usedByClass) {
        String path = switch (key) {
            case "TAXI_STOPS" -> TAXI_STOPS;
            case "TAXI_TRIPS" -> TAXI_TRIPS;
            case "MYCITI_STOPS" -> MYCITI_STOPS;
            case "MYCITI_TRIPS" -> MYCITI_TRIPS;
            case "TRAIN_STOPS" -> TRAIN_STOPS;
            case "TRAIN_TRIPS" -> TRAIN_TRIPS;
            default -> throw new IllegalArgumentException("Unknown file key: " + key);
        };

        // Record who used it
        usageMap.put(path, usedByClass);
        return path;
    }

    /**
     * Return a copy of usage logs
     */
    public static Map<String, String> getUsageLogs() {
        return new HashMap<>(usageMap);
    }
}
