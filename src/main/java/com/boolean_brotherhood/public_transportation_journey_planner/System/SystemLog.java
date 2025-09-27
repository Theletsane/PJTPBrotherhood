package com.boolean_brotherhood.public_transportation_journey_planner.System;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Enhanced system logging with performance tracking and filtered admin logs
 */
public final class SystemLog {

    private static final int MAX_ENDPOINT_RECORDS = 100;
    private static final int MAX_SYSTEM_EVENTS = 500;
    private static final int MAX_ADMIN_EVENTS = 200;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Performance tracking for important operations
    private static final Map<String, Long> operationStartTimes = new ConcurrentHashMap<>();
    private static final Map<String, List<Long>> operationDurations = new ConcurrentHashMap<>();
    
    // Event storage
    private static final ConcurrentLinkedQueue<Map<String, Object>> endpointCalls = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Map<String, Object>> systemEvents = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Map<String, Object>> adminEvents = new ConcurrentLinkedQueue<>();
    
    // Station and route tracking (for compatibility)
    private static final Map<String, String> stationRegistry = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> activeRoutes = new ConcurrentHashMap<>();
    
    private SystemLog() {}

    /**
     * Start timing an important operation (data loading, graph building, etc.)
     */
    public static void startOperation(String operationName) {
        operationStartTimes.put(operationName, System.currentTimeMillis());
        log_event("PERFORMANCE", "Started operation: " + operationName, "INFO", Map.of(
            "operation", operationName,
            "startTime", System.currentTimeMillis()
        ));
    }

    /**
     * End timing an operation and record the duration
     */
    public static long endOperation(String operationName) {
        Long startTime = operationStartTimes.remove(operationName);
        if (startTime == null) {
            log_event("PERFORMANCE", "No start time found for operation: " + operationName, "WARN", Map.of(
                "operation", operationName
            ));
            return -1;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        operationDurations.computeIfAbsent(operationName, k -> new ArrayList<>()).add(duration);
        
        String level = duration > 10000 ? "WARN" : "INFO"; // Warn if operation takes > 10 seconds
        log_event("PERFORMANCE", "Completed operation: " + operationName, level, Map.of(
            "operation", operationName,
            "durationMs", duration,
            "totalOperations", operationDurations.get(operationName).size()
        ));
        
        return duration;
    }

    /**
     * Get performance statistics for operations
     */
    public static Map<String, Object> getOperationPerformance() {
        Map<String, Object> performance = new HashMap<>();
        
        for (Map.Entry<String, List<Long>> entry : operationDurations.entrySet()) {
            String operation = entry.getKey();
            List<Long> durations = entry.getValue();
            
            if (!durations.isEmpty()) {
                long total = durations.stream().mapToLong(Long::longValue).sum();
                long max = durations.stream().mapToLong(Long::longValue).max().orElse(0);
                long min = durations.stream().mapToLong(Long::longValue).min().orElse(0);
                double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
                
                performance.put(operation, Map.of(
                    "count", durations.size(),
                    "totalMs", total,
                    "averageMs", Math.round(avg * 100.0) / 100.0,
                    "maxMs", max,
                    "minMs", min,
                    "lastMs", durations.get(durations.size() - 1)
                ));
            }
        }
        
        return performance;
    }

    public static void log_endpoint(String endpoint) {
        Map<String, Object> record = new HashMap<>();
        record.put("endpoint", endpoint);
        record.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        record.put("thread", Thread.currentThread().getName());
        
        endpointCalls.offer(record);
        
        // Keep only the most recent records
        while (endpointCalls.size() > MAX_ENDPOINT_RECORDS) {
            endpointCalls.poll();
        }
    }

    public static void log_event(String component, String message, String level, Map<String, Object> details) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        event.put("component", component);
        event.put("level", level);
        event.put("message", message);
        event.put("details", new HashMap<>(details));
        event.put("thread", Thread.currentThread().getName());
        
        // Add to system events
        systemEvents.offer(event);
        while (systemEvents.size() > MAX_SYSTEM_EVENTS) {
            systemEvents.poll();
        }
        
        // Add to admin events only if WARNING or SEVERE/ERROR
        if ("WARN".equalsIgnoreCase(level) || 
            "WARNING".equalsIgnoreCase(level) || 
            "ERROR".equalsIgnoreCase(level) || 
            "SEVERE".equalsIgnoreCase(level) ||
            "CRITICAL".equalsIgnoreCase(level)) {
            
            adminEvents.offer(event);
            while (adminEvents.size() > MAX_ADMIN_EVENTS) {
                adminEvents.poll();
            }
            
            // Print critical events to console immediately
            if ("ERROR".equalsIgnoreCase(level) || 
                "SEVERE".equalsIgnoreCase(level) ||
                "CRITICAL".equalsIgnoreCase(level)) {
                System.err.println("[" + level + "] " + component + ": " + message);
                if (!details.isEmpty()) {
                    System.err.println("  Details: " + details);
                }
            }
        }
    }

    public static List<Map<String, Object>> GET_ENDPOINT_DATA() {
        return new ArrayList<>(endpointCalls);
    }

    public static List<Map<String, Object>> GET_SYSTEM_EVENTS(int limit) {
        List<Map<String, Object>> events = new ArrayList<>(systemEvents);
        Collections.reverse(events); // Most recent first
        return events.stream().limit(Math.max(1, limit)).toList();
    }

    /**
     * Get only WARNING and SEVERE events for admin dashboard
     */
    public static List<Map<String, Object>> GET_ADMIN_EVENTS(int limit) {
        List<Map<String, Object>> events = new ArrayList<>(adminEvents);
        Collections.reverse(events); // Most recent first
        return events.stream().limit(Math.max(1, limit)).toList();
    }

    /**
     * Get summary of log levels in admin events
     */
    public static Map<String, Integer> getAdminEventSummary() {
        Map<String, Integer> summary = new HashMap<>();
        for (Map<String, Object> event : adminEvents) {
            String level = (String) event.get("level");
            summary.merge(level, 1, Integer::sum);
        }
        return summary;
    }

    // Existing methods for compatibility
    public static void add_stations(String stationName, String mode) {
        stationRegistry.put(stationName, mode);
        log_event("STATION_REGISTRY", "Registered station", "DEBUG", Map.of(
            "station", stationName,
            "mode", mode
        ));
    }

    public static void add_active_route(String routeName) {
        activeRoutes.put(routeName, LocalDateTime.now());
        log_event("ROUTE_REGISTRY", "Active route registered", "DEBUG", Map.of(
            "route", routeName
        ));
    }

    public static void add_stop(Object stop) {
        if (stop != null) {
            log_event("STOP_REGISTRY", "Stop registered", "DEBUG", Map.of(
                "stop", stop.toString()
            ));
        }
    }

    /**
     * Get registry information
     */
    public static Map<String, Object> getRegistryInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalStations", stationRegistry.size());
        info.put("activeRoutes", activeRoutes.size());
        info.put("stationsByMode", getStationsByMode());
        return info;
    }

    private static Map<String, Integer> getStationsByMode() {
        Map<String, Integer> byMode = new HashMap<>();
        for (String mode : stationRegistry.values()) {
            byMode.merge(mode, 1, Integer::sum);
        }
        return byMode;
    }

    /**
     * Clear old performance data (useful for testing or periodic cleanup)
     */
    public static void clearPerformanceData() {
        operationDurations.clear();
        operationStartTimes.clear();
        log_event("SYSTEM", "Performance data cleared", "INFO", Map.of());
    }

    /**
     * Get current operation status (for debugging stuck operations)
     */
    public static Map<String, Object> getCurrentOperations() {
        Map<String, Object> current = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, Long> entry : operationStartTimes.entrySet()) {
            String operation = entry.getKey();
            long startTime = entry.getValue();
            long elapsed = System.currentTimeMillis() - startTime;
            
            current.put(operation, Map.of(
                "startTime", startTime,
                "elapsedMs", elapsed,
                "status", elapsed > 30000 ? "STUCK" : "RUNNING"
            ));
        }
        
        return current;
    }
}