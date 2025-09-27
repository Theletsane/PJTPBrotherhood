/**
 * SystemHealthMonitor.java
 * 
 * Comprehensive system health monitoring for the Public Transportation Journey Planner.
 * Monitors graph loading status, data integrity, and system health to prevent crashes.
 * 
 * Author: Boolean Brotherhood
 * Date: 2025
 */

package com.boolean_brotherhood.public_transportation_journey_planner.System;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;

@Service
public class SystemHealthMonitor {

    @Autowired(required = false)
    private TrainGraph trainGraph;
    
    @Autowired(required = false)
    private MyCitiBusGraph busGraph;
    
    @Autowired(required = false)
    private GABusGraph gaBusGraph;
    
    @Autowired(required = false)
    private TaxiGraph taxiGraph;

    // Health status tracking
    private final Map<String, GraphHealth> graphHealthStatus = new ConcurrentHashMap<>();
    private final List<SystemAlert> systemAlerts = new ArrayList<>();
    private final AtomicLong totalHealthChecks = new AtomicLong(0);
    private final AtomicInteger criticalErrors = new AtomicInteger(0);
    private final LocalDateTime startTime = LocalDateTime.now();
    private final AtomicBoolean systemHealthy = new AtomicBoolean(false);

    /**
     * Represents the health status of a single graph
     */
    public static class GraphHealth {
        private final String graphName;
        private boolean isLoaded;
        private boolean hasData;
        private int stopCount;
        private int tripCount;
        private LocalDateTime lastChecked;
        private List<String> issues;
        private long loadTimeMs;
        private String status; // HEALTHY, WARNING, CRITICAL, UNKNOWN
        private String errorMessage;

        public GraphHealth(String graphName) {
            this.graphName = graphName;
            this.isLoaded = false;
            this.hasData = false;
            this.stopCount = 0;
            this.tripCount = 0;
            this.lastChecked = LocalDateTime.now();
            this.issues = new ArrayList<>();
            this.loadTimeMs = -1;
            this.status = "UNKNOWN";
            this.errorMessage = null;
        }

        // Getters
        public String getGraphName() { return graphName; }
        public boolean isLoaded() { return isLoaded; }
        public boolean hasData() { return hasData; }
        public int getStopCount() { return stopCount; }
        public int getTripCount() { return tripCount; }
        public LocalDateTime getLastChecked() { return lastChecked; }
        public List<String> getIssues() { return new ArrayList<>(issues); }
        public long getLoadTimeMs() { return loadTimeMs; }
        public String getStatus() { return status; }
        public String getErrorMessage() { return errorMessage; }

        // Setters
        public void setLoaded(boolean loaded) { this.isLoaded = loaded; }
        public void setHasData(boolean hasData) { this.hasData = hasData; }
        public void setStopCount(int stopCount) { this.stopCount = stopCount; }
        public void setTripCount(int tripCount) { this.tripCount = tripCount; }
        public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }
        public void setLoadTimeMs(long loadTimeMs) { this.loadTimeMs = loadTimeMs; }
        public void setStatus(String status) { this.status = status; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public void addIssue(String issue) { 
            this.issues.add("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + issue); 
        }
        
        public void clearIssues() { this.issues.clear(); }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("graphName", graphName);
            map.put("isLoaded", isLoaded);
            map.put("hasData", hasData);
            map.put("stopCount", stopCount);
            map.put("tripCount", tripCount);
            map.put("lastChecked", lastChecked.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            map.put("issues", issues);
            map.put("loadTimeMs", loadTimeMs);
            map.put("status", status);
            map.put("errorMessage", errorMessage);
            return map;
        }
    }

    /**
     * Represents a system alert
     */
    public static class SystemAlert {
        private final String alertId;
        private final String severity; // INFO, WARNING, CRITICAL
        private final String message;
        private final LocalDateTime timestamp;
        private final String component;
        private boolean resolved;

        public SystemAlert(String alertId, String severity, String message, String component) {
            this.alertId = alertId;
            this.severity = severity;
            this.message = message;
            this.component = component;
            this.timestamp = LocalDateTime.now();
            this.resolved = false;
        }

        // Getters
        public String getAlertId() { return alertId; }
        public String getSeverity() { return severity; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getComponent() { return component; }
        public boolean isResolved() { return resolved; }

        public void resolve() { this.resolved = true; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("alertId", alertId);
            map.put("severity", severity);
            map.put("message", message);
            map.put("component", component);
            map.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            map.put("resolved", resolved);
            return map;
        }
    }

    /**
     * Performs comprehensive health check on all transportation graphs
     */
    public Map<String, Object> performHealthCheck() {
        totalHealthChecks.incrementAndGet();
        
        Map<String, Object> healthReport = new HashMap<>();
        Map<String, GraphHealth> graphStatuses = new HashMap<>();
        
        // Check Train Graph
        GraphHealth trainHealth = checkTrainGraph();
        graphStatuses.put("train", trainHealth);
        graphHealthStatus.put("train", trainHealth);
        
        // Check MyCiti Bus Graph
        GraphHealth mycitiBusHealth = checkMyCitiBusGraph();
        graphStatuses.put("myciti", mycitiBusHealth);
        graphHealthStatus.put("myciti", mycitiBusHealth);
        
        // Check GA Bus Graph
        GraphHealth gaBusHealth = checkGABusGraph();
        graphStatuses.put("ga", gaBusHealth);
        graphHealthStatus.put("ga", gaBusHealth);
        
        // Check Taxi Graph
        GraphHealth taxiHealth = checkTaxiGraph();
        graphStatuses.put("taxi", taxiHealth);
        graphHealthStatus.put("taxi", taxiHealth);
        
        // Calculate overall system health
        boolean allGraphsHealthy = trainHealth.getStatus().equals("HEALTHY") &&
                                  mycitiBusHealth.getStatus().equals("HEALTHY") &&
                                  gaBusHealth.getStatus().equals("HEALTHY") &&
                                  taxiHealth.getStatus().equals("HEALTHY");
        
        systemHealthy.set(allGraphsHealthy);
        
        // Generate alerts for critical issues
        generateSystemAlerts(graphStatuses);
        
        // Build health report
        healthReport.put("systemHealthy", allGraphsHealthy);
        healthReport.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        healthReport.put("uptime", java.time.Duration.between(startTime, LocalDateTime.now()).toString());
        healthReport.put("totalHealthChecks", totalHealthChecks.get());
        healthReport.put("criticalErrors", criticalErrors.get());
        healthReport.put("graphs", graphStatuses.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, 
                    e -> e.getValue().toMap())));
        healthReport.put("activeAlerts", getActiveAlerts());
        healthReport.put("systemStatus", allGraphsHealthy ? "HEALTHY" : "DEGRADED");
        
        return healthReport;
    }

    // Add this method to SystemHealthMonitor class
public Map<String, Object> getDetailedMemoryStats() {
    Map<String, Object> memory = MemoryMonitor.getMemoryStats();
    
    // Add graph-specific memory estimates
    memory.put("graphMemoryEstimates", getGraphMemoryEstimates());
    memory.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    
    return memory;
}

private Map<String, Object> getGraphMemoryEstimates() {
    Map<String, Object> estimates = new HashMap<>();
    
    estimates.put("trainGraphMB", estimateTrainMemory());
    estimates.put("myCitiGraphMB", estimateMyCitiMemory());
    estimates.put("gaGraphMB", estimateGAMemory());
    estimates.put("taxiGraphMB", estimateTaxiMemory());
    
    return estimates;
}

private long estimateTrainMemory() {
    if (trainGraph == null) return 0;
    // Rough estimate: 200 bytes per stop + 150 bytes per trip
    long stopMemory = trainGraph.getTrainStops().size() * 200L;
    long tripMemory = trainGraph.getTrainTrips().size() * 150L;
    return (stopMemory + tripMemory) / (1024 * 1024);
}

private long estimateMyCitiMemory() {
    if (busGraph == null) return 0;
    long stopMemory = busGraph.getMyCitiStops().size() * 200L;
    long tripMemory = busGraph.getMyCitiTrips().size() * 150L;
    return (stopMemory + tripMemory) / (1024 * 1024);
}

private long estimateGAMemory() {
    if (gaBusGraph == null) return 0;
    long stopMemory = gaBusGraph.getGAStops().size() * 200L;
    long tripMemory = gaBusGraph.getGATrips().size() * 150L;
    return (stopMemory + tripMemory) / (1024 * 1024);
}

private long estimateTaxiMemory() {
    if (taxiGraph == null) return 0;
    long stopMemory = taxiGraph.getTaxiStops().size() * 200L;
    long tripMemory = taxiGraph.getTaxiTrips().size() * 150L;
    return (stopMemory + tripMemory) / (1024 * 1024);
}

    /**
     * Checks the health of the Train Graph
     */
    private GraphHealth checkTrainGraph() {
        GraphHealth health = new GraphHealth("TrainGraph");
        
        try {
            if (trainGraph == null) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("TrainGraph not injected/loaded");
                health.addIssue("TrainGraph bean is null - dependency injection failed");
                return health;
            }
            
            health.setLoaded(true);
            
            // Check if data is loaded
            List<?> stops = trainGraph.getTrainStops();
            List<?> trips = trainGraph.getTrainTrips();
            
            if (stops == null || stops.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No train stops loaded");
                health.addIssue("Train stops list is empty or null");
            } else if (trips == null || trips.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No train trips loaded");
                health.addIssue("Train trips list is empty or null");
            } else {
                health.setHasData(true);
                health.setStopCount(stops.size());
                health.setTripCount(trips.size());
                
                // Check for reasonable data volumes
                if (stops.size() < 10) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of train stops: " + stops.size());
                } else if (trips.size() < 100) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of train trips: " + trips.size());
                } else {
                    health.setStatus("HEALTHY");
                }
                
                // Check metrics if available
                try {
                    Map<String, Object> metrics = trainGraph.getMetrics();
                    if (metrics.containsKey("stopLoadTimeMs")) {
                        health.setLoadTimeMs((Long) metrics.get("stopLoadTimeMs"));
                    }
                } catch (Exception e) {
                    health.addIssue("Error retrieving train graph metrics: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            health.setStatus("CRITICAL");
            health.setErrorMessage("Exception during train graph check: " + e.getMessage());
            health.addIssue("Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        health.setLastChecked(LocalDateTime.now());
        return health;
    }

    /**
     * Checks the health of the MyCiti Bus Graph
     */
    private GraphHealth checkMyCitiBusGraph() {
        GraphHealth health = new GraphHealth("MyCitiBusGraph");
        
        try {
            if (busGraph == null) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("MyCitiBusGraph not injected/loaded");
                health.addIssue("MyCitiBusGraph bean is null - dependency injection failed");
                return health;
            }
            
            health.setLoaded(true);
            
            // Check if data is loaded
            List<?> stops = busGraph.getMyCitiStops();
            List<?> trips = busGraph.getMyCitiTrips();
            
            if (stops == null || stops.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No MyCiti bus stops loaded");
                health.addIssue("MyCiti bus stops list is empty or null");
            } else if (trips == null || trips.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No MyCiti bus trips loaded");
                health.addIssue("MyCiti bus trips list is empty or null");
            } else {
                health.setHasData(true);
                health.setStopCount(stops.size());
                health.setTripCount(trips.size());
                
                // Check for reasonable data volumes
                if (stops.size() < 10) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of MyCiti bus stops: " + stops.size());
                } else if (trips.size() < 50) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of MyCiti bus trips: " + trips.size());
                } else {
                    health.setStatus("HEALTHY");
                }
                
                // Check metrics if available
                try {
                    Map<String, Long> metrics = busGraph.getMetrics();
                    if (metrics.containsKey("stopsLoadTimeMs")) {
                        health.setLoadTimeMs(metrics.get("stopsLoadTimeMs"));
                    }
                } catch (Exception e) {
                    health.addIssue("Error retrieving MyCiti bus graph metrics: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            health.setStatus("CRITICAL");
            health.setErrorMessage("Exception during MyCiti bus graph check: " + e.getMessage());
            health.addIssue("Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        health.setLastChecked(LocalDateTime.now());
        return health;
    }

    /**
     * Checks the health of the GA Bus Graph
     */
    private GraphHealth checkGABusGraph() {
        GraphHealth health = new GraphHealth("GABusGraph");
        
        try {
            if (gaBusGraph == null) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("GABusGraph not injected/loaded");
                health.addIssue("GABusGraph bean is null - dependency injection failed");
                return health;
            }
            
            health.setLoaded(true);
            
            // Check if data is loaded
            List<?> stops = gaBusGraph.getGAStops();
            List<?> trips = gaBusGraph.getGATrips();
            
            if (stops == null || stops.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No GA bus stops loaded");
                health.addIssue("GA bus stops list is empty or null");
            } else if (trips == null || trips.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No GA bus trips loaded");
                health.addIssue("GA bus trips list is empty or null");
            } else {
                health.setHasData(true);
                health.setStopCount(stops.size());
                health.setTripCount(trips.size());
                
                // Check for reasonable data volumes
                if (stops.size() < 10) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of GA bus stops: " + stops.size());
                } else if (trips.size() < 50) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of GA bus trips: " + trips.size());
                } else {
                    health.setStatus("HEALTHY");
                }
                
                // Check metrics if available
                try {
                    Map<String, Long> metrics = gaBusGraph.getMetrics();
                    if (metrics.containsKey("stopsLoadTimeMs")) {
                        Object loadTime = metrics.get("stopsLoadTimeMs");
                        if (loadTime instanceof Long) {
                            health.setLoadTimeMs((Long) loadTime);
                        }
                    }
                } catch (Exception e) {
                    health.addIssue("Error retrieving GA bus graph metrics: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            health.setStatus("CRITICAL");
            health.setErrorMessage("Exception during GA bus graph check: " + e.getMessage());
            health.addIssue("Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        health.setLastChecked(LocalDateTime.now());
        return health;
    }

    /**
     * Checks the health of the Taxi Graph
     */
    private GraphHealth checkTaxiGraph() {
        GraphHealth health = new GraphHealth("TaxiGraph");
        
        try {
            if (taxiGraph == null) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("TaxiGraph not injected/loaded");
                health.addIssue("TaxiGraph bean is null - dependency injection failed");
                return health;
            }
            
            health.setLoaded(true);
            
            // Check if data is loaded
            List<?> stops = taxiGraph.getTaxiStops();
            List<?> trips = taxiGraph.getTaxiTrips();
            
            if (stops == null || stops.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No taxi stops loaded");
                health.addIssue("Taxi stops list is empty or null");
            } else if (trips == null || trips.isEmpty()) {
                health.setStatus("CRITICAL");
                health.setErrorMessage("No taxi trips loaded");
                health.addIssue("Taxi trips list is empty or null");
            } else {
                health.setHasData(true);
                health.setStopCount(stops.size());
                health.setTripCount(trips.size());
                
                // Check for reasonable data volumes
                if (stops.size() < 5) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of taxi stops: " + stops.size());
                } else if (trips.size() < 20) {
                    health.setStatus("WARNING");
                    health.addIssue("Suspiciously low number of taxi trips: " + trips.size());
                } else {
                    health.setStatus("HEALTHY");
                }
                
                // Check metrics if available
                try {
                    Map<String, Long> metrics = taxiGraph.getMetrics();
                    if (metrics.containsKey("loadTimeMs")) {
                        health.setLoadTimeMs(metrics.get("loadTimeMs"));
                    }
                } catch (Exception e) {
                    health.addIssue("Error retrieving taxi graph metrics: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            health.setStatus("CRITICAL");
            health.setErrorMessage("Exception during taxi graph check: " + e.getMessage());
            health.addIssue("Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        health.setLastChecked(LocalDateTime.now());
        return health;
    }

    /**
     * Generates system alerts based on graph health
     */
    private void generateSystemAlerts(Map<String, GraphHealth> graphStatuses) {
        for (Map.Entry<String, GraphHealth> entry : graphStatuses.entrySet()) {
            String graphName = entry.getKey();
            GraphHealth health = entry.getValue();
            
            if (health.getStatus().equals("CRITICAL")) {
                criticalErrors.incrementAndGet();
                addAlert("CRITICAL_" + graphName.toUpperCase(), "CRITICAL", 
                        health.getErrorMessage(), graphName);
            } else if (health.getStatus().equals("WARNING")) {
                addAlert("WARNING_" + graphName.toUpperCase(), "WARNING",
                        "Graph has warnings: " + String.join(", ", health.getIssues()), graphName);
            } else if (health.getStatus().equals("HEALTHY")) {
                // Resolve any existing alerts for this graph
                resolveAlertsForComponent(graphName);
            }
        }
    }

    /**
     * Adds a system alert
     */
    private void addAlert(String alertId, String severity, String message, String component) {
        // Check if alert already exists and is unresolved
        boolean alertExists = systemAlerts.stream()
                .anyMatch(alert -> alert.getAlertId().equals(alertId) && !alert.isResolved());
        
        if (!alertExists) {
            systemAlerts.add(new SystemAlert(alertId, severity, message, component));
            
            // Keep only last 100 alerts
            if (systemAlerts.size() > 100) {
                systemAlerts.remove(0);
            }
        }
    }

    /**
     * Resolves alerts for a specific component
     */
    private void resolveAlertsForComponent(String component) {
        systemAlerts.stream()
                .filter(alert -> alert.getComponent().equals(component) && !alert.isResolved())
                .forEach(SystemAlert::resolve);
    }

    /**
     * Gets list of active (unresolved) alerts
     */
    public List<Map<String, Object>> getActiveAlerts() {
        return systemAlerts.stream()
                .filter(alert -> !alert.isResolved())
                .map(SystemAlert::toMap)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets all alerts (including resolved ones)
     */
    public List<Map<String, Object>> getAllAlerts() {
        return systemAlerts.stream()
                .map(SystemAlert::toMap)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Checks if a specific graph is ready for use
     */
    public boolean isGraphReady(String graphName) {
        GraphHealth health = graphHealthStatus.get(graphName.toLowerCase());
        return health != null && health.getStatus().equals("HEALTHY") && health.hasData();
    }

    /**
     * Gets the current status of a specific graph
     */
    public Map<String, Object> getGraphStatus(String graphName) {
        GraphHealth health = graphHealthStatus.get(graphName.toLowerCase());
        if (health == null) {
            return Map.of("error", "Graph not found: " + graphName);
        }
        return health.toMap();
    }

    /**
     * Checks if the overall system is healthy
     */
    public boolean isSystemHealthy() {
        return systemHealthy.get();
    }

    /**
     * Gets system summary statistics
     */
    public Map<String, Object> getSystemSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("overallStatus", systemHealthy.get() ? "HEALTHY" : "DEGRADED");
        summary.put("uptime", java.time.Duration.between(startTime, LocalDateTime.now()).toString());
        summary.put("totalHealthChecks", totalHealthChecks.get());
        summary.put("criticalErrors", criticalErrors.get());
        summary.put("activeAlerts", getActiveAlerts().size());
        
        // Graph summary
        Map<String, String> graphStatuses = new HashMap<>();
        for (Map.Entry<String, GraphHealth> entry : graphHealthStatus.entrySet()) {
            graphStatuses.put(entry.getKey(), entry.getValue().getStatus());
        }
        summary.put("graphStatuses", graphStatuses);
        
        // Memory info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        memory.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        memory.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        memory.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        summary.put("memory", memory);

        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("overview", PerformanceMetricsRegistry.getOverview());
        performance.put("endpointSummaries", PerformanceMetricsRegistry.getEndpointSummaries());
        performance.put("recentSamples", PerformanceMetricsRegistry.getRecentSamples().stream().limit(25).toList());
        summary.put("performance", performance);
        
        return summary;
    }

    /**
     * Forces a manual health check (useful for debugging)
     */
    public Map<String, Object> forceHealthCheck() {
        System.out.println("=== FORCED SYSTEM HEALTH CHECK ===");
        Map<String, Object> result = performHealthCheck();
        
        // Print summary to console for immediate visibility
        System.out.println("System Status: " + result.get("systemStatus"));
        System.out.println("Active Alerts: " + getActiveAlerts().size());
        
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> graphs = (Map<String, Map<String, Object>>) result.get("graphs");
        for (Map.Entry<String, Map<String, Object>> entry : graphs.entrySet()) {
            Map<String, Object> graph = entry.getValue();
            System.out.printf("%s: %s (Stops: %s, Trips: %s)%n", 
                    entry.getKey().toUpperCase(),
                    graph.get("status"),
                    graph.get("stopCount"),
                    graph.get("tripCount"));
        }
        
        System.out.println("=====================================");
        return result;
    }
}