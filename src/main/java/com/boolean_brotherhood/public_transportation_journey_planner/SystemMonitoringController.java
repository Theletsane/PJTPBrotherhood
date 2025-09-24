/**
 * SystemMonitoringController.java
 * 
 * REST Controller for system health monitoring and graph status checking.
 * Provides endpoints for monitoring graph loading status and system health.
 * 
 * Author: Boolean Brotherhood
 * Date: 2025
 */

package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
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
        try {
            Map<String, Object> summary = healthMonitor.getSystemSummary();
            
            // Add additional monitoring-specific stats
            Map<String, Object> stats = Map.of(
                "systemStatus", summary.get("overallStatus"),
                "uptime", summary.get("uptime"),
                "totalHealthChecks", summary.get("totalHealthChecks"),
                "criticalErrors", summary.get("criticalErrors"),
                "activeAlerts", summary.get("activeAlerts"),
                "memory", summary.get("memory"),
                "graphStatuses", summary.get("graphStatuses"),
                "lastCheckTime", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Stats retrieval failed: " + e.getMessage()));
        }
    }
}
