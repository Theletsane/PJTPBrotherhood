package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalDateTime;
import java.util.*;

/*
 * ____________NAMING CONVENTION ______________
 *  = Functions in all caps (UPPERCASE) are used by AdminController. They are designed to be easily called at an endpoint
 *  = Functions in lowercase are used by self to record, calculate and present data/
 */
public class SystemLog {

    private static final Map<String, LocalDateTime> endpoint_and_timeCalled = new HashMap<>();
    private static final Map<String, Integer> numberoftimescalled = new HashMap<>();
    private static final Map<Stop, Integer> allStops = new HashMap<>();
    private static final List<String> routeNames = new ArrayList<>();
    private static final Map<String,String> allStations = new HashMap<>();
    private static final List<Map<String, Object>> systemEvents = new ArrayList<>();
    private static final int MAX_EVENT_HISTORY = 2000;
    private static final int DEFAULT_EVENT_LIMIT = 100;

    /*
     * Returns count of all stops in the system
     */
    public static long GET_NUMBER_OF_STOPS(){
        return (long)allStops.size();
    }

    /*
     * Returns an ArrayList<Stop> of all stops loaded in the system
     */
    public static List<Stop> GET_ALL_STOPS(){
        return new ArrayList<>(allStops.keySet());
    }

    /*
     * Called in specific graph class to add stop
     * Uses hashmap so stops should be implicitly unique
     */
    public static void add_stop(Stop st){
        allStops.put(st,allStops.getOrDefault(st, 0)+1);
    }

    public static void add_active_route(String routeName){
        routeNames.add(routeName);
    }

    public static List<String> GET_ROUTES(){
        return routeNames;
    }

    public static Map<String,String> GET_STATIONS(){
        return allStations;
    }

    public static void add_stations(String StationName,String Type){
        allStations.put(StationName,Type);
    }

    public static void add_stations(String StationName){
        allStations.put(StationName,"UNKNOWN TYPE");
    }

    public static void log_endpoint(String endpoint_name){
        LocalDateTime timeNow = LocalDateTime.now();
        int invocationCount;
        synchronized (SystemLog.class) {
            endpoint_and_timeCalled.put(endpoint_name, timeNow);
            invocationCount = numberoftimescalled.getOrDefault(endpoint_name, 0) + 1;
            numberoftimescalled.put(endpoint_name, invocationCount);
        }
        log_event("ENDPOINT", "Endpoint invoked: " + endpoint_name, "INFO", Map.of(
                "endpoint", endpoint_name,
                "invocationCount", invocationCount
        ));
    }

    private static List<Map.Entry<String, LocalDateTime>> sortByTime() {
        List<Map.Entry<String, LocalDateTime>> sortedEntries = new ArrayList<>(endpoint_and_timeCalled.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue()); // sorts by LocalDateTime
        return sortedEntries;
    }

    public static List<Map<String, Object>> GET_ENDPOINT_DATA() {
        List<Map<String, Object>> response = new ArrayList<>();

        for (Map.Entry<String, LocalDateTime> entry : sortByTime()) {
            Map<String, Object> map = new HashMap<>();
            map.put("endpoint", entry.getKey());
            map.put("lastCalled", entry.getValue());
            map.put("timesCalled", numberoftimescalled.get(entry.getKey()));
            response.add(map);
        }

        return response;
    }

    public static List<Map<String, Object>> GET_SYSTEM_EVENTS(){
        return GET_SYSTEM_EVENTS(DEFAULT_EVENT_LIMIT);
    }

    public static List<Map<String, Object>> GET_SYSTEM_EVENTS(int limit){
        int safeLimit = Math.max(1, Math.min(limit, MAX_EVENT_HISTORY));
        List<Map<String, Object>> snapshot;
        synchronized (systemEvents) {
            int fromIndex = Math.max(0, systemEvents.size() - safeLimit);
            snapshot = new ArrayList<>(systemEvents.subList(fromIndex, systemEvents.size()));
        }
        List<Map<String, Object>> result = new ArrayList<>(snapshot.size());
        for (Map<String, Object> event : snapshot) {
            Map<String, Object> copy = new HashMap<>(event);
            Object details = event.get("details");
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailsMap = new HashMap<>((Map<String, Object>) details);
                copy.put("details", detailsMap);
            }
            result.add(copy);
        }
        return result;
    }

    public static void log_event(String source, String message){
        log_event(source, message, "INFO", Collections.emptyMap());
    }

    public static void log_event(String source, String message, String level, Map<String, ?> details){
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now());
        event.put("source", source);
        event.put("level", level);
        event.put("message", message);
        if (details != null && !details.isEmpty()) {
            event.put("details", new HashMap<>(details));
        }
        synchronized (systemEvents) {
            systemEvents.add(event);
            int overflow = systemEvents.size() - MAX_EVENT_HISTORY;
            if (overflow > 0) {
                systemEvents.subList(0, overflow).clear();
            }
        }
    }
}
