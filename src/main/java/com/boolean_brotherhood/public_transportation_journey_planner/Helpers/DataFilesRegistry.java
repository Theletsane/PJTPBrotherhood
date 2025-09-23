package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DataFilesRegistry {

    // Store all your files in constants
    public static final String TAXI_STOPS = "CapeTownTransitData/Taxi_Data/TaxiStops(09_15_2025).csv";
    public static final String TAXI_TRIPS = "CapeTownTransitData/Taxi_Data/taxi-routes.csv" ;
    public static final String MYCITI_STOPS = "CapeTownTransitData/MyCiti_Data/stops_cleaned.csv";
    public static final String MYCITI_TRIPS = "CapeTownTransitData/MyCiti_Data/myciti-route_summary.csv";
    public static final String TRAIN_STOPS = "CapeTownTransitData/Train_Data/TrainStations_Corrected(09_22_2025).csv";
    public static final String TRAIN_TRIPS = "CapeTownTransitData/Train_Data/train-routes-summary.csv";

    // Map to record usage dynamically
    private static final Map<String, String> usageMap = new HashMap<>();
            // Maps a file key to its path
    private static final Map<String, String> filePaths = new HashMap<>();

    // Maps a file key to its current usage (e.g., controller or process using it)
    private static final Map<String, String> usageLogs = new HashMap<>();


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




    /**
     * Register a file usage (e.g., which controller or process is using it)
     */
    public static void registerUsage(String fileKey, String usedBy) {
        usageLogs.put(fileKey, usedBy);
    }


    /**
     * Get full path for a file
     */
    public static String getFilePath(String fileKey) {
        return filePaths.get(fileKey);
    }

    /**
     * Get filename only
     */
    public static String getFileName(String fileKey) {
        String path = filePaths.get(fileKey);
        if (path == null) return null;
        return path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * Return a detailed report of all files with path, name, and usage
     */
    public static Map<String, Map<String, String>> getFileDetails() {
        Map<String, Map<String, String>> details = new HashMap<>();
        for (String key : filePaths.keySet()) {
            Map<String, String> info = new HashMap<>();
            info.put("path", filePaths.get(key));
            info.put("filename", getFileName(key));
            info.put("usage", usageLogs.getOrDefault(key, "Not in use"));
            details.put(key, info);
        }
        return details;
    }
}
