package com.boolean_brotherhood.public_transportation_journey_planner.Controllers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.DataFilesRegistry;
import com.boolean_brotherhood.public_transportation_journey_planner.System.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.System.PerformanceMetricsRegistry;
import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemLog;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String DATA_PATH = "CapeTownTransitData/";

    private final TrainController trainController;
    private final TaxiController taxiController;
    private final MyCitiBusController busController;

    @Autowired
    public AdminController(
            TrainController trainController,
            TaxiController taxiController,
            MyCitiBusController busController
    ) {
        this.trainController = trainController;
        this.taxiController = taxiController;
        this.busController = busController;
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles(@RequestParam(required = false) String subPath) throws IOException {
        SystemLog.log_endpoint("/api/admin/list");
        try {
            String targetPath = DATA_PATH + (subPath != null ? subPath : "");
            Resource resource = new ClassPathResource(targetPath);

            if (!resource.exists()) {
                SystemLog.log_event("ADMIN", "Requested data path not found", "WARN", Map.of(
                        "targetPath", targetPath,
                        "absolutePath", resource.getFile().getAbsolutePath()
                ));
                return ResponseEntity.notFound().build();
            }

            List<String> fileNames = new ArrayList<>();
            File[] files = resource.getFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileNames.add(file.getName());
                    }
                }
            }
            
            return ResponseEntity.ok(fileNames);
        } catch (Exception e) {
            SystemLog.log_event("ADMIN", "File listing failed", "ERROR", Map.of(
                    "error", e.getMessage(),
                    "subPath", subPath
            ));
            return ResponseEntity.status(500).body(List.of("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<String> readFile(@RequestParam String filePath) throws IOException {
        SystemLog.log_endpoint("/api/admin/file");
        Resource resource = null;
        if (filePath.startsWith("CapeTownTransitData/")) {
            resource = new ClassPathResource(filePath);
        } else {
            resource = new ClassPathResource(DATA_PATH + filePath);
        }

        if (!resource.exists()) {
            SystemLog.log_event("ADMIN", "Requested data file not found", "WARN", Map.of(
                    "filePath", filePath
            ));
            return ResponseEntity.notFound().build();
        }

        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        SystemLog.log_event("ADMIN", "Read data file", "INFO", Map.of(
                "filePath", filePath,
                "size", content.length()
        ));
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) throws IOException {
        SystemLog.log_endpoint("/api/admin/download");
        Resource resource = new ClassPathResource(DATA_PATH + filePath);

        if (!resource.exists()) {
            SystemLog.log_event("ADMIN", "Requested download missing", "WARN", Map.of(
                    "filePath", filePath
            ));
            return ResponseEntity.notFound().build();
        }

        SystemLog.log_event("ADMIN", "Prepared file download", "INFO", Map.of(
                "filePath", filePath,
                "fileName", resource.getFilename()
        ));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/systemMetrics")
    public Map<String, Object> getAllMetrics() {
        SystemLog.log_endpoint("/api/admin/systemMetrics");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("train", trainController.getMetrics());
        metrics.put("taxi", taxiController.getMetrics());
        metrics.put("bus", busController.getMetrics());
        metrics.put("performanceOverview", PerformanceMetricsRegistry.getOverview());
        metrics.put("trackedEndpoints", PerformanceMetricsRegistry.getEndpointSummaries());
        metrics.put("operationPerformance", SystemLog.getOperationPerformance());
        metrics.put("registryInfo", SystemLog.getRegistryInfo());
        
        SystemLog.log_event("ADMIN", "Collected subsystem metrics", "INFO", Map.of(
                "sections", metrics.keySet().size()
        ));
        return MetricsResponseBuilder.build("admin", metrics, "/api/admin");
    }
    
    @GetMapping("/GetFileInUse")
    public Map<String, List<String>> getFilesInUse() {
        SystemLog.log_endpoint("/api/admin/GetFileInUse");
        Map<String, String> usage = DataFilesRegistry.getUsageLogs();
        Map<String, List<String>> grouped = new HashMap<>();

        for (Map.Entry<String, String> entry : usage.entrySet()) {
            String filePath = entry.getKey();
            String graphType = entry.getValue();

            grouped.computeIfAbsent(graphType, k -> new ArrayList<>()).add(filePath);
        }

        SystemLog.log_event("ADMIN", "Fetched files-in-use registry (grouped)", "INFO", Map.of(
                "groups", grouped.size()
        ));

        return grouped;
    }

    @GetMapping("/MostRecentCall")
    public List<Map<String, Object>> getMostRecentCalls() {
        SystemLog.log_endpoint("/api/admin/MostRecentCall");
        List<Map<String, Object>> recent = SystemLog.GET_ENDPOINT_DATA();
        SystemLog.log_event("ADMIN", "Requested endpoint call history", "INFO", Map.of(
                "count", recent.size()
        ));
        return recent;
    }

    /**
     * Get system logs - returns all logs for debugging
     */
    @GetMapping("/systemLogs")
    public List<Map<String, Object>> getSystemLogs(@RequestParam(name = "limit", defaultValue = "100") int limit) {
        SystemLog.log_endpoint("/api/admin/systemLogs");
        int safeLimit = Math.max(1, limit);
        List<Map<String, Object>> logs = SystemLog.GET_SYSTEM_EVENTS(safeLimit);
        SystemLog.log_event("ADMIN", "Retrieved system event snapshot", "INFO", Map.of(
                "requestedLimit", limit,
                "returned", logs.size()
        ));
        return logs;
    }

    /**
     * NEW: Get admin logs - returns only WARNING, ERROR, SEVERE, CRITICAL events
     */
    @GetMapping("/adminLogs")
    public Map<String, Object> getAdminLogs(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        SystemLog.log_endpoint("/api/admin/adminLogs");
        int safeLimit = Math.max(1, limit);
        List<Map<String, Object>> adminLogs = SystemLog.GET_ADMIN_EVENTS(safeLimit);
        Map<String, Integer> summary = SystemLog.getAdminEventSummary();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("events", adminLogs);
        response.put("summary", summary);
        response.put("totalEvents", adminLogs.size());
        response.put("requestedLimit", limit);
        
        SystemLog.log_event("ADMIN", "Retrieved admin event snapshot", "INFO", Map.of(
                "requestedLimit", limit,
                "returned", adminLogs.size(),
                "criticalEvents", summary.getOrDefault("CRITICAL", 0),
                "errorEvents", summary.getOrDefault("ERROR", 0) + summary.getOrDefault("SEVERE", 0),
                "warningEvents", summary.getOrDefault("WARN", 0) + summary.getOrDefault("WARNING", 0)
        ));
        
        return response;
    }

    /**
     * NEW: Get performance data for important operations
     */
    @GetMapping("/operationPerformance")
    public Map<String, Object> getOperationPerformance() {
        SystemLog.log_endpoint("/api/admin/operationPerformance");
        Map<String, Object> performance = SystemLog.getOperationPerformance();
        Map<String, Object> currentOps = SystemLog.getCurrentOperations();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("completedOperations", performance);
        response.put("currentOperations", currentOps);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        SystemLog.log_event("ADMIN", "Retrieved operation performance data", "INFO", Map.of(
                "completedOperations", performance.size(),
                "currentOperations", currentOps.size()
        ));
        
        return response;
    }

    /**
     * NEW: Clear performance data (useful for testing)
     */
    @PostMapping("/clearPerformanceData")
    public ResponseEntity<Map<String, Object>> clearPerformanceData() {
        SystemLog.log_endpoint("/api/admin/clearPerformanceData");
        try {
            SystemLog.clearPerformanceData();
            Map<String, Object> response = Map.of(
                "message", "Performance data cleared successfully",
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            SystemLog.log_event("ADMIN", "Performance data cleared", "INFO", Map.of());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SystemLog.log_event("ADMIN", "Failed to clear performance data", "ERROR", Map.of(
                "error", e.getMessage()
            ));
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to clear performance data: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/replaceFile")
    public ResponseEntity<String> replaceFile(
            @RequestParam String filePath,
            @RequestPart("file") MultipartFile file
    ) {
        SystemLog.log_endpoint("/api/admin/replaceFile");
        try {
            Path target = Paths.get(DATA_PATH, filePath).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());

            SystemLog.log_event("ADMIN", "Replaced data file", "INFO", Map.of(
                    "filePath", target.toString(),
                    "size", file.getSize()
            ));

            return ResponseEntity.ok("File replaced successfully: " + target.getFileName());
        } catch (IOException e) {
            SystemLog.log_event("ADMIN", "Failed to replace file", "ERROR", Map.of(
                    "filePath", filePath,
                    "error", e.getMessage()
            ));
            return ResponseEntity.status(500).body("Error replacing file: " + e.getMessage());
        }
    }

    @PostMapping("/updateFile")
    public ResponseEntity<String> updateFile(
            @RequestParam String filePath,
            @RequestParam String content
    ) {
        SystemLog.log_endpoint("/api/admin/updateFile");
        try {
            Path target = Paths.get(DATA_PATH, filePath).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, content.getBytes(StandardCharsets.UTF_8), 
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            SystemLog.log_event("ADMIN", "Updated data file", "INFO", Map.of(
                    "filePath", target.toString(),
                    "appendLength", content.length()
            ));

            return ResponseEntity.ok("File updated successfully: " + target.getFileName());
        } catch (IOException e) {
            SystemLog.log_event("ADMIN", "Failed to update file", "ERROR", Map.of(
                    "filePath", filePath,
                    "error", e.getMessage()
            ));
            return ResponseEntity.status(500).body("Error updating file: " + e.getMessage());
        }
    }
}