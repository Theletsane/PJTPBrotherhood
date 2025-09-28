/**
 * SystemMonitoringController.java
 * 
 * REST Controller for system health monitoring and graph status checking.
 * Provides endpoints for monitoring graph loading status and system health.
 * 
 * Author: Boolean Brotherhood
 * Date: 2025
 */

package com.boolean_brotherhood.public_transportation_journey_planner.Controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemHealthMonitor;
import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemLog;
import com.boolean_brotherhood.public_transportation_journey_planner.System.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.System.PerformanceMetricsRegistry;

@RestController
@RequestMapping("/api/monitor")

public class SystemMonitoringController {

    @Autowired
    private SystemHealthMonitor healthMonitor;

    /**
     * Get comprehensive system health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        SystemLog.log_endpoint("/api/monitor/health");
        try {
            Map<String, Object> healthReport = healthMonitor.performHealthCheck();
            
            // Return 503 Service Unavailable if system is unhealthy
            if (!healthMonitor.isSystemHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthReport);
            }
            
            return ResponseEntity.ok(healthReport);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Health check failed: " + e.getMessage(),
                               "timestamp", java.time.LocalDateTime.now().toString()));
        }
    }

    /**
     * Get quick system summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSystemSummary() {
        SystemLog.log_endpoint("/api/monitor/summary");
        try {
            Map<String, Object> summary = healthMonitor.getSystemSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Summary generation failed: " + e.getMessage()));
        }
    }

    /**
     * Check if system is ready to serve requests
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> isSystemReady() {
        SystemLog.log_endpoint("/api/monitor/ready");
        boolean isReady = healthMonitor.isSystemHealthy();
        
        Map<String, Object> response = Map.of(
            "ready", isReady,
            "status", isReady ? "READY" : "NOT_READY",
            "timestamp", java.time.LocalDateTime.now().toString()
        );
        
        if (!isReady) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a specific graph is ready
     */
    @GetMapping("/graph/{graphName}/ready")
    public ResponseEntity<Map<String, Object>> isGraphReady(@PathVariable String graphName) {
        SystemLog.log_endpoint("/api/monitor/graph/{graphName}/ready");
        try {
            boolean isReady = healthMonitor.isGraphReady(graphName);
            
            Map<String, Object> response = Map.of(
                "graph", graphName,
                "ready", isReady,
                "status", isReady ? "READY" : "NOT_READY",
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            if (!isReady) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Graph status check failed: " + e.getMessage(),
                               "graph", graphName));
        }
    }

    /**
     * Get detailed status for a specific graph
     */
    @GetMapping("/graph/{graphName}")
    public ResponseEntity<Map<String, Object>> getGraphStatus(@PathVariable String graphName) {
        SystemLog.log_endpoint("/api/monitor/graph/{graphName}");
        try {
            Map<String, Object> status = healthMonitor.getGraphStatus(graphName);
            
            if (status.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(status);
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Graph status retrieval failed: " + e.getMessage(),
                               "graph", graphName));
        }
    }

    /**
     * Get all active alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        SystemLog.log_endpoint("/api/monitor/alerts");
        try {
            List<Map<String, Object>> alerts = healthMonitor.getActiveAlerts();
            
            return ResponseEntity.ok(Map.of(
                "alerts", alerts,
                "count", alerts.size(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Alert retrieval failed: " + e.getMessage()));
        }
    }

    /**
     * Get all alerts (including resolved ones)
     */
    @GetMapping("/alerts/all")
    public ResponseEntity<Map<String, Object>> getAllAlerts() {
        SystemLog.log_endpoint("/api/monitor/alerts/all");
        try {
            List<Map<String, Object>> alerts = healthMonitor.getAllAlerts();
            
            return ResponseEntity.ok(Map.of(
                "alerts", alerts,
                "count", alerts.size(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Alert retrieval failed: " + e.getMessage()));
        }
    }

    /**
     * Force a manual health check (useful for debugging)
     */
    @PostMapping("/health/check")
    public ResponseEntity<Map<String, Object>> forceHealthCheck() {
        SystemLog.log_endpoint("/api/monitor/health/check");
        try {
            Map<String, Object> healthReport = healthMonitor.forceHealthCheck();
            return ResponseEntity.ok(healthReport);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Forced health check failed: " + e.getMessage(),
                               "timestamp", java.time.LocalDateTime.now().toString()));
        }
    }

    /**
     * Get monitoring statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMonitoringStats() {
        SystemLog.log_endpoint("/api/monitor/stats");
        try {
            Map<String, Object> summary = healthMonitor.getSystemSummary();
            
            // Add additional monitoring-specific stats
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("systemStatus", summary.get("overallStatus"));
            stats.put("uptime", summary.get("uptime"));
            stats.put("totalHealthChecks", summary.get("totalHealthChecks"));
            stats.put("criticalErrors", summary.get("criticalErrors"));
            stats.put("activeAlerts", summary.get("activeAlerts"));
            stats.put("memory", summary.get("memory"));
            stats.put("graphStatuses", summary.get("graphStatuses"));
            stats.put("lastCheckTime", java.time.LocalDateTime.now().toString());

            Map<String, Object> response = MetricsResponseBuilder.build("systemMonitor", stats, "/api/monitor");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Stats retrieval failed: " + e.getMessage()));
        }
    }

    /**
     * Get performance metrics from PerformanceMetricsRegistry
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        SystemLog.log_endpoint("/api/monitor/performance");
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("overview", PerformanceMetricsRegistry.getOverview());
            payload.put("endpoints", PerformanceMetricsRegistry.getEndpointSummaries());
            
            List<Map<String, Object>> samples = PerformanceMetricsRegistry.getRecentSamples();
            payload.put("recentSamples", samples.stream().limit(25).collect(Collectors.toList()));
            payload.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Performance metrics retrieval failed: " + e.getMessage()));
        }
    }
}
